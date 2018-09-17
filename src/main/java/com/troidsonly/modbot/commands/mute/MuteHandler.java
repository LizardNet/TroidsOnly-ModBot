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

package com.troidsonly.modbot.commands.mute;

import java.awt.Color;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.utils.Miscellaneous;

class MuteHandler implements CommandHandler {

    private static final String CMD_MUTE = "mute";
    private static final String CMD_CFGMUTE = "cfgmute";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_MUTE, CMD_CFGMUTE);

    private static final String PERM_MUTE = CMD_MUTE;
    private static final String PERM_CFGMUTE = CMD_CFGMUTE;

    private final MuteListener parent;

    public MuteHandler(MuteListener parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_CFGMUTE)) {
            return ImmutableSet.copyOf(Miscellaneous.getAllRoles(event, false));
        }

        return Collections.emptySet();
    }

    @Override
    public synchronized void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        switch (commands.get(0)) {
            case CMD_CFGMUTE:
                if (parent.getAcl().hasPermission(event.getMember(), PERM_CFGMUTE)) {
                    if (commands.size() == 2) {
                        Role muteRole = event.getGuild().getRolesByName(commands.get(1), true).get(0);

                        if (muteRole.isPublicRole()) {
                            Miscellaneous
                                    .respond(event, "Hmm... it does't make sense to make everyone the mute role....");
                            return;
                        }

                        parent.getConfig().setMuteRoleId(muteRole.getId());
                        parent.sync();

                        Miscellaneous.respond(event, "Mute role set to " + muteRole.toString());
                    } else {
                        String muteRole;

                        if (parent.getConfig().getMuteRoleId() == null) {
                            muteRole = "(not set)";
                        } else {
                            muteRole = event.getGuild().getRoleById(parent.getConfig().getMuteRoleId()).toString();
                        }

                        Miscellaneous.respond(event, "Sorry, I didn't recognize that role name\n" +
                                "Mute role is currently set to: " + muteRole + '\n' +
                                "Roles I recognize: " + Miscellaneous
                                .getStringRepresentation(Miscellaneous.getAllRoles(event, true)) +
                                '\n' + "Syntax: `" + CMD_CFGMUTE + " [roleName]`");
                    }
                } else {
                    Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
                }
                break;
            case CMD_MUTE:
                if (parent.getAcl().hasPermission(event.getMember(), PERM_MUTE)) {

                    if (parent.getMuteRoleId() == null) {
                        Miscellaneous.respond(event, "Please set the mute role before muting anyone.");
                        return;
                    }

                    String[] args = remainder.split(" ");
                    if (args.length != 2) {
                        Miscellaneous.respond(event,
                                "The mute command requires exactly two arguments, the user to be muted and the expiration.");
                        Miscellaneous.respond(event, "Syntax: `" + CMD_MUTE + " + [targetUser] [expiry]");
                        return;
                    }

                    List<User> mentionedUsers = event.getMessage().getMentionedUsers();
                    User targetUser;
                    if (mentionedUsers == null || mentionedUsers.size() != 1) {
                        try {
                            targetUser = event.getJDA().getUserById(args[0]);
                        } catch (Exception e) {
                            Miscellaneous.respond(event, "Failed identifying the target user: " + e.toString());
                            return;
                        }

                        if (targetUser == null) {
                            Miscellaneous.respond(event,
                                    "Could not identify the target user.  Please specify exactly one user by mention or by User ID.");
                            return;
                        }
                    } else {
                        targetUser = mentionedUsers.get(0);
                    }

                    if (targetUser == null) {
                        Miscellaneous.respond(event,
                                "A serious error has occurred. Could not complete action. Please contact my operator.");
                        throw new IllegalStateException("targetUser was somehow null when it should not have been");
                    }

                    MuteConfig.Mute muteInfo = new MuteConfig.Mute();
                    muteInfo.setCreationTime(Instant.now().getEpochSecond());
                    muteInfo.setCreatorUid(event.getMember().getUser().getId());

                    if (args[1].equals("0")) {
                        muteInfo.setExpiryTime(null);
                    } else {
                        ZonedDateTime expiryTime;
                        try {
                            expiryTime = Miscellaneous.processTimeSpec(args[1]);
                        } catch (Exception e) {
                            Miscellaneous.respond(event, "Invalid time spec: " + e.toString());
                            return;
                        }
                        muteInfo.setExpiryTime(expiryTime.toEpochSecond());
                    }

                    Role muteRole = event.getGuild().getRoleById(parent.getMuteRoleId());
                    Member targetMember = event.getGuild().getMember(targetUser);

                    event.getGuild().getController().addRolesToMember(targetMember, muteRole)
                            .reason("Muting user per request of " + event.getMember().getUser().getName())
                            .complete();
                    parent.getConfig().getMutedUserTracker().put(targetUser.getId(), muteInfo);
                    parent.sync();
                    parent.scheduleMutePruning();

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("Mute set against above user");
                    embedBuilder.setAuthor(Miscellaneous.qualifyName(targetMember), null, targetUser.getAvatarUrl());
                    embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous
                            .unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);

                    embedBuilder.addField("Muted by", Miscellaneous.qualifyName(event.getMember()), false);
                    embedBuilder.addField("Muted until", muteInfo.getExpiryTime() == null ? "forever"
                            : Miscellaneous.unixEpochToRfc1123DateTimeString(muteInfo.getExpiryTime()), false);

                    embedBuilder.setColor(new Color(0xCC0000));
                    parent.getLogListener().sendToLog(embedBuilder.build(), event.getMember());
                } else {
                    Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
                }
                break;
        }
    }
}
