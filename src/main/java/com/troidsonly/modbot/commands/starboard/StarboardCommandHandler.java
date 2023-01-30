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
    private static final String OP_SET = "set";
    private static final String OP_DISABLE = "disable";
    private static final Set<String> PRIMARY_OPERATIONS = ImmutableSet.of(OP_SET, OP_DISABLE);

    // Sub-commands for STARBOARD_EXCLUDE
    private static final String OP_ADD = "add";
    private static final String OP_REMOVE = "remove";
    private static final Set<String> EXCLUDE_OPERATIONS = ImmutableSet.of(OP_ADD, OP_REMOVE);

    private static final String HELP_USAGE = "Syntax:\n" +
            "For the server-wide (global) starboard:\n" +
            "- View settings: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + "`\n" +
            "- Enable starboard or change settings: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_SET +
            " [#starboardChannel] [emote]`\n" +
            "- Disable starboard: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_DISABLE + "`\n\n" +
            "The Starboard can be enabled on a per-channel basis independent of the global setting. If the global " +
            "starboard is enabled, the per-channel setting (if enabled) overrides the global one. To set the per-" +
            "channel setting, use `" + CMD_STARBOARD + ' ' + STARBOARD_CHANNEL + " [#channelName]` instead of `" +
            CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + "` for the above commands.\n\n" +
            "If the global starboard is enabled, you can also specifically exclude channels from the starboard:\n" +
            "- List exclusions: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + "`\n" +
            "- Add exclusion: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_ADD + " [#channelName]`\n" +
            "- Remove exclusion: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_REMOVE + " [#channelName]`\n" +
            "Excluding a channel will also disable any per-channel starboard for that channel.\n\n" +
            "All changes do **not** apply retroactively (e.g., disabling a starboard does not remove its messages " +
            "from the starboard).";

    private final StarboardListener parent;

    public StarboardCommandHandler(StarboardListener parent) {
        this.parent = Objects.requireNonNull(parent);
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
        if (commands.isEmpty() || !commands.get(0).equals(CMD_STARBOARD)) {
            return;
        }

        Miscellaneous.respond(event, HELP_USAGE);
    }

    private static void sendErrorWithSyntaxHelp(String errorMessage, GuildMessageReceivedEvent event) {
        Objects.requireNonNull(errorMessage);

        String message = errorMessage + ". " + HELP_USAGE;
        Miscellaneous.respond(event, message);
    }
}
