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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.utils.Miscellaneous;

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
            + OP_ADD + " [roleName] [messageId] [emote]`\n"
            + "To remove a role-to-reaction mapping from a message: `" + CMD_REACTROLES + ' ' + REACTROLES_MAPROLE + ' '
            + OP_REMOVE + " [messageId] [emote]` (or just remove the emote from the message)\n\n"
            + "To get the ID of a react-to-roles message, please see: https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-\n"
            + "When specifying a role name, remove all spaces and run it together as a single word (for example, \"Server Admin\" becomes \"ServerAdmin\")\n"
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

        if (commands.size() == 3
                && commands.get(0).equals(CMD_REACTROLES)
                && commands.get(1).equals(REACTROLES_MAPROLE)
                && commands.get(2).equals(OP_ADD)
        ) {
            return ImmutableSet.copyOf(Miscellaneous.getAllRoles(event));
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
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
                            if (commands.size() == 4) {
                                handleMaproleAddCommand(event, commands, remainder);
                            } else {
                                sendErrorWithSyntaxHelp("Unrecognized or missing role name. Roles I recognize: "
                                                + Miscellaneous.getStringRepresentation(Miscellaneous.getAllRoles(event)),
                                        event);
                            }
                            break;
                        case OP_REMOVE:
                            handleMaproleRemoveCommand(event, remainder);
                            break;
                    }
                    break;
            }
        } catch (Exception e) {
            sendErrorWithSyntaxHelp(e.toString(), event);
            //noinspection UnnecessaryToStringCall
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

        String messageId = parent.createNewReactForRolesMessage(event.getGuild(), textChannel, message);

        Miscellaneous.respond(event, "Created a new react-for-roles message with ID `" + messageId + "`.\n"
                + "To add role-to-reaction mappings, use the command: `" + CMD_REACTROLES + ' ' + REACTROLES_MAPROLE
                + ' ' + OP_ADD + " [roleName] " + messageId + " [emote]`");
    }

    private void handleMessageEditCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 2) {
            throw new IllegalArgumentException("Too few parameters");
        }

        String messageId = args[0];
        String newMessage = Miscellaneous.discardMessageParts(event.getMessage().getContentRaw(), commands.size() + 1)
                .trim();

        parent.updateReactForRolesMessage(event.getGuild(), messageId, newMessage);

        Miscellaneous.respond(event, "Successfully updated message with ID `" + messageId + "`.");
    }

    private void handleMessageRemoveCommand(GuildMessageReceivedEvent event, String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 1) {
            throw new IllegalArgumentException("Too few parameters");
        }
        String messageId = args[0];

        parent.deleteReactForRolesMessage(event.getGuild(), messageId, event.getMember());

        Miscellaneous.respond(event, "React-for-roles message with ID `" + messageId + "` was successfully"
                + " deleted.");
    }

    private void handleMaproleAddCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 2) {
            throw new IllegalArgumentException("Too few parameters");
        }

        String messageId = args[0];
        PotentialEmote potentialEmote = new PotentialEmote(args[1], event.getGuild().getEmoteCache());

        List<Role> roles = event.getGuild().getRolesByName(commands.get(3), true);

        if (roles.size() != 1) {
            // Should never happen
            throw new IllegalArgumentException("Could not resolve the role name `" + remainder + "`.");
        }

        String roleId = roles.get(0).getId();

        parent.addReactionToRoleMappingToMessage(event.getGuild(), messageId, potentialEmote, roleId);

        if (potentialEmote.getEmoteObject().isPresent()) {
            Miscellaneous.respond(event, "Successfully added reaction-to-role mapping! Emote "
                    + potentialEmote.getEmoteObject().get().getAsMention() + " will grant the " + roles.get(0).getName()
                    + " role.");
        } else {
            Miscellaneous.respond(event, "Successfully added reaction-to-role mapping! Emote "
                    + potentialEmote.getEmoteRaw() + " will grant the " + roles.get(0).getName() + " role.");
        }
    }

    private void handleMaproleRemoveCommand(GuildMessageReceivedEvent event, String remainder) {
        String[] args = remainder.split(" ");
        if (args.length < 2) {
            throw new IllegalArgumentException("Too few parameters");
        }

        String messageId = args[0];
        PotentialEmote potentialEmote = new PotentialEmote(args[1], event.getGuild().getEmoteCache());

        parent.removeReactionToRoleMappingFromMessage(event.getGuild(), messageId, potentialEmote, true);

        if (potentialEmote.getEmoteObject().isPresent()) {
            Miscellaneous.respond(event, "Successfully removed reaction-to-role mapping for emote "
                    + potentialEmote.getEmoteObject().get().getAsMention() + '.');
        } else {
            Miscellaneous.respond(event, "Successfully removed reaction-to-role mapping for emote "
                    + potentialEmote.getEmoteRaw() + '.');
        }
    }
}
