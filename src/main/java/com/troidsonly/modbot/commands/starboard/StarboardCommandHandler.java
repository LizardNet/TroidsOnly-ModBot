/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2023 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.starboard;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.utils.Miscellaneous;

class StarboardCommandHandler implements CommandHandler {

    private static final String CMD_STARBOARD = "starboard";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_STARBOARD);

    private static final String STARBOARD_GLOBAL = "global";
    private static final String STARBOARD_CHANNEL = "channel";
    private static final String STARBOARD_EXCLUDE = "exclude";
    private static final Set<String> STARBOARD_SUB_COMMANDS = ImmutableSet.of(STARBOARD_GLOBAL, STARBOARD_CHANNEL,
            STARBOARD_EXCLUDE);

    // Sub-commands for STARBOARD_GLOBAL and STARBOARD_CHANNEL
    private static final String OP_GET = "get";
    private static final String OP_SET = "set";
    private static final String OP_DISABLE = "disable";
    private static final Set<String> PRIMARY_OPERATIONS = ImmutableSet.of(OP_GET, OP_SET, OP_DISABLE);

    // Sub-commands for STARBOARD_EXCLUDE
    private static final String OP_LIST = "list";
    private static final String OP_ADD = "add";
    private static final String OP_REMOVE = "remove";
    private static final Set<String> EXCLUDE_OPERATIONS = ImmutableSet.of(OP_LIST, OP_ADD, OP_REMOVE);

    private static final String PERM_STARBOARD_CONFIG = "starboard_config";

    private static final String HELP_USAGE = "Syntax:\n" +
            "For the server-wide (global) starboard:\n" +
            "- View settings: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_GET + "`\n" +
            "- Enable starboard or change settings: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_SET +
            " [#starboardChannel] [emote]`\n" +
            "- Disable starboard: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_DISABLE + "`\n\n" +
            "The Starboard can be enabled on a per-channel basis independent of the global setting. If the global " +
            "starboard is enabled, the per-channel setting (if enabled) overrides the global one. To set the per-" +
            "channel setting, use `" + CMD_STARBOARD + ' ' + STARBOARD_CHANNEL + " [#channelName]` instead of `" +
            CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + "` for the above commands.\n\n" +
            "If the global starboard is enabled, you can also specifically exclude channels from the starboard:\n" +
            "- List exclusions: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_LIST + "`\n" +
            "- Add exclusion: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_ADD + " [#channelName]`\n" +
            "- Remove exclusion: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_REMOVE + " [#channelName]`\n" +
            "Excluding a channel will also disable any per-channel starboard for that channel.\n\n" +
            "All changes do **not** apply retroactively (e.g., disabling a starboard does not remove its messages " +
            "from the starboard).\n\n" +
            "(Hint: If you're getting this error for a command that requires a channel name, try specifying the full " +
            "channel name, including the leading `#`, and make sure the bot has permissions to see the channel.)";

    private final StarboardListener parent;

    public StarboardCommandHandler(StarboardListener parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_STARBOARD)) {
            return STARBOARD_SUB_COMMANDS;
        }

        if (commands.size() == 2 && commands.get(0).equals(CMD_STARBOARD)) {
            switch (commands.get(1)) {
                case STARBOARD_GLOBAL:
                    return PRIMARY_OPERATIONS;
                case STARBOARD_CHANNEL:
                    return Miscellaneous.getAllTextChannels(event);
                case STARBOARD_EXCLUDE:
                    return EXCLUDE_OPERATIONS;
            }
        }

        if (commands.size() == 3 && commands.get(0).equals(CMD_STARBOARD)) {
            switch (commands.get(1)) {
                case STARBOARD_CHANNEL:
                    return PRIMARY_OPERATIONS;
                case STARBOARD_EXCLUDE:
                    return Miscellaneous.getAllTextChannels(event);
            }
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty() || !commands.get(0).equals(CMD_STARBOARD)) {
            return;
        }

        if (!parent.getAcl().hasPermission(event.getMember(), PERM_STARBOARD_CONFIG)) {
            Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
            return;
        }

        try {
            if (commands.size() <= 2) {
                sendErrorWithSyntaxHelp("Too few arguments", event);
                return;
            }

            if (commands.size() == 3) {
                switch (commands.get(1)) {
                    case STARBOARD_GLOBAL:
                        switch (commands.get(2)) {
                            case OP_GET:
                                Miscellaneous.respond(event, "Would get global starboard config here"); // TODO
                                return;
                            case OP_SET:
                                Miscellaneous.respond(event, "Would set global starboard config here"); // TODO
                                return;
                            case OP_DISABLE:
                                Miscellaneous.respond(event, "Would disable global starboard here"); // TODO
                                return;
                        }
                        break;
                    case STARBOARD_CHANNEL:
                        sendErrorWithSyntaxHelp("Too few arguments", event);
                        return;
                    case STARBOARD_EXCLUDE:
                        switch (commands.get(2)) {
                            case OP_LIST:
                                Miscellaneous.respond(event, "Would list exclusions here"); // TODO
                                return;
                            case OP_ADD:
                            case OP_REMOVE:
                                sendErrorWithSyntaxHelp("Too few arguments", event);
                                return;
                        }
                        break;
                }
            }

            if (commands.size() == 4) {
                if (commands.get(1).equals(STARBOARD_CHANNEL)) {
                    String channelName = commands.get(2);

                    switch (commands.get(3)) {
                        case OP_GET:
                            Miscellaneous.respond(event,
                                    "Would get per-channel starboard config here for channel " + channelName); // TODO
                            return;
                        case OP_SET:
                            Miscellaneous.respond(event,
                                    "Would set per-channel starboard config here for channel " + channelName); // TODO
                            return;
                        case OP_DISABLE:
                            Miscellaneous.respond(event,
                                    "Would disable per-channel starboard here for channel " + channelName); // TODO
                            return;
                    }
                } else if (commands.get(1).equals(STARBOARD_EXCLUDE)) {
                    String channelName = commands.get(3);

                    switch (commands.get(2)) {
                        case OP_LIST:
                            Miscellaneous.respond(event, "Would list exclusions here"); // TODO
                            return;
                        case OP_ADD:
                            Miscellaneous.respond(event, "Would add exclusion for " + channelName + " here"); // TODO
                            return;
                        case OP_REMOVE:
                            Miscellaneous.respond(event, "Would remove exclusion for " + channelName + " here"); // TODO
                            return;
                    }
                }
            }

            throw new RuntimeException("Undefined command behavior; this is a bug!");
        } catch (Exception e) {
            Miscellaneous.respond(event, "Failed to run the command: " + e);
            throw e;  // So it gets logged
        }
    }

    private static void sendErrorWithSyntaxHelp(String errorMessage, GuildMessageReceivedEvent event) {
        Objects.requireNonNull(errorMessage);

        String message = errorMessage + ". " + HELP_USAGE;
        Miscellaneous.respond(event, message);
    }
}
