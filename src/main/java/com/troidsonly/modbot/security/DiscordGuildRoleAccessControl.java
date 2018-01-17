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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.utils.Miscellaneous;

public class DiscordGuildRoleAccessControl implements AccessControl {
    private final DGRACHandler commandHandler = new DGRACHandler();
    private final Set<String> owners;
    private final PersistenceManager<Persistence> pm;
    private final Persistence persistence;

    public DiscordGuildRoleAccessControl(PersistenceWrapper<?> wrapper, Set<String> owners) {
        pm = wrapper.getPersistenceManager("DiscordGuildRoleAccessControl", Persistence.class);
        this.owners = owners;
        persistence = pm.get().orElseGet(Persistence::empty);
    }

    @Override
    public boolean hasPermission(Member member, String permission) {
        return getPermissions(member).contains("*") || getPermissions(member).contains(permission);
    }

    @Override
    public Set<String> getPermissions(Member member) {
        if (owners.contains(member.getUser().getId())) {
            return ImmutableSet.of("*");
        }

        return member.getRoles().stream()
            .map(Role::getId)
            .filter(role -> persistence.getRoleIdToPermissions().containsKey(role))
            .map(role -> persistence.getRoleIdToPermissions().get(role))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());
    }

    @Override
    public CommandHandler getHandler() {
        return commandHandler;
    }

    private synchronized void sync() {
        pm.persist(persistence);
        pm.sync();
    }

    private static class Persistence {
        private Map<String, Set<String>> roleIdToPermissions;

        public static Persistence empty() {
            Persistence retval = new Persistence();
            retval.roleIdToPermissions = new HashMap<>();
            return retval;
        }

        public Map<String, Set<String>> getRoleIdToPermissions() {
            return roleIdToPermissions;
        }
    }

    private class DGRACHandler implements CommandHandler {
        private static final String CMD_ACL = "acl";
        private final Set<String> COMMANDS = ImmutableSet.of(CMD_ACL);

        private static final String SCMD_GRANT = "grant";
        private static final String SCMD_REVOKE = "revoke";
        private static final String SCMD_LIST = "list";
        private Set<String> SUBCOMMANDS = ImmutableSet.of(SCMD_GRANT, SCMD_REVOKE, SCMD_LIST);

        private static final String PERM_ACL = "acl";

        @Override
        public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
            if (commands.isEmpty()) {
                return COMMANDS;
            }

            if (commands.get(0).equals(CMD_ACL)) {
                if (commands.size() == 2) {
                    return getAllRoles(event);
                } else if (commands.size() == 1){
                    return SUBCOMMANDS;
                }
            }

            return Collections.emptySet();
        }

        @Override
        public synchronized void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
            if (commands.isEmpty()) {
                return;
            }

            remainder = remainder.trim();

            if (hasPermission(event.getMember(), PERM_ACL)) {
                if (commands.size() >= 2) {
                    switch (commands.get(1)) {
                        case SCMD_GRANT:
                            if (commands.size() < 3) {
                                Miscellaneous.respond(event, "Sorry, I didn't recognize that role name.\n" +
                                    "Recognized roles: " + Miscellaneous.getStringRepresentation(getAllRoles(event)) + '\n' +
                                    "Syntax: acl grant [roleName] [permission]");
                            } else {
                                String roleName = commands.get(2);
                                String roleId = event.getGuild().getRolesByName(roleName, true).get(0).getId();

                                Set<String> permissions = persistence.getRoleIdToPermissions().computeIfAbsent(roleId, k -> new HashSet<>());

                                permissions.add(remainder);
                                sync();

                                Miscellaneous.respond(event, "Granted permission \"" + remainder + "\" to role \"" + roleName + '"');
                            }
                            break;
                        case SCMD_REVOKE:
                            if (commands.size() < 3) {
                                Miscellaneous.respond(event, "Sorry, I didn't recognize that role name.\n" +
                                    "Recognized roles: " + Miscellaneous.getStringRepresentation(getAllRoles(event)) + '\n' +
                                    "Syntax: acl revoke [roleName] [permission]");
                            } else {
                                String roleName = commands.get(2);
                                String roleId = event.getGuild().getRolesByName(roleName, true).get(0).getId();

                                Set<String> permissions = persistence.getRoleIdToPermissions().get(roleId);

                                if (permissions == null) {
                                    Miscellaneous.respond(event, "Error: Cannot revoke permission \"" + remainder + "\" from role \"" + roleName +
                                        "\" - this permission wasn't assigned in the first place.");
                                    return;
                                }

                                if (permissions.remove(remainder)) {
                                    sync();
                                    Miscellaneous.respond(event, "Revoked permission " + remainder.trim() + " from role " + roleName);
                                } else {
                                    Miscellaneous.respond(event, "Error: Cannot revoke permission \"" + remainder + "\" from role \"" + roleName +
                                        "\" - this permission wasn't assigned in the first place.");
                                }
                            }
                            break;
                        case SCMD_LIST:
                            if (commands.size() < 3) {
                                try {
                                    StringBuilder sb = new StringBuilder();

                                    for (Map.Entry<String, Set<String>> entry : persistence.getRoleIdToPermissions().entrySet()) {
                                        sb.append("Role \"")
                                            .append(event.getGuild().getRoleById(entry.getKey()))
                                            .append("\" has permissions: ")
                                            .append(Miscellaneous.getStringRepresentation(entry.getValue()))
                                            .append('\n');
                                    }

                                    if (sb.length() > 0) {
                                        Miscellaneous.respond(event, "PMing you all roles I have permissions assigned to.");
                                        event.getMember().getUser().openPrivateChannel().complete(false).sendMessage(sb.toString()).complete(false);
                                    } else {
                                        Miscellaneous.respond(event, "No permissions have yet been assigned.");
                                    }
                                } catch (Exception e) {
                                    Miscellaneous.respond(event, "Failed to PM you: " + e.toString());
                                    e.printStackTrace(System.err);
                                    return;
                                }
                            } else {
                                String roleName = commands.get(2);

                                try {
                                    String roleId = event.getGuild().getRolesByName(roleName, true).get(0).getId();
                                    StringBuilder sb = new StringBuilder();
                                    Set<String> permissions = persistence.getRoleIdToPermissions().get(roleId);

                                    if (permissions == null) {
                                        permissions = Collections.emptySet();
                                    }

                                    sb.append("Role \"")
                                        .append(roleName)
                                        .append("\" has permissions: ")
                                        .append(Miscellaneous.getStringRepresentation(permissions))
                                        .append('\n');

                                    Miscellaneous.respond(event, sb.toString());
                                } catch (Exception e) {
                                    Miscellaneous.respond(event, "Failed to get permissions information: " + e.toString());
                                    e.printStackTrace(System.err);
                                    return;
                                }
                            }
                            break;
                        default:
                            Miscellaneous.respond(event, "Syntax error.  Usage:\n```\n" +
                                "acl <grant|revoke> [roleName] [permission]\n" +
                                "acl list {[roleName]}\n```");
                            break;
                    }
                } else {
                    Miscellaneous.respond(event, "Syntax error.  Usage:\n```\n" +
                        "acl <grant|revoke> [roleName] [permission]\n" +
                        "acl list {[roleName]}\n```");
                }
            } else {
                Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
            }
        }

        private Set<String> getAllRoles(GuildMessageReceivedEvent event) {
            return event.getGuild().getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
        }
    }
}
