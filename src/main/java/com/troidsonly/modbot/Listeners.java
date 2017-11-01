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
 *
 * The code in this file is modified from the following class(es) in LizardIRC/Beancounter:
 *   org.lizardirc.beancounter.Listeners
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import net.dv8tion.jda.core.hooks.EventListener;

import com.troidsonly.modbot.commands.admin.AdminHandler;
import com.troidsonly.modbot.commands.filter.FilterListener;
import com.troidsonly.modbot.commands.log.LogListener;
import com.troidsonly.modbot.commands.tuuuuuuubes.BombAndTubesHandler;
import com.troidsonly.modbot.commands.usermanagement.UserManagementListener;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.hooks.CommandListener;
import com.troidsonly.modbot.hooks.Fantasy;
import com.troidsonly.modbot.hooks.MultiCommandHandler;
import com.troidsonly.modbot.persistence.GsonPersistenceWrapper;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.security.DiscordGuildRoleAccessControl;

class Listeners {
    private final Set<EventListener> ownListeners = new HashSet<>();

    private final Properties properties;
    private final ExecutorService executorService;

    public Listeners(Properties properties, ExecutorService executorService) {
        this.properties = properties;
        this.executorService = executorService;
    }

    public Set<EventListener> getAllListeners() {
        return ownListeners;
    }

    public void register() {
        String fantasyString = properties.getProperty("fantasyString", "?");
        String[] ownerUids = properties.getProperty("ownerUid", "").split(",");
        String statefileName = properties.getProperty("statefileName", "state.json");
        String tubesDirectory = properties.getProperty("tubesDirectory", "tubes");

        Path statefile = Paths.get(statefileName);
        Path tubes = Paths.get(tubesDirectory);

        PersistenceWrapper<?> wrapper = new GsonPersistenceWrapper(statefile);
        AccessControl acl = new DiscordGuildRoleAccessControl(wrapper, new HashSet<>(Arrays.asList(ownerUids)));
        LogListener logListener = new LogListener(wrapper, acl);
        UserManagementListener userManagementListener = new UserManagementListener(acl, wrapper);
        FilterListener filterListener = new FilterListener(acl, logListener, wrapper, userManagementListener, executorService, fantasyString);

        List<CommandHandler> handlers = new ArrayList<>();
        handlers.add(acl.getHandler());
        handlers.add(new AdminHandler(acl));
        handlers.add(new BombAndTubesHandler(wrapper, tubes, acl));
        handlers.add(logListener.getCommandHandler());
        handlers.add(userManagementListener.getCommandHandler());
        handlers.add(filterListener.getCommandHandler());

        MultiCommandHandler commands = new MultiCommandHandler(handlers);
        ownListeners.add(new Fantasy(new CommandListener(commands), fantasyString));
        ownListeners.add(logListener);
        ownListeners.add(filterListener);
        ownListeners.add(userManagementListener);
    }
}
