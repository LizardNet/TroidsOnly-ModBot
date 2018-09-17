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

package com.troidsonly.modbot.commands.mute;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

class MuteConfig {

    // Old field name preserved for compatibility; methods renamed
    private String cryoRoleId;
    private Map<String, Mute> mutedUserTracker;

    public static MuteConfig empty() {
        MuteConfig retval = new MuteConfig();
        retval.cryoRoleId = null;
        retval.mutedUserTracker = new HashMap<>();
        return retval;
    }

    public String getMuteRoleId() {
        return cryoRoleId;
    }

    public void setMuteRoleId(String muteRoleId) {
        cryoRoleId = Objects.requireNonNull(muteRoleId);
    }

    public Map<String, Mute> getMutedUserTracker() {
        return mutedUserTracker;
    }

    public void setMutedUserTracker(Map<String, Mute> mutedUserTracker) {
        this.mutedUserTracker = Objects.requireNonNull(mutedUserTracker);
    }

    public static class Mute {

        private Long expiryTime;
        private Long creationTime;
        private String creatorUid;
        private String comment;

        public Long getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(Long expiryTime) {
            this.expiryTime = expiryTime;
        }

        public long getCreationTime() {
            return creationTime;
        }

        public void setCreationTime(Long creationTime) {
            this.creationTime = Objects.requireNonNull(creationTime);
        }

        public String getCreatorUid() {
            return creatorUid;
        }

        public void setCreatorUid(String creatorUid) {
            this.creatorUid = creatorUid;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }

        public boolean hasExpired() {
            if (expiryTime == null) {
                return false;
            }

            long currentTime = Instant.now().getEpochSecond();
            return currentTime > expiryTime;
        }
    }
}
