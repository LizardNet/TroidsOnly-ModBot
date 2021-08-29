/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2021 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.log;

import java.util.HashMap;
import java.util.Map;

class LoggerConfig {
    // A quick explanation on how all this works
    // If logging is enabled (which can only be done if primaryLogChannelId is not null):
    // * First, check if the user who is the subject of the log entry (i.e., the user banned, the user whose message was
    //   deleted, the user who joined the server, etc.) is in the keyset of the userIdToLogChannelIdFilters map; if so,
    //   send the log to the channel specified as the mapped value; otherwise continue.
    // * Next, if applicable to the type of event, check if the channel involved is in the keyset of the
    //   channelIdToLogChannelidFilters map; if so, send the log to the channel specified as the mapped value.  If the
    //   channel is not in the key set or the event type being logged is not specific to any text channel, continue.
    // * Finally, log everything not caught above to the channel specified by the primaryLogChannelId.

    private boolean enabled;
    private String primaryLogChannelId;
    private Map<String, String> userIdToLogChannelIdFilters;
    private Map<String, String> channelIdToLogChannelIdFilters;

    public static LoggerConfig empty() {
        LoggerConfig retval = new LoggerConfig();
        retval.enabled = false;
        retval.primaryLogChannelId = null;
        retval.userIdToLogChannelIdFilters = new HashMap<>();
        retval.channelIdToLogChannelIdFilters = new HashMap<>();
        return retval;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled && primaryLogChannelId == null) {
            throw new IllegalStateException("Cannot enable logging when the primary log channel is not set.");
        }

        this.enabled = enabled;
    }

    public String getPrimaryLogChannelId() {
        return primaryLogChannelId;
    }

    public void setPrimaryLogChannelId(String primaryLogChannelId) {
        this.primaryLogChannelId = primaryLogChannelId;
    }

    public Map<String, String> getUserIdToLogChannelIdFilters() {
        return userIdToLogChannelIdFilters;
    }

    public Map<String, String> getChannelIdToLogChannelIdFilters() {
        return channelIdToLogChannelIdFilters;
    }
}
