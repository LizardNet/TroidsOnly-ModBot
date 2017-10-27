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

import java.util.Objects;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public class GsonPersistenceManager<T> implements PersistenceManager<T> {
    private final PersistenceWrapper<JsonElement> wrapper;
    private final String namespace;
    private final Class<T> type;
    private final Gson gson = new Gson();

    GsonPersistenceManager(PersistenceWrapper<JsonElement> wrapper, String namespace, Class<T> type) {
        this.wrapper = Objects.requireNonNull(wrapper);
        this.namespace = Objects.requireNonNull(namespace);

        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace cannot be an empty string");
        }

        this.type = Objects.requireNonNull(type);
    }

    @Override
    public Optional<T> get() {
        return wrapper.get(namespace).map(jsonElement -> gson.fromJson(jsonElement, type));
    }

    @Override
    public synchronized void persist(T data) {
        wrapper.set(namespace, gson.toJsonTree(data, type));
    }

    @Override
    public synchronized void sync() {
        wrapper.sync();
    }
}
