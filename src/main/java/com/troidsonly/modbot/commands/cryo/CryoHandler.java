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

package com.troidsonly.modbot.commands.cryo;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

public class CryoHandler implements CommandHandler {
    private static final String CMD_CFGCRYO = "cfgcryo";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_CFGCRYO);

    private static final String PERM_CFGCRYO = CMD_CFGCRYO;

    private final AccessControl acl;
    private final PersistenceManager<CryoConfig> pm;

    private final CryoConfig config;

    public CryoHandler(AccessControl acl, PersistenceWrapper<?> wrapper) {
        this.acl = acl;
        pm = wrapper.getPersistenceManager("CryoConfig", CryoConfig.class);

        config = pm.get().orElseGet(CryoConfig::empty);
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_CFGCRYO)) {
            return ImmutableSet.copyOf(getAllRoles(event));
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        switch (commands.get(0)) {
            case CMD_CFGCRYO:
                if (acl.hasPermission(event.getMember(), PERM_CFGCRYO)) {
                    if (commands.size() == 2) {
                        Role cryoRole = event.getGuild().getRolesByName(commands.get(1), true).get(0);

                        config.setCryoRoleId(cryoRole.getId());
                        sync();

                        Miscellaneous.respond(event, "Cryo role set to " + cryoRole.toString());
                    } else {
                        String cryoRole;

                        if (config.getCryoRoleId() == null) {
                            cryoRole = "(not set)";
                        } else {
                            cryoRole = event.getGuild().getRoleById(config.getCryoRoleId()).toString();
                        }

                        Miscellaneous.respond(event, "Sorry, I didn't recognize that role name\n" +
                            "Cryo role is currently set to: " + cryoRole + '\n' +
                            "Roles I recognize: " + Miscellaneous.getStringRepresentation(getAllRoles(event)) + '\n' +
                            "Syntax: `" + CMD_CFGCRYO + " [roleName]`");
                    }
                } else {
                    Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
                }
                break;
        }
    }

    private synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }

    public String getCryoRoleId() {
        return config.getCryoRoleId();
    }

    private Set<String> getAllRoles(GuildMessageReceivedEvent event) {
        return event.getGuild().getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toSet());
    }
}
