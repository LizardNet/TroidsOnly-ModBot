/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2020 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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
 */

package com.troidsonly.modbot.commands.tuuuuuuubes;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.RateLimitedException;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;

public class BombAndTubesHandler implements CommandHandler {
    private static final String CMD_TUBES = "tubes";
    private static final String CMD_BOMB = "bomb";
    private static final String CMD_BOOTY = "booty";
    private final Set<String> COMMANDS;

    private static final String URLLIST_SCMD_ADD = "add";
    private static final String URLLIST_SCMD_REMOVE = "remove";
    private static final String URLLIST_SCMD_LIST = "list";
    private static final Set<String> URLLIST_SUBCOMMANDS = ImmutableSet.of(URLLIST_SCMD_ADD, URLLIST_SCMD_REMOVE, URLLIST_SCMD_LIST);

    private static final String TUBES_SCMD_EXPLAIN = "explain";
    private static final Set<String> TUBES_SUBCOMMANDS = ImmutableSet.of(TUBES_SCMD_EXPLAIN, "");
    // The empty string here is a workaround to prevent the bot from "helpfully" assuming that the explain subcommand
    // is always implied.

    private static final String PERM_MANAGE_TUBES = "manageBombs";

    private final PersistenceManager<BombsAndTubesRepository> pm;
    private final Path tubesPath;
    private final AccessControl acl;
    private final BombsAndTubesRepository config;
    private final Random random = new Random();

    // Ratelimiting stuff; mapping the usage of each command or the end of the ratelimit period for each channel.
    // Keys are numeric channel IDs.
    private final Map<String, Integer> bombCount = new HashMap<>();
    private final Map<String, Long> bombRatelimitEnd = new HashMap<>();
    private final Map<String, Integer> tubeCount = new HashMap<>();
    private final Map<String, Long> tubeRatelimitEnd = new HashMap<>();
    private final Map<String, Integer> bootyCount = new HashMap<>();
    private final Map<String, Long> bootyRatelimitEnd = new HashMap<>();

    public BombAndTubesHandler(PersistenceWrapper<?> persistenceWrapper, Path tubesPath, AccessControl acl,
            boolean enableBooty) {
        pm = persistenceWrapper.getPersistenceManager("tuuuuuuubes", BombsAndTubesRepository.class);
        this.tubesPath = tubesPath;
        this.acl = acl;

        if (!Files.isDirectory(tubesPath)) {
            throw new IllegalArgumentException("tubesPath must be a directory");
        }

        if (!Files.isReadable(tubesPath)) {
            throw new IllegalArgumentException("tubesPath is not readable");
        }

        if (!Files.isExecutable(tubesPath)) {
            throw new IllegalArgumentException("tubesPath is not enumeratable (\"executable\")");
        }

        config = pm.get().orElseGet(BombsAndTubesRepository::empty);

        if (enableBooty) {
            COMMANDS = ImmutableSet.of(CMD_TUBES, CMD_BOMB, CMD_BOOTY);
        } else {
            COMMANDS = ImmutableSet.of(CMD_TUBES, CMD_BOMB);
        }
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1) {
            if (commands.get(0).equals(CMD_BOMB) || commands.get(0).equals(CMD_BOOTY)) {
                return URLLIST_SUBCOMMANDS;
            } else if (commands.get(0).equals(CMD_TUBES)) {
                return TUBES_SUBCOMMANDS;
            }
        }

