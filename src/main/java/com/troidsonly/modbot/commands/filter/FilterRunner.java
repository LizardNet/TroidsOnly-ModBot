/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2018 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.filter;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.utils.Miscellaneous;

public class FilterRunner implements Runnable {
    private final FilterListener parent;
    private final GuildMessageReceivedEvent event;
    private final Member member;
    private final String message;

    public FilterRunner(FilterListener parent, GuildMessageReceivedEvent event, Member member, String message) {
        this.parent = Objects.requireNonNull(parent);
        this.event = Objects.requireNonNull(event);
        this.member = Objects.requireNonNull(member);
        this.message = Objects.requireNonNull(message);
    }

    @Override
    public void run() {
        // For thread safety, make a copy of the filter list.  This is computationally more expensive, but means we
        // don't have to block on synchronization for as long and minimizes the effect of multiple FilterRunner threads
        // blocking each other.

        List<RegexFilter> filterList;

        synchronized (parent.getFilterRepository()) {
            filterList = new ArrayList<>(parent.getFilterRepository().getFilterList());
        }

        for (RegexFilter filter : filterList) {
            Pattern pattern = filter.getPattern();

            PatternMatchCallable pmc = new PatternMatchCallable(pattern, message);

            Future<Boolean> future = parent.getExecutorService().submit(pmc);

            boolean response;

            try {
                response = future.get(1, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                parent.getLogger().sendToLog("**WARNING:** `" + filter.getRegex() + "` timed out during processing");
                if (!future.cancel(true)) {
                    parent.getLogger().sendToLog("**WARNING:** Attempt to cancel pending regex operations DID NOT succeed.");
                }

                continue;
            } catch (Exception e) {
                parent.getLogger().sendToLog("An error occurred while executing filter `" + filter.getRegex() + "`: " + e.toString());
                continue;
            }

            if (response) {
                EmbedBuilder embedBuilder = new EmbedBuilder();

                // We have a match!!
                embedBuilder.setAuthor(Miscellaneous.qualifyName(member), null, member.getUser().getAvatarUrl());
                embedBuilder.setDescription('`' + filter.getRegex() + '`');
                embedBuilder.setTimestamp(Instant.now());
                embedBuilder.setFooter(getClass().getSimpleName(), null);
                embedBuilder.addField("Offending message", message, false);
                embedBuilder.addField("Tripped filter comment", filter.getComment(), false);
                embedBuilder.addField("Tripped filter action", filter.getAction().toString(), false);
                embedBuilder.addField("Tripped filter added by", event.getGuild().getMemberById(filter.getCreatorUid()).getEffectiveName(), false);
                embedBuilder.addField("Tripped filter added at", Miscellaneous.unixEpochToRfc1123DateTimeString(filter.getCreationTime()), false);

                // First, check if the match is against someone who has permission to change filters - if so, they're considered exempt.  Always log only.
                if (parent.getAcl().hasPermission(member, FilterCommandHandler.PERM_FILTER)) {
                    embedBuilder.setTitle("User tripped a filter but is exempt");
                    embedBuilder.addField("Action taken", "Logged only - user who tripped filter is authorized to modify them, so they are considered exempt.", false);
                    embedBuilder.setColor(new Color(0xAAAAAA));
                } else {
                    embedBuilder.setTitle("User tripped a message filter!");

                    switch (filter.getAction()) {
                        case LOG_ONLY:
                            embedBuilder.addField("Action taken", "Logged only", false);
                            break;
                        case WARN_USER:
                            performActionsWithErrorHandling(embedBuilder, "User warned, message not deleted",
                                () -> actionSendPrivateMessage(
                                "Hello.  This message is to inform you that a message you sent to the " + event.getGuild().getName() + " server tripped " +
                                "an automated message filter.  No action has been taken against you and your message has not been deleted; however, the moderators have been " +
                                "notified of this incident.\n" +
                                "Offending message: `" + message + "`\n" +
                                "Filter comment: " + filter.getComment()));
                            break;
                        case DELETE_MESSAGE_SILENT:
                            performActionsWithErrorHandling(embedBuilder, "Message deleted without notifying user",
                                this::actionDeleteMessage);
                            break;
                        case DELETE_MESSAGE_AND_WARN:
                            performActionsWithErrorHandling(embedBuilder, "Message deleted and user notified",
                                this::actionDeleteMessage,
                                () -> actionSendPrivateMessage(
                                    "Hello.  This message is to inform you that a message you sent to the " + event.getGuild().getName() + " server tripped " +
                                    "an automated message filter.  The message has been automatically deleted and the moderators have been notified of this incident.\n" +
                                    "Offending message: `" + message + "`\n" +
                                    "Filter comment: " + filter.getComment()));
                            break;
                        case DELETE_MESSAGE_AND_KICK:
                            performActionsWithErrorHandling(embedBuilder, "Message deleted and sending user kicked with direct notification",
                                this::actionDeleteMessage,
                                () -> actionSendPrivateMessage(
                                    "Hello.  This message is to inform you that a message you sent to the " + event.getGuild().getName() + " server tripped " +
                                    "an automated message filter.  The message has been automatically deleted and you have been automatically kicked from the server.\n" +
                                    "Offending message: `" + message + "`\n" +
                                    "Filter comment: " + filter.getComment() + '\n' +
                                    "Please take a moment to consider the comment before attempting to rejoin the server.  Remember that continued violation of server rules " +
                                    "may result in a ban."),
                                this::actionKickUser);
                            break;
                        case DELETE_MESSAGE_AND_CRYO:
                            performActionsWithErrorHandling(embedBuilder, "Message deleted and user cryo'd with direct notification",
                                this::actionDeleteMessage,
                                () -> actionSendPrivateMessage(
                                    "Hello.  This message is to inform you that a message you sent to the " + event.getGuild().getName() + " server tripped " +
                                    "an automated message filter.  The message has been automatically deleted and you have been automatically quieted - though you may remain " +
                                    "on the server and continue to *read* messages, you may not participate or send messages for the time being.\n" +
                                    "Offending message: `" + message + "`\n" +
                                    "Filter comment: " + filter.getComment() + '\n' +
                                    "Please contact a server moderator directly if you have any questions, or if you would like to enquire about when your quiet will be removed."),
                                this::actionCryoUser);
                            break;
                        case DELETE_MESSAGE_AND_BAN:
                            performActionsWithErrorHandling(embedBuilder, "Message deleted and user banned with direct notification",
                                this::actionDeleteMessage,
                                () -> actionSendPrivateMessage(
                                    "Hello.  This message is to inform you that a message you sent to the " + event.getGuild().getName() + " server tripped " +
                                    "an automated message filter.  The message has been automatically deleted and you have been automatically banned from the server for violation " +
                                    "of the server rules.\n" +
                                    "Offending message: `" + message + "`\n" +
                                    "Filter comment: " + filter.getComment()),
                                this::actionBanUser);
                            break;
                    }

                    embedBuilder.setColor(new Color(0xCC0000));
                }

                parent.getLogger().sendToLog(embedBuilder.build(), member);
            }
        }
    }

    private String actionSendPrivateMessage(String message) {
        try {
            PrivateChannel pc = member.getUser().openPrivateChannel().complete();
            pc.sendMessage(message).complete();
        } catch (Exception e) {
            return "Failed to send private message: " + e.toString();
        }

        return null;
    }

    private String actionDeleteMessage() {
        try {
            event.getMessage().delete().reason("Automatic message deletion due to message filter violation").complete();
        } catch (Exception e) {
            return "Failed to delete message: " + e.toString();
        }

        return null;
    }

    private String actionKickUser() {
        try {
            event.getGuild().getController().kick(member).reason("Automatic kick due to message filter violation").complete();
        } catch (Exception e) {
            return "Failed to kick user: " + e.toString();
        }

        return null;
    }

    private String actionCryoUser() {
        try {
            event.getGuild().getController()
                .addRolesToMember(member, event.getGuild().getRoleById(parent.getCryoHandler().getCryoRoleId()))
                .reason("Automatic cryo due to message filter violation")
                .complete();
        } catch (Exception e) {
            return "Failed to cryo user: " + e.toString();
        }

        return null;
    }

    private String actionBanUser() {
        try {
            event.getGuild().getController().ban(member, 0, "Automatic ban due to message filter violation").reason("Automatic ban due to message filter violation").complete();
        } catch (Exception e) {
            return "Failed to ban user: " + e.toString();
        }

        return null;
    }

    @SafeVarargs
    private static void performActionsWithErrorHandling(EmbedBuilder embedBuilder, String outputIfSuccess, Supplier<String>... actions) {
        List<String> retval = new ArrayList<>();

        for (Supplier<String> action : actions) {
            String output = action.get();

            if (output != null) {
                retval.add(output);
            }
        }

        if (!retval.isEmpty()) {
            StringBuilder sb = new StringBuilder("One or more actions **failed**:");

            //noinspection SimplifyStreamApiCallChains
            retval.stream()
                .forEachOrdered(s -> sb.append("\n* ").append(s));

            embedBuilder.addField("Action taken", sb.toString(), false);
        } else {
            embedBuilder.addField("Action taken", outputIfSuccess, false);
        }
    }
}
