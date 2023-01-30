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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;

/**
 * Class representing state needed for the Starboard feature.<p>
 *
 * Instances of this class should not be constructed directly. Instead, please use the {@link #empty()} method to get
 * fresh instances. This is to allow JSON serialization to work correctly.
 */
class StarboardState {

    public static final int NINETY_DAYS_IN_SECONDS = 60 * 60 * 24 * 90;

    /**
     * This is a map containing all known messages that have been sent to a starboard. The outer map is keyed on the
     * <strong>source</strong> channel ID, with the inner map keyed on the <strong>source</strong> message ID. The
     * resulting {@code StarboardMessageState} object contains details about the message as it was promoted, for example
     * the channel ID of the starboard channel the message was promoted to.
     */
    private Map<String, Map<String, StarboardMessageState>> promotedMessageRecord;

    private StarboardConfig globalConfig;
    private Map<String, StarboardConfig> perChannelConfig;

    private Set<String> excludedChannelIds;

    public static StarboardState empty() {
        StarboardState newState = new StarboardState();
        newState.promotedMessageRecord = new HashMap<>();
        newState.globalConfig = StarboardConfig.of(false, null, null, null);
        newState.perChannelConfig = new HashMap<>();
        newState.excludedChannelIds = new HashSet<>();
        return newState;
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

    /**
     * Gets the Starboard configuration for the specified channel, prioritizing per-channel configuration and excludes,
     * if present, over the global configuration.
     *
     * @param channelId The Discord channel ID of the channel to get the configuration for. May not be empty or null.
     * @return A {@link StarboardConfig} object describing the Starboard configuration. Will never be null.
     */
    public StarboardConfig getConfigForChannel(String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            throw new IllegalArgumentException("channelId may not be empty or null.");
        }

        if (excludedChannelIds.contains(channelId)) {
            return StarboardConfig.of(false, null, null, null);
        }

        if (perChannelConfig.containsKey(channelId)) {
            return perChannelConfig.get(channelId);
        }

        return globalConfig;
    }

    /**
     * Explicitly get the global Starboard config (and only the global Starboard config). Useful for printing config,
     * but shouldn't be used for normal operation since this won't apply channel overrides. Use
     * {@link #getConfigForChannel(String)} for that.
     *
     * @return The global Starboard configuration. Will never be null.
     */
    public StarboardConfig getGlobalConfig() {
        return globalConfig;
    }

    /**
     * Set the global Starboard configuration to the configuration represented by the specified {@link StarboardConfig}
     * object.
     *
     * @param globalConfig The {@code StarboardConfig} object. May not be null.
     */
    public void setGlobalConfig(StarboardConfig globalConfig) {
        Objects.requireNonNull(globalConfig);

        this.globalConfig = globalConfig;
    }

    /**
     * Check if the specified Discord channel has a Starboard override configuration set (i.e., a per-channel
     * configuration). A value of {@code false} implies that the channel will use the global configuration.
     *
     * @param channelId The Discord channel ID of the channel to check. May not be empty or null.
     * @return {@code true} if the channel has an override Starboard configuration set. {@code false} otherwise.
     */
    public boolean channelHasOverride(String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            throw new IllegalArgumentException("channelId may not be empty or null.");
        }

        return perChannelConfig.containsKey(channelId);
    }

    /**
     * Set the per-channel Starboard configuration for the given channel to the configuration represented by the
     * specified {@link StarboardConfig} object. Remember that disabling the per-channel Starboard does not disable the
     * Starboard for that channel entirely; rather, it deletes the per-channel specific configuration and causes it to
     * fall back to the global configuration instead. To exclude a channel from the Starboard entirely (or remove such
     * an exclusion, use
     *
     * @param channelId The Discord channel ID. May not be empty or null.
     * @param starboardConfig The {@code StarboardConfig} object. May not be null.
     */
    public void setPerChannelConfig(String channelId, StarboardConfig starboardConfig) {
        if (StringUtils.isEmpty(channelId)) {
            throw new IllegalArgumentException("channelId may not be empty or null.");
        }

        Objects.requireNonNull(starboardConfig);

        if (!starboardConfig.isEnabled()) {
            perChannelConfig.remove(channelId);
        }

        perChannelConfig.put(channelId, starboardConfig);
    }

    /**
     * Adds the specified channel to the exclude list, but raises an {@code IllegalArgumentException} if the channel is
     * already excluded.
     *
     * @param channelId The Discord channel ID of the channel to be added to the Starboard exclusion list. May not be
     * empty or null.
     */
    public void addChannelExclusion(String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            throw new IllegalArgumentException("channelId may not be empty or null.");
        }

        if (excludedChannelIds.contains(channelId)) {
            throw new IllegalArgumentException("The specified channel (ID `" + channelId + "`) is already excluded.");
        }

        excludedChannelIds.add(channelId);
    }

    /**
     * Removes the specified channel from the exclude list, but raises an {@code IllegalArgumentException} if the
     * channel is not excluded to begin with.
     *
     * @param channelId The Discord channel ID of the channel to be removed from the Starboard exclusion list. May not
     * be empty or null.
     */
    public void removeChannelExclusion(String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            throw new IllegalArgumentException("channelId may not be empty or null.");
        }

        if (!excludedChannelIds.contains(channelId)) {
            throw new IllegalArgumentException(
                    "The specified channel (ID `" + channelId + "`) is not excluded to begin with.");
        }

        excludedChannelIds.remove(channelId);
    }

    /**
     * Checks if the specified channel is in the set of excluded channels. Useful for explicitly checking if a channel
     * is excluded, but is taken into consideration automatically when using {@link #getConfigForChannel(String)}.
     *
     * @param channelId The Discord channel ID of the channel to check
     * @return Whether the channel is excluded
     */
    public boolean isChannelExcluded(String channelId) {
        if (StringUtils.isEmpty(channelId)) {
            throw new IllegalArgumentException("channelId may not be empty or null.");
        }

        return excludedChannelIds.contains(channelId);
    }

    /**
     * Get the full set of channel IDs that have been excluded from the Starboard.
     *
     * @return The set of Discord text channel IDs that have been excluded
     */
    public Set<String> getExcludedChannelIds() {
        return ImmutableSet.copyOf(excludedChannelIds);
    }
}
