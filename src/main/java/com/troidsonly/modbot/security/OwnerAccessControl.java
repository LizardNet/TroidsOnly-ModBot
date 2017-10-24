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

package com.troidsonly.modbot.security;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;

/**
 * Basic AccessControl scheme that grants the defined owners of the bot full control.
 */
public class OwnerAccessControl implements AccessControl {
    private final Set<String> owners;
    private final CommandHandler commandHandler = new OwnerAccessControlCommandHandler();

    public OwnerAccessControl(Set<String> owners) {
        this.owners = owners;
    }

    @Override
    public boolean hasPermission(Member member, String permission) {
        return owners.contains(member.getUser().getId());
    }

    @Override
    public Set<String> getPermissions(Member member) {
        if (hasPermission(member, null)) {
            return ImmutableSet.of("*");
        } else {
            return Collections.emptySet();
        }
    }

    @Override
    public CommandHandler getHandler() {
        return commandHandler;
    }

    public static class OwnerAccessControlCommandHandler implements CommandHandler {
        @Override
        public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
            return Collections.emptySet();
        }

        @Override
        public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
            // Goes nowhere, does nothing.
        }
    }
}
