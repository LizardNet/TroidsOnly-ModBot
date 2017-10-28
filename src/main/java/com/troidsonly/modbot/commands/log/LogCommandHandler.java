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
 */

package com.troidsonly.modbot.commands.log;

import java.awt.Color;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.utils.Miscellaneous;

class LogCommandHandler implements CommandHandler {
    private static final String CMD_CFGLOG = "cfglog";
    private static final String CMD_LOG = "log";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_CFGLOG, CMD_LOG);

    private static final String CFGLOG_SCMD_ENABLE = "enable";
    private static final String CFGLOG_SCMD_DISABLE = "disable";
    private static final String CFGLOG_SCMD_SETTARGET = "setlogtarget";
    private static final String CFGLOG_SCMD_SHOWTARGET = "showtarget";
    private static final String CFGLOG_SCMD_ADD_FILTER = "addfilter";
    private static final String CFGLOG_SCMD_REMOVE_FILTER = "removefilter";
    private static final String CFGLOG_SCMD_LIST_FILTERS = "listfilters";
    private static final Set<String> CFGLOG_SCMDS = ImmutableSet.of(CFGLOG_SCMD_ENABLE, CFGLOG_SCMD_DISABLE, CFGLOG_SCMD_SETTARGET,
        CFGLOG_SCMD_SHOWTARGET, CFGLOG_SCMD_ADD_FILTER, CFGLOG_SCMD_REMOVE_FILTER, CFGLOG_SCMD_LIST_FILTERS);

    private static final String FILTER_TYPE_USER = "user";
    private static final String FILTER_TYPE_CHANNEL = "channel";
    private static final Set<String> FILTER_TYPES = ImmutableSet.of(FILTER_TYPE_USER, FILTER_TYPE_CHANNEL);

    private static final String PERM_LOG_CONTROL = "logcon";
    private static final String PERM_MK_LOG_ENTRY = "mklog";
    private static final String E_PERMFAIL = "No u!  (You don't have permission to do this.)";

    private final LogListener parent;

    public LogCommandHandler(LogListener parent) {
        this.parent = parent;
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_CFGLOG)) {
            return CFGLOG_SCMDS;
        }

        if (commands.size() == 2 && commands.get(0).equals(CMD_CFGLOG) && (commands.get(1).equals(CFGLOG_SCMD_ADD_FILTER) || commands.get(1).equals(CFGLOG_SCMD_REMOVE_FILTER))) {
            return FILTER_TYPES;
        }

        return Collections.emptySet();
    }

    @Override
    public synchronized void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        remainder = remainder.trim();
        boolean addFilter = false;

        switch (commands.get(0)) {
            case CMD_LOG:
                if (parent.getAcl().hasPermission(event.getMember(), PERM_MK_LOG_ENTRY)) {
                    if (parent.getConfig().getEnabled()) {
                        if (!remainder.isEmpty()) {
                            String logMessage = event.getMessage().getRawContent().substring(commands.get(0).length()).trim();

                            EmbedBuilder embedBuilder = new EmbedBuilder();

                            embedBuilder.setTitle("Log annotation");
                            embedBuilder.setDescription(logMessage);
                            embedBuilder.setColor(new Color(0xAAAAAA));
                            embedBuilder.setTimestamp(Instant.now());
                            embedBuilder.setFooter(event.getMember().getEffectiveName(), event.getAuthor().getAvatarUrl());

                            parent.sendToLog(embedBuilder.build());
                            Miscellaneous.respond(event, "Logged your message.");
                        } else {
                            Miscellaneous.respond(event, "You didn't give me a message to log!");
                        }
                    } else {
                        Miscellaneous.respond(event, "Cannot comply - Logging is currently disabled.");
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_CFGLOG:
                if (parent.getAcl().hasPermission(event.getMember(), PERM_LOG_CONTROL)) {
                    if (commands.size() >= 2) {
                        switch (commands.get(1)) {
                            case CFGLOG_SCMD_ENABLE:
                                if (parent.getConfig().getPrimaryLogChannelId() != null) {
                                    parent.enableLog();
                                    Miscellaneous.respond(event, "Logging enabled!");
                                } else {
                                    Miscellaneous.respond(event, "Cannot enable logging - primary logging channel has not yet been set.\n" +
                                        "Set with `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_SETTARGET + " [#channelName]`");
                                }
                                break;
                            case CFGLOG_SCMD_DISABLE:
                                parent.disableLog();
                                Miscellaneous.respond(event, "Logging disabled.");
                                break;
                            case CFGLOG_SCMD_SETTARGET:
                                if (remainder.isEmpty()) {
                                    Miscellaneous.respond(event, "You need to tell me what channel to log to!\n" +
                                        "Syntax: `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_SETTARGET + " [#channelName]`");
                                } else {
                                    String[] args = remainder.split(" ");

                                    if (Miscellaneous.isChannelLike(args[0])) {
                                        TextChannel channel = Miscellaneous.resolveTextChannelName(event, args[0]);

                                        if (channel != null) {
                                            parent.getConfig().setPrimaryLogChannelId(channel.getId());
                                            parent.sync();
                                            Miscellaneous.respond(event, "Done!");
                                        } else {
                                            return;
                                        }
                                    } else {
                                        Miscellaneous.respond(event, "That doesn't seem to be a valid chanel name.  Remember that text channel names start with a `#`.");
                                    }
                                }
                                break;
                            case CFGLOG_SCMD_SHOWTARGET:
                                if (parent.getConfig().getPrimaryLogChannelId() != null) {
                                    TextChannel channel = event.getGuild().getTextChannelById(parent.getConfig().getPrimaryLogChannelId());

                                    Miscellaneous.respond(event, "Primary logging channel is set to " + channel.toString());
                                } else {
                                    Miscellaneous.respond(event, "No primary logging channel has yet been set up; please set one up before enabling logging.\n" +
                                        "Syntax to do this: `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_SETTARGET + " [#channelName]`");
                                }
                                break;
                            case CFGLOG_SCMD_ADD_FILTER:
                                addFilter = true;
                            case CFGLOG_SCMD_REMOVE_FILTER:
                                if (commands.size() == 3) {
                                    String args[] = remainder.split(" ");

                                    switch (commands.get(2)) {
                                        case FILTER_TYPE_CHANNEL:
                                            if (addFilter) {
                                                if (args.length < 2) {
                                                    Miscellaneous.respond(event, "Error: Too few arguments.\n" +
                                                        "Syntax: `" + CMD_CFGLOG + ' ' +
                                                        CFGLOG_SCMD_ADD_FILTER + ' ' + FILTER_TYPE_CHANNEL + " [#targetChannel] [#filterChannel]`");
                                                } else {
                                                    if (Miscellaneous.isChannelLike(args[0])) {
                                                        if (Miscellaneous.isChannelLike(args[1])) {
                                                            TextChannel targetChannel = Miscellaneous.resolveTextChannelName(event, args[0]);

                                                            if (targetChannel != null) {
                                                                TextChannel filterChannel = Miscellaneous.resolveTextChannelName(event, args[1]);

                                                                if (filterChannel != null) {
                                                                    Map<String, String> filters = parent.getConfig().getChannelIdToLogChannelIdFilters();

                                                                    if (filters.containsKey(filterChannel.getId())) {
                                                                        Miscellaneous.respond(event, "**Warning:** Channel " + filterChannel.getName() + " was previously being " +
                                                                            "filtered to " + event.getGuild().getTextChannelById(filters.get(filterChannel.getId())).getName() +
                                                                            ".  Overwriting.");
                                                                    }

                                                                    filters.put(filterChannel.getId(), targetChannel.getId());
                                                                    parent.sync();
                                                                    Miscellaneous.respond(event, "Channel log filter added.");
                                                                }
                                                            }
                                                        } else {
                                                            Miscellaneous.respond(event, "Error: Specified filter channel doesn't look like a valid channel name.  " +
                                                                "Remember that channel names start with a `#`.\n" +
                                                                "Syntax: `" + CMD_CFGLOG + ' ' +
                                                                CFGLOG_SCMD_ADD_FILTER + ' ' + FILTER_TYPE_CHANNEL + " [#targetChannel] [#filterChannel]`");
                                                        }
                                                    } else {
                                                        Miscellaneous.respond(event, "Error: Specified target channel doesn't look like a valid channel name.  " +
                                                            "Remember that channel names start with a `#`.\n" +
                                                            "Syntax: `" + CMD_CFGLOG + ' ' +
                                                            CFGLOG_SCMD_ADD_FILTER + ' ' + FILTER_TYPE_CHANNEL + " [#targetChannel] [#filterChannel]`");
                                                    }
                                                }
                                            } else {
                                                if (args.length < 1) {
                                                    Miscellaneous.respond(event, "Error: Too few arguments\n" +
                                                        "Syntax: `" + CMD_CFGLOG + ' ' +
                                                        CFGLOG_SCMD_REMOVE_FILTER + ' ' + FILTER_TYPE_CHANNEL + " [#filterChannel]`");
                                                } else {
                                                    if (Miscellaneous.isChannelLike(args[0])) {
                                                        TextChannel filterChannel = Miscellaneous.resolveTextChannelName(event, args[0]);

                                                        if (filterChannel != null) {
                                                            if (parent.getConfig().getChannelIdToLogChannelIdFilters().remove(filterChannel.getId()) != null) {
                                                                parent.sync();
                                                                Miscellaneous.respond(event, "Channel log filter removed");
                                                            } else {
                                                                Miscellaneous.respond(event, "Unable to comply - that channel was never filtered in the first place!");
                                                            }
                                                        }
                                                    } else {
                                                        Miscellaneous.respond(event, "Error: Specified filter channel doesn't look like a valid channel name.  " +
                                                            "Remember that channel names start with a `#`.\n" +
                                                            "Syntax: `" + CMD_CFGLOG + ' ' +
                                                            CFGLOG_SCMD_REMOVE_FILTER + ' ' + FILTER_TYPE_CHANNEL + " [#filterChannel]`");
                                                    }
                                                }
                                            }
                                            break;
                                        case FILTER_TYPE_USER:
                                            if (addFilter) {
                                                if (args.length < 2) {
                                                    Miscellaneous.respond(event, "Error: Too few arguments\n" +
                                                    "Syntax: `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_ADD_FILTER + ' ' + FILTER_TYPE_USER +
                                                    " [#targetChannel] [@filterUsername]`");
                                                } else {
                                                    if (Miscellaneous.isChannelLike(args[0])) {
                                                        TextChannel targetChannel = Miscellaneous.resolveTextChannelName(event, args[0]);

                                                        if (targetChannel != null) {
                                                            List<User> targetUsers = event.getMessage().getMentionedUsers();
                                                            if (targetUsers.size() == 1) {
                                                                User filterUser = targetUsers.get(0);
                                                                Map<String, String> filter = parent.getConfig().getUserIdToLogChannelIdFilters();

                                                                if (filter.containsKey(filterUser.getId())) {
                                                                    Miscellaneous.respond(event, "**Warning:** User " + filterUser.getName() + '#' + filterUser.getDiscriminator() +
                                                                        " was previously being filtered to " + event.getGuild().getTextChannelById(filter.get(filterUser.getId())).getName() +
                                                                        ".  Overwriting.");
                                                                }

                                                                filter.put(filterUser.getId(), targetChannel.getId());
                                                                parent.sync();
                                                                Miscellaneous.respond(event, "User log filter added.");
                                                            } else {
                                                                Miscellaneous.respond(event, "Error: Your message must mention the user whose log entries you wish to filter, and only one user." +
                                                                    "Syntax: `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_ADD_FILTER + ' ' + FILTER_TYPE_USER +
                                                                    " [#targetChannel] [@filterUsername]`");
                                                            }
                                                        }
                                                    } else {
                                                        Miscellaneous.respond(event, "Error: Specified target channel doesn't look like a valid channel name.  " +
                                                            "Remember that channel names start with a `#`.\n" +
                                                            "Syntax: `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_ADD_FILTER + ' ' + FILTER_TYPE_USER +
                                                            " [#targetChannel] [@filterUsername]`");
                                                    }
                                                }
                                            } else {
                                                if (args.length < 1) {
                                                    Miscellaneous.respond(event, "Error: Too few arguments\n" +
                                                        "Syntax: `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_REMOVE_FILTER + ' ' + FILTER_TYPE_USER +
                                                        " [@filterUsername]`");
                                                } else {
                                                    List<User> targetUsers = event.getMessage().getMentionedUsers();

                                                    if (targetUsers.size() == 1) {
                                                        User filterUser = targetUsers.get(0);

                                                        if (parent.getConfig().getUserIdToLogChannelIdFilters().remove(filterUser.getId()) != null) {
                                                            parent.sync();
                                                            Miscellaneous.respond(event, "User log filter removed.");
                                                        } else {
                                                            Miscellaneous.respond(event, "Unable to comply - that user was never filtered in the first place!");
                                                        }
                                                    } else {
                                                        Miscellaneous.respond(event, "Error: Your message must mention the user whose log entires you wish to unfilter, and only one user." +
                                                            "Syntax: `" + CMD_CFGLOG + ' ' + CFGLOG_SCMD_REMOVE_FILTER + ' ' + FILTER_TYPE_USER +
                                                            " [@filterUsername]`");
                                                    }
                                                }
                                            }
                                            break;
                                    }
                                } else {
                                    Miscellaneous.respond(event, "Too few arguments.  Syntax:\n`" + CMD_CFGLOG + ' ' +
                                        CFGLOG_SCMD_ADD_FILTER + " <" +
                                        Miscellaneous.getStringRepresentation(FILTER_TYPES, "|") + "> [#targetChannel] [filterSpec]`\n" +
                                        '`' + CMD_CFGLOG + ' ' +
                                        CFGLOG_SCMD_REMOVE_FILTER + " <" +
                                        Miscellaneous.getStringRepresentation(FILTER_TYPES, "|") + "> [filterSpec]`");
                                }
                                break;
                            case CFGLOG_SCMD_LIST_FILTERS:
                                StringBuilder response = new StringBuilder();

                                if (!parent.getConfig().getChannelIdToLogChannelIdFilters().isEmpty()) {

                                    response.append("I have the following channel log filters saved:\n```\n");

                                    for (Map.Entry<String, String> filterEntry : parent.getConfig().getChannelIdToLogChannelIdFilters().entrySet()) {
                                        TextChannel filterChannel = event.getGuild().getTextChannelById(filterEntry.getKey());
                                        TextChannel targetChannel = event.getGuild().getTextChannelById(filterEntry.getValue());

                                        response.append(filterChannel)
                                            .append(" is filtered to ")
                                            .append(targetChannel)
                                            .append('\n');
                                    }

                                    response.append("```\n\n");
                                }

                                if (!parent.getConfig().getUserIdToLogChannelIdFilters().isEmpty()) {

                                    response.append("I have the following user log filters saved:\n```\n");

                                    for (Map.Entry<String, String> filterEntry : parent.getConfig().getUserIdToLogChannelIdFilters().entrySet()) {
                                        Member filterUser = event.getGuild().getMemberById(filterEntry.getKey());
                                        TextChannel targetChannel = event.getGuild().getTextChannelById(filterEntry.getValue());

                                        response.append(filterUser)
                                            .append(" is filtered to ")
                                            .append(targetChannel)
                                            .append('\n');
                                    }

                                    response.append("```");
                                }

                                if (response.length() == 0) {
                                    response.append("No log filters are currently in effect.");
                                }

                                event.getMessage().getChannel().sendMessage(response.toString()).complete();
                                break;
                        }
                    } else {
                        Miscellaneous.respond(event, "Error: Too few arguments.  Syntax: `" + CMD_CFGLOG + " <" +
                            Miscellaneous.getStringRepresentation(CFGLOG_SCMDS, "|") + ">`");
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
        }
    }
}
