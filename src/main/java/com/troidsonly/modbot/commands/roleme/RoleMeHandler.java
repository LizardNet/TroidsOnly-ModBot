/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2018 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.roleme;

import java.awt.Color;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.utils.Miscellaneous;

class RoleMeHandler implements CommandHandler {

    private static final String CMD_ROLEME = "acceptrules";
    private static final String CMD_CFGROLEME = "cfgroleme";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_ROLEME, CMD_CFGROLEME);

    private static final String CONFIG_ROLE = "role";
    private static final String CONFIG_CHANNEL = "channel";
    private static final String CONFIG_WELCOME_MESSAGES = "welcomes";
    private static final String CONFIG_AUTOSEND_MESSAGE = "autosend";
    private static final Set<String> CONFIGS = ImmutableSet.of(CONFIG_ROLE, CONFIG_CHANNEL, CONFIG_WELCOME_MESSAGES);

    private static final String CFG_OP_ADD = "add";
    private static final String CFG_OP_REMOVE = "remove";
    private static final String CFG_OP_LIST = "list";
    private static final String CFG_OP_SET = "set";
    private static final Set<String> CFG_OPS_SET_AND_REMOVE = ImmutableSet.of(CFG_OP_SET, CFG_OP_REMOVE);
    private static final Set<String> CFG_OPS_ADD_REMOVE_AND_LIST = ImmutableSet
            .of(CFG_OP_ADD, CFG_OP_REMOVE, CFG_OP_LIST);

    private static final Set<String> CFGS_WITH_SET_AND_REMOVE = ImmutableSet
            .of(CONFIG_ROLE, CONFIG_CHANNEL, CONFIG_AUTOSEND_MESSAGE);
    private static final Set<String> CFGS_WITH_ADD_REMOVE_AND_LIST = ImmutableSet.of(CONFIG_WELCOME_MESSAGES);

    private final RoleMeListener parent;

    public RoleMeHandler(RoleMeListener parent) {
        this.parent = parent;
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (CMD_CFGROLEME.equals(commands.get(0))) {
            if (commands.size() == 2) {
                if (CFGS_WITH_SET_AND_REMOVE.contains(commands.get(1))) {
                    return CFG_OPS_SET_AND_REMOVE;
                }

                if (CFGS_WITH_ADD_REMOVE_AND_LIST.contains(commands.get(1))) {
                    return CFG_OPS_ADD_REMOVE_AND_LIST;
                }
            }

            return CONFIGS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        switch (commands.get(0)) {
            case CMD_ROLEME:
                RoleMeConfig config = parent.getConfig();
                if (config.getRoleMeChannelId() == null || config.getRoleIdToGrant() == null) {
                    System.err.println(Miscellaneous.qualifyName(event.getMember())
                            + " tried to use roleme, but the module is disabled.");
                    return;
                }

                // Verify that the command was given in the correct channel
                if (!event.getChannel().getId().equals(config.getRoleMeChannelId())) {
                    System.err.println(Miscellaneous.qualifyName(event.getMember())
                            + " tried to use roleme, but the command was given in the wrong channel (" + event
                            .getChannel().getName() + ").");
                    return;
                }

                // For safety, verify that the user has *no* roles at all
                if (!event.getMember().getRoles().isEmpty()) {
                    String message = "Sorry, but I can't assign you a role automatically because you seem to already "
                            + "have one or more roles.  Please contact a server moderator.";
                    event.getAuthor().openPrivateChannel().complete().sendMessage(message).complete();
                    System.err.println(Miscellaneous.qualifyName(event.getMember())
                            + " tried to use roleme but already has roles.");
                    return;
                }

                // TODO: Check the user against the name filter here

                try {
                    Role roleToAssign = event.getGuild().getRoleById(config.getRoleIdToGrant());

                    if (roleToAssign == null) {
                        throw new Exception("The role specified in configuration could not be found.");
                    }

                    event.getGuild()
                            .getController()
                            .addRolesToMember(event.getMember(), roleToAssign)
                            .reason("Requested by user using automated roleme functionality")
                            .complete();

                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("User automatically granted role by using " + CMD_ROLEME + " command");
                    embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null,
                            event.getAuthor().getAvatarUrl());
                    embedBuilder.setFooter(
                            getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(
                                    Instant.now().getEpochSecond()), null);
                    embedBuilder.setColor(new Color(0x00CC00));
                    parent.getLogger().sendToLog(embedBuilder.build(), event.getMember(), event.getChannel());

                    if (config.getWelcomeChannelId() != null && !config.getWelcomeMessages().isEmpty()) {
                        sendWelcomeMessage(event, config.getWelcomeChannelId(), config.getWelcomeMessages());
                    }
                } catch (Exception e) {
                    String message = "Sorry, an error occurred when I tried to grant you your role: " + e.toString()
                            + ".  Please contact a server moderator; apologies for the inconvenience.";
                    event.getAuthor().openPrivateChannel().complete().sendMessage(message).complete();
                    System.err.println("An error occurred when " + Miscellaneous.qualifyName(event.getMember())
                            + " attempted to use roleme: " + e.toString());
                    e.printStackTrace(System.err);
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    embedBuilder.setTitle("Error occurred while attempting to grant role to user");
                    embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null,
                            event.getAuthor().getAvatarUrl());
                    embedBuilder.setFooter(
                            getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(
                                    Instant.now().getEpochSecond()), null);
                    embedBuilder.setDescription(e.toString());
                    embedBuilder.setColor(new Color(0xCC0000));
                    parent.getLogger().sendToLog(embedBuilder.build(), event.getMember(), event.getChannel());
                }
                break;
        }
    }

    private static void sendWelcomeMessage(GuildMessageReceivedEvent event, String welcomeChannelId, List<String> welcomeMessages) {
        Objects.requireNonNull(event);
        Objects.requireNonNull(welcomeChannelId);
        Objects.requireNonNull(welcomeMessages);

        if (welcomeMessages.isEmpty()) {
            throw new IllegalArgumentException("welcomeMessages may not be an empty list.");
        }

        Random rand = new Random();
        String message = welcomeMessages.get(rand.nextInt(welcomeMessages.size()));
        String mention = event.getMember().getAsMention();
        message = String.format(message, mention);
        TextChannel channel = event.getGuild().getTextChannelById(welcomeChannelId);

        if (channel == null) {
            throw new IllegalArgumentException("Could not find channel with ID " + welcomeChannelId);
        }

        channel.sendMessage(message).queue();
    }
}
