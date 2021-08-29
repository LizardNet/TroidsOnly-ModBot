/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2021 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.reactforroles;

import com.google.common.collect.ImmutableSet;
import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.utils.Miscellaneous;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;

class ReactForRolesHandler implements CommandHandler {

    private static final String CMD_REACTROLES = "reactroles";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_REACTROLES);

    private static final String REACTROLES_MESSAGE = "message";
    private static final String REACTROLES_MAPROLE = "maprole";
    private static final Set<String> REACTROLES_SUBCMDS = ImmutableSet.of(REACTROLES_MESSAGE, REACTROLES_MAPROLE);

    private static final String OP_ADD = "add";
    private static final String OP_EDIT = "edit"; // Only applies to the message subcommand
    private static final String OP_REMOVE = "remove";
    private static final Set<String> SCMD_MESSAGE_OPS = ImmutableSet.of(OP_ADD, OP_EDIT, OP_REMOVE);
    private static final Set<String> SCMD_MAPROLE_OPS = ImmutableSet.of(OP_ADD, OP_REMOVE);

    private static final String PERM_REACTROLES = "reactroles";
    private static final String E_PERMFAIL = ModBot.PERMFAIL_MESSAGE;

    private static final String HELP_USAGE = "Syntax:\n"
            + "To add a new react-for-roles message: `" + CMD_REACTROLES + ' ' + REACTROLES_MESSAGE + ' ' + OP_ADD
            + " [#textChannel] [message text]`\n"
            + "To edit an existing react-for-roles message: `" + CMD_REACTROLES + ' ' + REACTROLES_MESSAGE + ' '
            + OP_EDIT + " [messageId] [new text]`\n"
            + "To remove a react-for-roles message: `" + CMD_REACTROLES + ' ' + REACTROLES_MESSAGE + ' ' + OP_REMOVE
            + " [messageId]` (or just delete the message)\n"
            + "To add a role-to-reaction mapping to a message: `" + CMD_REACTROLES + ' ' + REACTROLES_MAPROLE + ' '
            + OP_ADD + " [messageId] [emote] [roleIdOrName]`\n"
            + "To remove a role-to-reaction mapping from a message: `" + CMD_REACTROLES + ' ' + REACTROLES_MAPROLE + ' '
            + OP_REMOVE + " [messageId] [emote]` (or just remove the emote from the message)\n\n"
            + "To get the ID of a react-to-roles message, please see: https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-\n"
            + "Remember to include the leading `#` when specifying the name of a text channel!";

    private final ReactForRolesListener parent;

    public ReactForRolesHandler(ReactForRolesListener parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_REACTROLES)) {
            return REACTROLES_SUBCMDS;
        }

        if (commands.size() == 2) {
            if (commands.get(1).equals(REACTROLES_MESSAGE)) {
                return SCMD_MESSAGE_OPS;
            }
            if (commands.get(1).equals(REACTROLES_MAPROLE)) {
                return SCMD_MAPROLE_OPS;
            }
        }

        return Collections.emptySet();
    }

    @Override
    public synchronized void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty() || !commands.get(0).equals(CMD_REACTROLES)) {
            return;
        }

        try {
            remainder = remainder.trim();

            if (commands.size() < 3) {
                sendErrorWithSyntaxHelp("Too few or incorrect parameters. ", event);
                return;
            }

            if (!parent.getAcl().hasPermission(event.getMember(), PERM_REACTROLES)) {
                Miscellaneous.respond(event, E_PERMFAIL);
                return;
            }

            switch (commands.get(1)) {
                case REACTROLES_MESSAGE:
                    switch (commands.get(2)) {
                        case OP_ADD:
                            handleMessageAddCommand(event, commands, remainder);
                            break;
                        case OP_EDIT:
                            handleMessageEditCommand(event, commands, remainder);
                            break;
                        case OP_REMOVE:
                            handleMessageRemoveCommand(event, remainder);
                            break;
                    }
                    break;
                case REACTROLES_MAPROLE:
                    switch (commands.get(2)) {
                        case OP_ADD:
                            handleMaproleAddCommand(event, remainder);
                            break;
                        case OP_REMOVE:
                            handleMaproleRemoveCommand(event, commands, remainder);
                            break;
                    }
                    break;
            }
        } catch (Exception e) {
            sendErrorWithSyntaxHelp(e.toString(), event);
            System.err.println(e.toString());
            e.printStackTrace(System.err);
        }
    }

    private static void sendErrorWithSyntaxHelp(String errorMessage, GuildMessageReceivedEvent event) {
        Objects.requireNonNull(errorMessage);

        String message = errorMessage + ". " + HELP_USAGE;
        Miscellaneous.respond(event, message);
    }

    private void handleMessageAddCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 2) {
            throw new IllegalArgumentException("Too few parameters");
        }

        TextChannel textChannel = Miscellaneous.resolveTextChannelName(event, args[0]);
        String message = Miscellaneous.discardMessageParts(event.getMessage().getContentRaw(), commands.size() + 1)
                .trim();

        if (textChannel == null) {
            return;
        }

        Message discordMessage = textChannel.sendMessage("This is not a text message").complete();
        String messageId = discordMessage.getId();

        ReactForRolesMessageConfig messageConfig = parent.getConfig()
                .addAndGetNewReactForRolesMessage(messageId, textChannel.getId(), message);
        parent.sync();

        discordMessage.editMessage(messageConfig.renderMessage(event.getGuild())).complete();

        Miscellaneous.respond(event, "Created a new react-for-roles message with ID `" + messageId + "`.\n"
                + "To add role-to-reaction mappings, use the command: `" + CMD_REACTROLES + ' ' + REACTROLES_MAPROLE
                + ' ' + OP_ADD + ' ' + messageId + " [emote] [roleIdOrName]`");
    }

    private void handleMessageEditCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 2) {
            throw new IllegalArgumentException("Too few parameters");
        }

        String messageId = args[0];
        String newMessage = Miscellaneous.discardMessageParts(event.getMessage().getContentRaw(), commands.size() + 1)
                .trim();

        ReactForRolesMessageConfig messageConfig = parent.getConfig().getMessagesToConfigs().get(messageId);
        if (messageConfig == null) {
            throw new IllegalArgumentException("The provided messageId (`" + messageId
                    + "`) does not appear to be a valid react-for-roles message.");
        }

        TextChannel textChannel = event.getGuild().getTextChannelById(messageConfig.getTextChannelId());
        if (textChannel == null) {
            return;
        }

        Message discordMessage = textChannel.retrieveMessageById(messageId).complete();

        messageConfig.setMessageText(newMessage);
        parent.sync();
        discordMessage.editMessage(messageConfig.renderMessage(event.getGuild())).complete();

        Miscellaneous.respond(event, "Successfully updated message with ID `" + messageId + "`.");
    }

    private void handleMessageRemoveCommand(GuildMessageReceivedEvent event,String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 1) {
            throw new IllegalArgumentException("Too few parameters");
        }

        String messageId = args[0];
        ReactForRolesMessageConfig messageConfig = parent.getConfig().getMessagesToConfigs().get(messageId);
        if (messageConfig == null) {
            throw new IllegalArgumentException("The provided messageId (`" + messageId
                    + "`) does not appear to be a valid react-for-roles message.");
        }

        TextChannel textChannel = event.getGuild().getTextChannelById(messageConfig.getTextChannelId());
        if (textChannel == null) {
            return;
        }

        Message discordMessage = textChannel.retrieveMessageById(messageId).complete();

        discordMessage.delete()
                .reason("Requested by " + Miscellaneous.qualifyName(event.getMember()))
                .complete();
        parent.getConfig().removeReactForRolesMessageById(messageId);
        parent.sync();

        Miscellaneous.respond(event, "React-for-roles message with ID `" + messageId + "` was successfully"
                + " deleted.");
    }

    private void handleMaproleAddCommand(GuildMessageReceivedEvent event, String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 1) {
            throw new IllegalArgumentException("Too few parameters");
        }

        String messageId = args[0];
        String emoteRaw = args[1];
        Emote emote = null;

        if (emoteRaw.length() > 4) {
            // Assuming this is a custom emote
            String emoteId = emoteRaw.replaceAll(":", "");
            emote = event.getGuild().getEmoteCache().stream()
                    .filter(e -> e.getName().equalsIgnoreCase(emoteId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Could not find the emote ID for `"
                            + emoteId + "`. Is it from this server?"));

            emoteRaw = emote.getId();
        }

        remainder = remainder.substring(args[0].length() + args[1].length() + 1).trim();
        List<Role> roles = event.getGuild().getRolesByName(remainder, true);

        if (roles.size() != 1) {
            throw new IllegalArgumentException("Could not resolve the role name `" + remainder + "`.");
        }

        String roleId = roles.get(0).getId();

        ReactForRolesMessageConfig messageConfig = parent.getConfig().getMessagesToConfigs().get(messageId);

        if (messageConfig == null) {
            throw new IllegalArgumentException("The provided messageId (`" + messageId
                    + "`) does not appear to be a valid react-for-roles message.");
        }

        messageConfig.addEmoteToRoleIdMapping(emoteRaw, roleId);
        parent.sync();
        String newMessage = messageConfig.renderMessage(event.getGuild());

        TextChannel textChannel = event.getGuild().getTextChannelById(messageConfig.getTextChannelId());
        if (textChannel == null) {
            return;
        }

        Message discordMessage = textChannel.retrieveMessageById(messageId).complete();
        discordMessage.editMessage(newMessage).complete();

        if (emote == null) {
            discordMessage.addReaction(emoteRaw).complete();
            Miscellaneous.respond(event, "Successfully added reaction-to-role mapping! Emote " + emoteRaw
                    + " will grant the " + remainder + "role.");
        } else {
            discordMessage.addReaction(emote).complete();
            Miscellaneous.respond(event, "Successfully added reaction-to-role mapping! Emote "
                    + emote.getAsMention() + " will grant the " + remainder + "role.");
        }
    }

    private void handleMaproleRemoveCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        Miscellaneous.respond(event, "handleMaproleRemoveCommand() called!");
    }
}
