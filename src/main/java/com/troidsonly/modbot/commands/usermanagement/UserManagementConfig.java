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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

class UserManagementConfig {
    private String cryoRoleId;
    private String regularRoleId;
    private Long threshholdInactiveDays;

    // These two maps map the banned/cryo'd user's UID to the expiry time; should not contain null values but they may
    // be used to indicate non-expiring elements.
    private ConcurrentHashMap<String, Long> banTracker;
    private ConcurrentHashMap<String, Long> cryoTracker;

    // This map maps the regular user's UID to the last time they spoke; must not contain null values.
    private ConcurrentHashMap<String, Long> regularTracker;

    public static UserManagementConfig empty() {
        UserManagementConfig retval = new UserManagementConfig();
        retval.cryoRoleId = null;
        retval.banTracker = new ConcurrentHashMap<>();
        retval.cryoTracker = new ConcurrentHashMap<>();
        retval.regularTracker = new ConcurrentHashMap<>();
        return retval;
    }

    public String getCryoRoleId() {
        return cryoRoleId;
    }

    public void setCryoRoleId(String cryoRoleId) {
        this.cryoRoleId = Objects.requireNonNull(cryoRoleId);
    }

    public String getRegularRoleId() {
        return regularRoleId;
    }

    public void setRegularRoleId(String regularRoleId) {
        this.regularRoleId = Objects.requireNonNull(regularRoleId);
    }

    public Long getThreshholdInactiveDays() {
        return threshholdInactiveDays;
    }

    public void setThreshholdInactiveDays(Long threshholdInactiveDays) {
        this.threshholdInactiveDays = threshholdInactiveDays;
    }

    public ConcurrentHashMap<String, Long> getBanTracker() {
        if (banTracker == null) {
            banTracker = new ConcurrentHashMap<>();
        }

        return banTracker;
    }

    public ConcurrentHashMap<String, Long> getCryoTracker() {
        if (cryoTracker == null) {
            cryoTracker = new ConcurrentHashMap<>();
        }

        return cryoTracker;
    }

    public ConcurrentHashMap<String, Long> getRegularTracker() {
        if (regularTracker == null) {
            regularTracker = new ConcurrentHashMap<>();
        }

        return regularTracker;
    }
}
