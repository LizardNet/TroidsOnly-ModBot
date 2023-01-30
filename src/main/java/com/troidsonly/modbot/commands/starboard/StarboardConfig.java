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
import java.util.Optional;
import java.util.OptionalInt;

import org.apache.commons.lang3.StringUtils;

/**
 * This class represents a configuration unit for the Starboard feature. Note that these objects are not scoped, and
 * scope will need to be tracked externally (e.g., whether this configuration applies to the entire server, or just one
 * channel).<p>
 *
 * Instances of this class should not be constructed directly. Instead, please use the
 * {@link #of(boolean, String, String, Integer)} method to get fresh instances. This is to allow JSON serialization to work
 * correctly.<p>
 *
 * Instances of this class are immutable. To change a configuration, a new instance must be constructed using
 * {@link #of(boolean, String, String, Integer)}.
 */
class StarboardConfig {

    private boolean enabled;
    private String targetStarboardChannelId;
    private String starboardEmoteId;
    private Integer promotionThreshold;

    /**
     * Constructs new {@code StarboardConfig} objects initialized to the specified values.
     *
     * @param enabled Whether the starboard is enabled. Boolean, may not be null.
     * @param targetStarboardChannelId The ID of the Discord channel that starred messages should be embedded to, as a
     * String. May only be null or empty if {@code enabled} is {@code false}.
     * @param starboardEmoteId he ID (if emote) or raw emoji used to star messages. May only be null or empty if
     * {@code enabled} is {@code false}.
     * @param promotionThreshold The number of reactions required for a message to be promoted to the Starboard. May
     * only be null if {@code enabled} is {@code false}. If not null, must be an integer greater than or equal to 1.
     * @return A new {@code StarboardConfig} object capturing the specified configuration.
     */
    public static StarboardConfig of(boolean enabled, String targetStarboardChannelId, String starboardEmoteId,
            Integer promotionThreshold) {
        if (enabled) {
            if (StringUtils.isEmpty(targetStarboardChannelId)) {
                throw new IllegalArgumentException(
                        "If enabled is true, targetStarboardChannelId may not be empty or null.");
            }

            if (StringUtils.isEmpty(starboardEmoteId)) {
                throw new IllegalArgumentException("If enabled is true, starboardEmoteId may not be empty or null.");
            }

            Objects.requireNonNull(promotionThreshold);
        }

        if (promotionThreshold != null && promotionThreshold < 1) {
            throw new IllegalArgumentException("If non-null, promotionThreshold must be >= 1");
        }

        StarboardConfig newConfig = new StarboardConfig();
        newConfig.enabled = enabled;
        newConfig.targetStarboardChannelId = targetStarboardChannelId;
        newConfig.starboardEmoteId = starboardEmoteId;
        newConfig.promotionThreshold = promotionThreshold;
        return newConfig;
    }

    /**
     * Check if the Starboard functionality is enabled
     *
     * @return Whether the starboard is enabled. May not be null.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Gets the Discord channel ID of the text channel Starboard messages should be promoted (embedded) to. If the
     * Starboard {@linkplain #isEnabled() is enabled}, this will never return an empty {@code Optional} or an empty
     * string.
     *
     * @return The channel ID of the target Starboard channel. Will not be an empty Optional or an empty string if the
     * Starboard functionality is enabled.
     */
    public Optional<String> getTargetStarboardChannelId() {
        return Optional.ofNullable(targetStarboardChannelId);
    }

    /**
     * Gets the emote ID or emoji used to track messages for promotion to the Starboard. If the Starboard
     * {@linkplain #isEnabled() is enabled}, this will never return an empty {@code Optional} or an empty string.
     *
     * @return The emote ID or raw emoji character. Will not be an empty Optional or an empty string if the Starboard
     * functionality is enabled.
     */
    public Optional<String> getStarboardEmoteId() {
        return Optional.ofNullable(starboardEmoteId);
    }

    /**
     * Gets the promotion threshold for the Starboard—i.e., the number of reactions needed for a message to be promoted
     * to the Starboard.
     *
     * @return The promotion threshold for the Starboard as an integer. Will not be an empty Optional if the Starboard
     * functionality is enabled. Will always be greater than or equal to 1.
     */
    public OptionalInt getPromotionThreshold() {
        if (promotionThreshold == null) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(promotionThreshold);
    }
}