        return Collections.emptySet();
    }

    @Override
    public synchronized void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        switch (commands.get(0)) {
            case CMD_BOMB:
                handleUrlListCommands(event, commands, remainder, config.getBombs(), bombCount, bombRatelimitEnd, "bomb");
                break;
            case CMD_BOOTY:
                handleUrlListCommands(event, commands, remainder, config.getBooty(), bootyCount, bootyRatelimitEnd, "booty");
                break;
            case CMD_TUBES:
                if (commands.size() > 1 && commands.get(1).equals(TUBES_SCMD_EXPLAIN)) {
                    event.getChannel().sendMessage("http://knowyourmeme.com/memes/tuuuuuubes").queue();
                } else {
                    tubeRatelimitEnd.putIfAbsent(event.getChannel().getId(), 0L);
                    tubeCount.putIfAbsent(event.getChannel().getId(), 0);

                    if (tubeRatelimitEnd.get(event.getChannel().getId()) <= Instant.now().getEpochSecond()) {
                        tubeRatelimitEnd.put(event.getChannel().getId(), Instant.now().getEpochSecond() + 30L);
                        tubeCount.put(event.getChannel().getId(), 0);
                    }

                    if (tubeCount.get(event.getChannel().getId()) == 3) {
                        Miscellaneous.respond(event, "There are too many tubes rn, slow down pls!");
                        tubeCount.put(event.getChannel().getId(), tubeCount.get(event.getChannel().getId()) + 1);
                        return;
                    } else if (tubeCount.get(event.getChannel().getId()) > 3) {
                        // Silently ignore until the ratelimit cools down again
                        return;
                    }

                    Path tube;

                    try {
                        List<Path> tubes = Files.list(tubesPath)
                            .collect(Collectors.toList());

                        do {
                            // Naively ignore non-image files by trying again if we pull out a file that ends in .txt
                            // This is far from foolproof, but allows us to do stuff like store the tubes images in a Git
                            // submodule repository for ease of modification, while allowing us to have text information in the
                            // same repository.

                            if (tubes.isEmpty()) {
                                throw new NoTubesException();
                            }

                            tube = tubes.get(random.nextInt(tubes.size()));
                        } while (tube.getFileName().toString().endsWith(".txt") && tubes.remove(tube));

                    } catch (IOException | NoTubesException e) {
                        Miscellaneous.respond(event, ":fearful: Could not find any tubes: " + e.toString());
                        e.printStackTrace(System.err);
                        return;
                    }

                    try (InputStream is = Files.newInputStream(tube, StandardOpenOption.READ)) {
                        event.getMessage().getChannel().sendFile(is, tube.getFileName().toString()).complete(false);
                        tubeCount.put(event.getChannel().getId(), tubeCount.get(event.getChannel().getId()) + 1);
                    } catch (IOException | RateLimitedException e) {
                        Miscellaneous.respond(event, ":fearful: Could not open the tubes: " + e.toString());
                        e.printStackTrace(System.err);
                    }
                }
                break;
        }
    }

    private synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }

    private synchronized void handleUrlListCommands(GuildMessageReceivedEvent event, List<String> commands, String remainder,
                                                    List<URL> urlList, Map<String, Integer> rateLimitCount, Map<String, Long> rateLimitEnd,
                                                    String what) {
        if (commands.size() > 1) {
            if (acl.hasPermission(event.getMember(), PERM_MANAGE_TUBES)) {
                remainder = remainder.trim();

                switch (commands.get(1)) {
                    case URLLIST_SCMD_ADD:
                        if (remainder.isEmpty()) {
                            Miscellaneous.respond(event, "You need to give me a " + what + "'s URL to add!");
                        } else {
                            try {
                                URL url = new URL(remainder);
                                urlList.add(url);
                                sync();
                                Miscellaneous.respond(event, "Done!");
                            } catch (MalformedURLException e) {
                                Miscellaneous.respond(event, "Could not add " + what + ": " + e.toString());
                            }
                        }
                        break;
                    case URLLIST_SCMD_LIST:
                        if (urlList.isEmpty()) {
                            Miscellaneous.respond(event, "I don't have any " + what + "s saved :(");
                        } else {
                            Miscellaneous.respond(event, "PMing you a list of " + what + "s!");
                            StringBuilder whats = new StringBuilder("```\n");
                            urlList.forEach(entry -> whats.append("* ").append(entry.toString()).append('\n'));
                            whats.append("```");

                            try {
                                Objects.requireNonNull(event.getMember()).getUser()
                                        .openPrivateChannel()
                                        .complete(false)
                                        .sendMessage(whats.toString())
                                        .complete(false);
                            } catch (Exception e) {
                                Miscellaneous.respond(event, "Could not send you a private message: " + e.toString());
                            }
                        }
                        break;
                    case URLLIST_SCMD_REMOVE:
                        if (remainder.isEmpty()) {
                            Miscellaneous.respond(event, "You need to tell me what " + what + " to remove!");
                        } else {
                            try {
                                URL url = new URL(remainder);
                                if (urlList.remove(url)) {
                                    sync();
                                    Miscellaneous.respond(event, "Done!");
                                } else {
                                    Miscellaneous.respond(event, "I couldn't find a " + what + " with that URL.");
                                }
                            } catch (MalformedURLException e) {
                                Miscellaneous.respond(event, "Could not add " + what + ": " + e.toString());
                            }
                        }
                        break;
                }
            } else {
                Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
            }
        } else {
            rateLimitEnd.putIfAbsent(event.getChannel().getId(), 0L);
            rateLimitCount.putIfAbsent(event.getChannel().getId(), 0);

            if (rateLimitEnd.get(event.getChannel().getId()) <= Instant.now().getEpochSecond()) {
                rateLimitEnd.put(event.getChannel().getId(), Instant.now().getEpochSecond() + 30L);
                rateLimitCount.put(event.getChannel().getId(), 0);
            }

            if (rateLimitCount.get(event.getChannel().getId()) == 3) {
                Miscellaneous.respond(event, "There are too many " + what + "s rn, slow down pls!");
                rateLimitCount.put(event.getChannel().getId(), rateLimitCount.get(event.getChannel().getId()) + 1);
                return;
            } else if (rateLimitCount.get(event.getChannel().getId()) > 3) {
                // Silently ignore until the ratelimit cools down again
                return;
            }

            if (!urlList.isEmpty()) {
                URL url = urlList.get(random.nextInt(urlList.size()));
                event.getMessage().getChannel().sendMessage(url.toString()).complete();
                rateLimitCount.put(event.getChannel().getId(), rateLimitCount.get(event.getChannel().getId()) + 1);

                if (what.equals("booty")) {
                    event.getMessage().getChannel().sendMessage("Credit: <https://largepiratebooty.tumblr.com/>").queue();
                }
            } else {
                Miscellaneous.respond(event, "I have no " + what + "s to drop at the moment.  :(");
            }
        }
    }
}
