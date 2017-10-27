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

package com.troidsonly.modbot.commands.filter;

import java.util.HashMap;
import java.util.Map;

enum FilterAction {
    LOG_ONLY("log-only"), // All actions imply this one
    WARN_USER("warn-only"),
    DELETE_MESSAGE_SILENT("delete-silent"),
    DELETE_MESSAGE_AND_WARN("delete-and-warn"),
    DELETE_MESSAGE_AND_KICK("delete-and-kick"), // Implies also informing the user what happened
    DELETE_MESSAGE_AND_CRYO("delete-and-cryo"), // Implies also informing the user what happened
    DELETE_MESSAGE_AND_BAN("delete-and-ban"); // Implies also informing the user what happened

    private static final Map<String, FilterAction> fromStringMap = new HashMap<>();

    private final String stringRepresentation;

    static {
        for (FilterAction value : FilterAction.values()) {
            fromStringMap.put(value.stringRepresentation, value);
        }
    }

    FilterAction(String stringRepresentation) {
        this.stringRepresentation = stringRepresentation;
    }

    @Override
    public String toString() {
        return stringRepresentation;
    }

    public static Map<String, FilterAction> getFromStringMap() {
        return new HashMap<>(fromStringMap);
    }
}
