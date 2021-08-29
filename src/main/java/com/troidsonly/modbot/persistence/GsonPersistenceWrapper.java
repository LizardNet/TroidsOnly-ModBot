/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017 by the Metroid Community Discord Server's Development Team. Some rights reserved.
 *
 * License GPLv3+: GNU General Public License version 3 or later (at your choice):
 * <http://gnu.org/licenses/gpl.html>. This is free software: you are free to
 * change and redistribute it at your will provided that your redistribution, with
 * or without modifications, is also licensed under the GNU GPL. (Although not
 * required by the license, we also ask that you attribute us!) There is NO
 * WARRANTY FOR THIS SOFTWARE to the extent permitted by law.
 *
 * This project contains code and components derived from the
 * LizardIRC/Beancounter IRC bot <https://www.lizardirc.org/?page=beancounter>,
 * which is also licensed GNU GPLv3+.
 *
 * This is an open source project. The source Git repositories, which you are
 * welcome to contribute to, can be found here:
 * <https://gerrit.fastlizard4.org/r/gitweb?p=TroidsOnly%2FModBot.git;a=summary>
 * <https://git.fastlizard4.org/gitblit/summary/?r=TroidsOnly/ModBot.git>
 *
 * Gerrit Code Review for the project:
 * <https://gerrit.fastlizard4.org/r/#/q/project:TroidsOnly/ModBot,n,z>
 *
 * Alternatively, the project source code can be found on the PUBLISH-ONLY mirror
 * on GitHub: <https://github.com/LizardNet/TroidsOnly-ModBot>
 *
 * Note: Pull requests and patches submitted to GitHub will be transferred by a
 * developer to Gerrit before they are acted upon.
 *
 * The code in this file is modified from the following class(es) in LizardIRC/Beancounter:
 *   org.lizardirc.beancounter.persistence.PropertiesPersistenceManager
 *   org.lizardirc.beancounter.persistence.PropertiesPersistenceManager.PropertiesWrapper
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.persistence;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

public class GsonPersistenceWrapper implements PersistenceWrapper<JsonElement> {
    private static final Type TYPE_TOKEN = new TypeToken<Map<String, JsonElement>>(){}.getType();

    private final Path path;
    private final Map<String, JsonElement> cache = new HashMap<>();
    private final Set<String> dirty = new HashSet<>();
    private final Gson gson;

    public GsonPersistenceWrapper(Path path) {
        this.path = path;
        gson = new GsonBuilder().setPrettyPrinting().create();

        loadClean();
    }

    @Override
    public <T> GsonPersistenceManager<T> getPersistenceManager(String namespace, Class<T> type) {
        return new GsonPersistenceManager<>(this, namespace, type);
    }

    @Override
    public Optional<JsonElement> get(String key) {
        JsonElement ret = cache.get(key);
        return ret == null ? Optional.empty() : Optional.of(ret);
    }

    @Override
    public synchronized void set(String key, JsonElement data) {
        cache.put(key, data);
        dirty.add(key);
    }

    public synchronized void loadClean() {
        try (InputStreamReader is = new InputStreamReader(Files.newInputStream(path, StandardOpenOption.READ),
                StandardCharsets.UTF_8)) {
            Map <String, JsonElement> loaded = gson.fromJson(is, TYPE_TOKEN);

            if (loaded != null) {
                loaded.keySet().forEach(prop -> {
                    if (!dirty.contains(prop)) {
                        cache.put(prop, loaded.get(prop));
                    }
                });
            }
        } catch (NoSuchFileException e) {
            System.err.println("WARNING: Could not find state file " + path + " (NoSuchFileException). This is normal if this is the first time running the bot.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void save() {
        if (!dirty.isEmpty()) {
            Path tempFile;

            try {
                tempFile = Files.createTempFile("modbot-state", ".tmp");
            } catch (IOException e) {
                System.err.println("ERROR: Unable to create temporary state file (IOException):");
                e.printStackTrace();
                return;
            }

            // Write out to the tempfile
            try (PrintStream ps = new PrintStream(Files.newOutputStream(tempFile, StandardOpenOption.WRITE), false,
                    StandardCharsets.UTF_8.name())) {
                String output = gson.toJson(cache, TYPE_TOKEN);
                ps.println(output);
            } catch (IOException e) {
                System.err.println("ERROR: IOException while writing bot state to state file " + tempFile + ':');
                e.printStackTrace();
                return;
            }

            dirty.clear();

            // Attempt to move the tempfile over the old beanledger
            try {
                Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("ERROR: IOException while moving temporary state file at " + tempFile + " to permanent location at " + path + ':');
                e.printStackTrace();
            }
        }
    }

    @Override
    public synchronized void sync() {
        loadClean();
        save();
    }
}
