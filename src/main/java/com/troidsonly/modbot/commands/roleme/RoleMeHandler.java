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

import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.commands.log.LogListener;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;

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
    private static final Set<String> CFG_OPS = ImmutableSet.of(CFG_OP_ADD, CFG_OP_REMOVE, CFG_OP_LIST, CFG_OP_SET);

    private final RoleMeListener parent;

    public RoleMeHandler(RoleMeListener parent) {
        this.parent = parent;
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        return null;
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {

    }
}