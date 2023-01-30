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

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

/**
 * Class representing configuration needed for the Starboard feature.<p>
 *
 * Instances of this class should not be constructed directly. Instead, please use the {@link #empty()} method to get
 * fresh instances. This is to allow JSON serialization to work correctly.
 */
class StarboardConfig {

    private static final int NINETY_DAYS_IN_SECONDS = 60 * 60 * 24 * 90;

    /**
     * This is a map containing all known messages that have been sent to a starboard. The outer map is keyed on the
     * <strong>source</strong> channel ID, with the inner map keyed on the <strong>source</strong> message ID. The
     * resulting {@code StarboardMessageState} object contains details about the message as it was promoted, for example
     * the channel ID of the starboard channel the message was promoted to.
     */
    private Map<String, Map<String, StarboardMessageState>> promotedMessageRecord;

    public static StarboardConfig empty() {
        StarboardConfig newConfig = new StarboardConfig();
        newConfig.promotedMessageRecord = new HashMap<>();
        return newConfig;
    }

    /**
     * Retrieves the {@link StarboardMessageState} object corresponding a promoted message that was sent in the given
     * channel and with the given message ID, or an empty {code Optional} if no matching message was found.<p>
     *
     * If this method returns an empty {@code Optional}, it likely means that the given message was never promoted in
     * the first place, or that it's old enough that it was purged from the local state.
     *
     * @param srcChannelId The channel ID, as a string, the original message was sent in
     * @param srcMessageId The ID, as a string, of the original message
     * @return The {@code StarboardMessageState} object for the given message, or an empty {@code Optional}.
     */
    public Optional<StarboardMessageState> getMessageStateFor(String srcChannelId, String srcMessageId) {
        if (promotedMessageRecord.containsKey(srcChannelId)) {
            Map<String, StarboardMessageState> msgsForChannel = promotedMessageRecord.get(srcChannelId);

            if (msgsForChannel.containsKey(srcMessageId)) {
                return Optional.of(msgsForChannel.get(srcMessageId));
            }
        }

        return Optional.empty();
    }

    /**
     * Adds a {@link StarboardMessageState} representing a message that has been promoted to a starboard channel. Note
     * that the first two parameters of this method refer to the message that was promoted, not the message containing
     * the embed that was itself sent to the starboard channel.
     *
     * @param srcChannelId The channel ID, as a string, the original message was sent in. May not be empty or null.
     * @param srcMessageId The ID, as a string, of the original message. May not be empty or null.
     * @param messageState The {@code StarboardMessageState} object. May not be null.
     */
    public void addMessageState(String srcChannelId, String srcMessageId, StarboardMessageState messageState) {
        if (StringUtils.isEmpty(srcChannelId)) {
            throw new IllegalArgumentException("srcChannelId may not be empty or null.");
        }

        if (StringUtils.isEmpty(srcMessageId)) {
            throw new IllegalArgumentException("srcMessageId may not be empty or null.");
        }

        Objects.requireNonNull(messageState);

        final Map<String, StarboardMessageState> msgsForChannel;

        if (promotedMessageRecord.containsKey(srcChannelId)) {
            msgsForChannel = promotedMessageRecord.get(srcChannelId);
        } else {
            msgsForChannel = new HashMap<>();
            promotedMessageRecord.put(srcMessageId, msgsForChannel);
        }

        msgsForChannel.put(srcMessageId, messageState);
    }

    /**
     * Deletes all messages older than 90 days, to save some space and reduce the overhead of messages we need to check
     * for updates.
     */
    public void pruneOldMessages() {
        for (Map<String, StarboardMessageState> stringStarboardMessageStateMap : promotedMessageRecord.values()) {
            stringStarboardMessageStateMap.values().removeIf(
                    messageState -> messageState.getPromotedAt() + NINETY_DAYS_IN_SECONDS < Instant.now()
                            .getEpochSecond());
        }
    }
}
