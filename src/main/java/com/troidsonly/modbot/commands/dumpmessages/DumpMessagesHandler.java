/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2018-2020 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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
 *   org.lizardirc.beancounter.Listeners
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.commands.dumpmessages;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.commands.log.LogListener;
import com.troidsonly.modbot.commands.log.MessageCache;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

public class DumpMessagesHandler implements CommandHandler {
    private static final String CMD_DUMP_MESSAGES = "dumpmessages";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_DUMP_MESSAGES);
    private static final String PERM_DUMP_MESSAGES = CMD_DUMP_MESSAGES;

    private final AccessControl acl;
    private final LogListener logListener;

    public DumpMessagesHandler(AccessControl acl, LogListener logListener) {
        this.logListener = Objects.requireNonNull(logListener);
        this.acl = Objects.requireNonNull(acl);
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.size() >= 1 && commands.get(0).equals(CMD_DUMP_MESSAGES)) {
            if (acl.hasPermission(event.getMember(), PERM_DUMP_MESSAGES)) {
                List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                Path messageHistory = null;

                remainder = remainder.trim();

                try {
                    MessageCache messageCache = logListener.getMessageCache();

                    if (messageCache == null) {
                        throw new Exception("Logging must be enabled for this functionality to work.");
                    }

                    if (mentionedUsers.size() == 1) {
                        User targetUser = mentionedUsers.get(0);
                        messageHistory = MessageHistoryDumper.dumpMemberMessageHistory(targetUser, event.getGuild(), messageCache);
                    } else {
                        if (!remainder.isEmpty()) {
                            User targetUser = event.getJDA().getUserById(remainder);

                            if (targetUser == null) {
                                throw new Exception("Could not identify a user with the specified UID.");
                            }

                            messageHistory = MessageHistoryDumper.dumpMemberMessageHistory(targetUser, event.getGuild(), messageCache);
                        } else {
                            throw new Exception("The user you wish to get message history for must be specified");
                        }
                    }

                    if (messageHistory != null) {
                        String messageText = event.getMember().getAsMention() + " Here is the message history dump file you requested:";
                        Message message = (new MessageBuilder()).append(messageText).build();
                        event.getChannel()
                                .sendMessage(message)
                                .addFile(messageHistory.toFile())
                                .complete();
                    } else {
                        throw new Exception("An unknown error has occurred.");
                    }
                } catch (Exception e) {
                    Miscellaneous.respond(event, "Failed to generate requested message history dump file: " + e.toString() + "\n" +
                        "Syntax: `" + CMD_DUMP_MESSAGES + " [user]`\n" +
                        "Where *user* is either the target user's numeric User ID (UID) **or** a mention of the target user.  Only a single user may be specified; if " +
                        "you need to get history for more than one user, just run this command more than once.");
                } finally {
                    try {
                        if (messageHistory != null) {
                            Files.deleteIfExists(messageHistory);
                        }
                    } catch (IOException e) {
                        // Oh well.
                    }
                }
            } else {
                Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
            }
        }
    }
}
