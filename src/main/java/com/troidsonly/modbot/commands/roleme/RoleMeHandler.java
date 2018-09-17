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
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.utils.Miscellaneous;

class RoleMeHandler implements CommandHandler {

    private static final String CMD_ROLEME = "acceptrules";
    private static final String CMD_CFGROLEME = "cfgroleme";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_ROLEME, CMD_CFGROLEME);

    private static final String CONFIG_ROLE = "role";
    private static final String CONFIG_ROLEME_CHANNEL = "roleme-channel";
    private static final String CONFIG_WELCOME_CHANNEL = "welcome-channel";
    private static final String CONFIG_WELCOME_MESSAGES = "welcomes";
    private static final String CONFIG_AUTOSEND_MESSAGE = "autosend";
    private static final Set<String> CONFIGS = ImmutableSet
            .of(CONFIG_ROLE, CONFIG_ROLEME_CHANNEL, CONFIG_WELCOME_CHANNEL, CONFIG_WELCOME_MESSAGES,
                    CONFIG_AUTOSEND_MESSAGE);

    private static final String CFG_OP_ADD = "add";
    private static final String CFG_OP_REMOVE = "remove";
    private static final String CFG_OP_LIST = "list";
    private static final String CFG_OP_SET = "set";
    private static final Set<String> CFG_OPS_SET_AND_REMOVE = ImmutableSet.of(CFG_OP_SET, CFG_OP_REMOVE);
    private static final Set<String> CFG_OPS_ADD_REMOVE_AND_LIST = ImmutableSet
            .of(CFG_OP_ADD, CFG_OP_REMOVE, CFG_OP_LIST);

    private static final Set<String> CFGS_WITH_SET_AND_REMOVE = ImmutableSet
            .of(CONFIG_ROLE, CONFIG_ROLEME_CHANNEL, CONFIG_WELCOME_CHANNEL, CONFIG_AUTOSEND_MESSAGE);
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

                throw new IllegalStateException("We got a bug here!");
            } else if (commands.size() >= 3) {
                if (CONFIG_ROLE.equals(commands.get(1)) && CFG_OP_SET.equals(commands.get(2))) {
                    return ImmutableSet.copyOf(Miscellaneous.getAllRoles(event, false));
                } else if (
                        (CONFIG_ROLEME_CHANNEL.equals(commands.get(1))
                                || CONFIG_WELCOME_CHANNEL.equals(commands.get(1))
                        )
                                && CFG_OP_SET.equals(commands.get(2))) {
                    return ImmutableSet.copyOf(event.getGuild().getTextChannels()
                            .stream()
                            .map(Channel::getName)
                            .collect(Collectors.toList())
                    );
                }

                return Collections.emptySet();
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

        final RoleMeConfig config = parent.getConfig();
        remainder = remainder.trim();

        switch (commands.get(0)) {
            case CMD_ROLEME:
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

            case CMD_CFGROLEME:
                if (!parent.getAcl().hasPermission(event.getMember(), CMD_CFGROLEME)) {
                    Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
                    return;
                }

                if (commands.size() >= 2) {
                    if (commands.size() >= 3) {
                        switch (commands.get(1)) {
                            case CONFIG_ROLE:
                                switch (commands.get(2)) {
                                    case CFG_OP_SET:
                                        if (commands.size() == 4) {
                                            Role role = event.getGuild().getRolesByName(commands.get(3), true).get(0);
                                            if (role.isPublicRole()) {
                                                Miscellaneous.respond(event,
                                                        "Everyone is automatically in the everyone role already....");
                                                return;
                                            }
                                            config.setRoleIdToGrant(role.getId());
                                            parent.sync();
                                        } else {
                                            Set<String> allRoles = Miscellaneous.getAllRoles(event, true);
                                            Miscellaneous.respond(event,
                                                    "You must specify the role to be granted to new users using roleme. Known roles are: "
                                                            + Miscellaneous.asSortedList(allRoles));
                                        }
                                        break;
                                    case CFG_OP_REMOVE:
                                        config.setRoleIdToGrant(null);
                                        parent.sync();
                                        Miscellaneous.respond(event,
                                                "Roleme role has been removed; roleme functionality is now disabled.");
                                }
                                break;
                            case CONFIG_ROLEME_CHANNEL:
                                switch (commands.get(2)) {
                                    case CFG_OP_SET:
                                        if (commands.size() == 4) {
                                            setChannel(event, "Roleme", commands.get(3));
                                        } else {
                                            Set<String> allChannels = event.getGuild().getTextChannels().stream()
                                                    .map(Channel::getName)
                                                    .collect(Collectors.toSet());
                                            Miscellaneous.respond(event,
                                                    "You must specify the channel to be set. Known channels are: "
                                                            + Miscellaneous.asSortedList(allChannels));
                                        }
                                        break;
                                    case CFG_OP_REMOVE:
                                        setChannel(event, "Roleme", null);
                                }
                                break;
                            case CONFIG_WELCOME_CHANNEL:
                                switch (commands.get(2)) {
                                    case CFG_OP_SET:
                                        if (commands.size() == 4) {
                                            setChannel(event, "Welcome", commands.get(3));
                                        } else {
                                            Set<String> allChannels = event.getGuild().getTextChannels().stream()
                                                    .map(Channel::getName)
                                                    .collect(Collectors.toSet());
                                            Miscellaneous.respond(event,
                                                    "You must specify the channel to be set. Known channels are: "
                                                            + Miscellaneous.asSortedList(allChannels));
                                        }
                                        break;
                                    case CFG_OP_REMOVE:
                                        setChannel(event, "Welcome", null);
                                }
                                break;
                            case CONFIG_AUTOSEND_MESSAGE:
                                switch (commands.get(2)) {
                                    case CFG_OP_SET:
                                        if (!remainder.isEmpty()) {
                                            config.setAutoSendMessage(remainder);
                                            parent.sync();
                                            Miscellaneous.respond(event, "Autosend message set.");
                                        } else {
                                            Miscellaneous.respond(event,
                                                    "You must specify the autosend message to be sent.");
                                        }
                                        break;
                                    case CFG_OP_REMOVE:
                                        Miscellaneous.respond(event, "Autosend message removed and disabled.");
                                        config.setAutoSendMessage(null);
                                        parent.sync();
                                }
                                break;
                            case CONFIG_WELCOME_MESSAGES:
                                switch (commands.get(2)) {
                                    case CFG_OP_LIST:
                                        if (config.getWelcomeMessages().isEmpty()) {
                                            Miscellaneous.respond(event, "No welcome messages have been defined.");
                                            return;
                                        }

                                        PrivateChannel dm;
                                        try {
                                            dm = event.getMember().getUser().openPrivateChannel().complete();
                                        } catch (Exception e) {
                                            Miscellaneous
                                                    .respond(event, "Failed to open a DM to you: `" + e.toString());
                                            return;
                                        }

                                        Miscellaneous
                                                .respond(event, "DMing you a list of welcome messages I have saved.");
                                        StringBuilder welcomes = new StringBuilder(
                                                "Saved welcome messages listed below. The number is not part of the message but can be passed to the delete command to remove that message. Remember that, in the messages, `%1$s` will be replaced with the new user's name.");
                                        for (int i = 0; i < config.getWelcomeMessages().size(); i++) {
                                            welcomes.append(i)
                                                    .append(": ")
                                                    .append(config.getWelcomeMessages().get(i))
                                                    .append('\n');
                                        }
                                        dm.sendMessage(welcomes.toString().trim()).queue();
                                        break;
                                    case CFG_OP_REMOVE:
                                        final int msgIndex;

                                        try {
                                            msgIndex = Integer.parseUnsignedInt(remainder);
                                        } catch (NumberFormatException e) {
                                            Miscellaneous.respond(event, "Invalid message number " + remainder);
                                            return;
                                        }

                                        try {
                                            String message = config.getWelcomeMessages().remove(msgIndex);
                                            Miscellaneous.respond(event,
                                                    "Successfully removed the welcome message `" + message + '`');
                                        } catch (Exception e) {
                                            Miscellaneous.respond(event,
                                                    "Failed to delete the requested welcome messgae: " + e.toString());
                                            return;
                                        }

                                        parent.sync();
                                        break;
                                    case CFG_OP_ADD:
                                        if (!remainder.isEmpty()) {
                                            config.getWelcomeMessages().add(remainder);
                                            parent.sync();
                                            Miscellaneous.respond(event, "Successfully added to the welcome messages");
                                        } else {
                                            Miscellaneous
                                                    .respond(event, "You must specify the message you want to add!");
                                        }
                                }
                        }
                    } else {
                        Set<String> nextPossibleCommands;
                        if (CFGS_WITH_SET_AND_REMOVE.contains(commands.get(1))) {
                            nextPossibleCommands = CFG_OPS_SET_AND_REMOVE;
                        } else if (CFGS_WITH_ADD_REMOVE_AND_LIST.contains(commands.get(1))) {
                            nextPossibleCommands = CFG_OPS_ADD_REMOVE_AND_LIST;
                        } else {
                            throw new IllegalStateException("We got a bug here!");
                        }
                        Miscellaneous.respond(event,
                                "Syntax error: Too few arguments. Usage: `" + CMD_CFGROLEME + ' ' + commands.get(1)
                                        + " <" + Miscellaneous.getStringRepresentation(nextPossibleCommands, "|")
                                        + ">`");
                    }
                } else {
                    String configState = "Current configuration state:\nAutomatic roleme is **" + (
                            config.getRoleIdToGrant() != null && config.getRoleMeChannelId() != null ? "ENABLED"
                                    : "DISABLED") + "**\n";
                    configState += "Role to be granted: " + (config.getRoleIdToGrant() == null ? "(not set)"
                            : event.getGuild().getRoleById(config.getRoleIdToGrant())) + '\n';
                    configState += "Roleme channel: " + (config.getRoleMeChannelId() == null ? "(not set)"
                            : event.getGuild().getTextChannelById(config.getRoleMeChannelId())) + '\n';
                    configState += "A welcome message will be sent to channel: " + (config.getWelcomeChannelId() == null
                            ? "(feature disabled)" : event.getGuild().getTextChannelById(config.getWelcomeChannelId()))
                            + '\n';
                    configState += "Welcome messages defined: " + config.getWelcomeMessages().size() + (
                            config.getWelcomeMessages().isEmpty() ? " (feature disabled)" : "") + '\n';
                    configState += "Message DM'd to user on server join: " + (config.getAutoSendMessage() == null
                            ? "(feature disabled)" : config.getAutoSendMessage());

                    Miscellaneous.respond(event, configState);
                    Miscellaneous.respond(event,
                            "To change this configuration: `" + CMD_CFGROLEME + " <" + Miscellaneous
                                    .getStringRepresentation(CONFIGS, "|") + ">`");
                }
        }
    }

    private static void sendWelcomeMessage(GuildMessageReceivedEvent event, String welcomeChannelId,
            List<String> welcomeMessages) {
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

    private void setChannel(GuildMessageReceivedEvent event, String channelType, String channelName) {
        Objects.requireNonNull(channelType);
        final String channelId;
        if (channelName == null) {
            channelId = null;
        } else {
            channelId = event.getGuild().getTextChannelsByName(channelName, true).get(0).getId();
        }

        if (channelType.equals("Welcome")) {
            parent.getConfig().setWelcomeChannelId(channelId);
        } else if (channelType.equals("Roleme")) {
            parent.getConfig().setRoleMeChannelId(channelId);
        } else {
            throw new IllegalArgumentException("Illegal channel type " + channelType);
        }

        parent.sync();

        if (channelId == null) {
            Miscellaneous.respond(event,
                    channelType + " unset." + (channelType.equals("Roleme") ? " Roleme funcionality is now disabled."
                            : ""));
        } else {
            Miscellaneous.respond(event, channelType + " channel set to " + channelName);
        }
    }
}
