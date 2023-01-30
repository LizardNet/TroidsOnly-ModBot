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

import org.apache.commons.lang3.StringUtils;

/**
 * Represents the state of a message that has been sent to the starboard. Messages sent to the starboard take the form
 * of an embed that contains a copy of the message at the moment it reached the promotion threshold. All values in this
 * class refer to the message with the embed in the starboard channel, not the message that was originally promoted.<p>
 *
 * Instances of this class should not be constructed directly. Instead, please use the
 * {@link #with(String, String, String)} method to get fresh instances. This is to allow JSON serialization to work
 * correctly.
 */
class StarboardMessageState {

    /**
     * The ID of the Starboard channel containing the promoted message as an embed
     */
    private String starboardChannelId;

    /**
     * The ID of the message containing the embed itself
     */
    private String starboardMessageId;

    /**
     * UNIX timestamp of when the message was sent to the starboard
     */
    private long promotedAt;

    /**
     * The emote ID (or raw value) of the starboard emote at the time the message was promoted, to allow for
     */
    private String emoteId;

    /**
     * Constructs a new {@code StarboardMessageState} object with the specified values.
     *
     * @param starboardChannelId The ID of the channel the starboard embed was sent to. May not be null or empty.
     * @param starboardMessageId The message ID of the starboard embed. May not be null or empty.
     * @param emoteId The ID (if emote) or raw emoji that was used at the time the message was promoted. May not be null
     * or empty. For the purpose of allowing the star count to be updated consistently even if the emoji is later
     * changed.
     * @return A fresh {@code StarboardMessageState} object.
     */
    public static StarboardMessageState with(String starboardChannelId, String starboardMessageId, String emoteId) {
        if (StringUtils.isEmpty(starboardChannelId)) {
            throw new IllegalArgumentException("starboardChannelId cannot be empty or null");
        }

        if (StringUtils.isEmpty(starboardMessageId)) {
            throw new IllegalArgumentException("starboardMessageId cannot be empty or null");
        }

        if (StringUtils.isEmpty(emoteId)) {
            throw new IllegalArgumentException("emoteId cannot be empty or null");
        }

        StarboardMessageState newState = new StarboardMessageState();
        newState.starboardChannelId = starboardChannelId;
        newState.starboardMessageId = starboardMessageId;
        newState.promotedAt = Instant.now().getEpochSecond();
        newState.emoteId = emoteId;
        return newState;
    }

    /**
     * The channel ID that the starboard embed was sent to, as a String.
     *
     * @return The starboard channel ID. Will not be empty or null.
     */
    public String getStarboardChannelId() {
        return starboardChannelId;
    }

    /**
     * The message ID of the actual embed that was sent to the starboard channel, as a String.
     *
     * @return The message ID of the starboard embed. Will not be empty or null.
     */
    public String getStarboardMessageId() {
        return starboardMessageId;
    }

    /**
     * The time that the message was sent to the starboard; i.e., the time the original message was promoted to the
     * starboard. Note that this is <em>not</em> the time that the original message was originally sent!
     *
     * @return The time that the message was sent to the starboard, as UNIX epoch seconds. Will not be null.
     */
    public long getPromotedAt() {
        return promotedAt;
    }

    /**
     * At the time that the message was promoted to the starboard, the emote/emoji that was used as the reaction to have
     * a message sent to the starboard. This is to allow the "star" count to be consistently updated even if the
     * reaction is changed later. Note that this may be an emoji or an ID representing an emote object; see
     * {@link com.troidsonly.modbot.utils.PotentialEmote PotentialEmote} for example semantics.
     *
     * @return The emoji or emote ID, as a string. Will not be empty or null.
     */
    public String getEmoteId() {
        return emoteId;
    }
}
