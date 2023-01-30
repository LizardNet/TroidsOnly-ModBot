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

import java.util.Objects;

import net.dv8tion.jda.api.hooks.ListenerAdapter;

import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;

public class StarboardListener extends ListenerAdapter {

    private final PersistenceManager<StarboardConfig> pm;
    private final AccessControl acl;
    private final StarboardCommandHandler commandHandler;

    private final StarboardConfig config;

    public StarboardListener(PersistenceWrapper<?> wrapper, AccessControl acl) {
        this.acl = Objects.requireNonNull(acl);

        this.commandHandler = new StarboardCommandHandler(this);
        pm = wrapper.getPersistenceManager("StarboardListener", StarboardConfig.class);
        config = pm.get().orElseGet(StarboardConfig::empty);
    }

    public StarboardCommandHandler getCommandHandler() {
        return commandHandler;
    }

    StarboardConfig getConfig() {
        return config;
    }

    AccessControl getAcl() {
        return acl;
    }

    synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }
}
