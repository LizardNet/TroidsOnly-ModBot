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

package com.troidsonly.modbot.commands.usermanagement;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.guild.GuildAvailableEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import com.troidsonly.modbot.commands.log.LogListener;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;

public class UserManagementListener extends ListenerAdapter {
    private final AccessControl acl;
    private final PersistenceManager<UserManagementConfig> pm;
    private final LogListener logListener;

    private final UserManagementConfig config;
    private final UserManagementHandler commandHandler;

    public UserManagementListener(AccessControl acl, PersistenceWrapper<?> wrapper, LogListener logListener) {
        this.acl = acl;
        pm = wrapper.getPersistenceManager("UserManagementConfig", UserManagementConfig.class);
        this.logListener = logListener;

        config = pm.get().orElseGet(UserManagementConfig::empty);
        commandHandler = new UserManagementHandler(this);
    }

    synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }

    public String getCryoRoleId() {
        return config.getCryoRoleId();
    }

    AccessControl getAcl() {
        return acl;
    }

    UserManagementConfig getConfig() {
        return config;
    }

    public UserManagementHandler getCommandHandler() {
        return commandHandler;
    }

    @Override
    public void onGuildAvailable(GuildAvailableEvent event) {
        // Get the list of currently banned and currently cryo'd users and compare to our maps.  If the current lists
        // don't contain entries in our maps, assume that the users in the maps were unbanned/uncryo'd externally and
        // remove them.  Alternatively, if entries appear in the lists that don't appear in the maps, ignore them; we
        // can assume they were set outside the bot and are thus indefinite and don't need to be tracked.

        List<String> bannedUsers = event.getGuild().getBans().complete().stream()
            .map(User::getId)
            .collect(Collectors.toList());

        List<String> cryodUsers = null;
        if (config.getCryoRoleId() != null) {
            Role cryoRole = event.getGuild().getRoleById(config.getCryoRoleId());
            cryodUsers = event.getGuild().getMembersWithRoles(cryoRole).stream()
                .map(Member::getUser)
                .map(User::getId)
                .collect(Collectors.toList());
        }

        List<String> regulars = null;
        if (config.getRegularRoleId() != null) {
            Role regularsRole = event.getGuild().getRoleById(config.getRegularRoleId());
            regulars = event.getGuild().getMembersWithRoles(regularsRole).stream()
                .map(Member::getUser)
                .map(User::getId)
                .collect(Collectors.toList());
        }

        synchronized (config) {
            config.getBanTracker().entrySet().removeIf(entry -> !bannedUsers.contains(entry.getKey()));

            if (cryodUsers != null) {
                Iterator<Map.Entry<String, Long>> iter = config.getCryoTracker().entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<String, Long> entry = iter.next();

                    if (!cryodUsers.contains(entry.getKey())) {
                        iter.remove();
                    }
                }
            }

            if (regulars != null) {
                Iterator<Map.Entry<String, Long>> iter = config.getRegularTracker().entrySet().iterator();

                while (iter.hasNext()) {
                    Map.Entry<String, Long> entry = iter.next();

                    User user = event.getJDA().getUserById(entry.getKey());

                    if ((user == null || !event.getGuild().isMember(user))
                        && config.getThreshholdInactiveDays() != null) {
                        Instant lastActive = Instant.ofEpochSecond(entry.getValue());

                        long daysSinceLastActive = ChronoUnit.DAYS.between(lastActive, Instant.now());

                        if (daysSinceLastActive >= config.getThreshholdInactiveDays()) {
                            iter.remove();
                        }
                    } else if (!regulars.contains(entry.getKey())) {
                        iter.remove();
                    }
                }
            }
        }
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if (config.getCryoRoleId() != null) {
            Role role = event.getGuild().getRoleById(config.getCryoRoleId());

            if (event.getRoles().contains(role)) {
                config.getCryoTracker().remove(event.getUser().getId());
            }
        }
    }
}
