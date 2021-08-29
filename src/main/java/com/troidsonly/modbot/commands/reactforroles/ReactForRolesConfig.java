/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2021 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.reactforroles;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;

/**
 * This class contains the configuration for the react-for-roles functionality.<p>
 *
 * Instances of this class should not be constructed directly. Instead, please use the {@link #empty()} method to get
 * fresh instances. This is to allow JSON serialization to work correctly.
 */
class ReactForRolesConfig {

    private Map<String, ReactForRolesMessageConfig> messagesToConfigs;

    /**
     * Constructs a new fresh {@code ReactForRolesConfig} object.
     *
     * @return A fresh {@code ReactForRolesConfig} object
     */
    public static ReactForRolesConfig empty() {
        ReactForRolesConfig reactForRolesConfig = new ReactForRolesConfig();
        reactForRolesConfig.messagesToConfigs = new HashMap<>();
        return reactForRolesConfig;
    }

    /**
     * Gets the current configuration of all react-for-roles messages managed by this bot and their configurations. Note
     * that the returned {@link Map} is an {@linkplain ImmutableMap immutable} copy of the underlying map.
     *
     * @return A map of IDs of react-for-roles messages as strings (never null or empty) to their corresponding
     * configuration as a {@link ReactForRolesMessageConfig} object (also never null).
     */
    public Map<String, ReactForRolesMessageConfig> getMessagesToConfigs() {
        if (messagesToConfigs == null) {
            throw new IllegalStateException("messagesToConfigs is null. Verify that this ReactForRolesConfig "
                    + "object was created using the empty() method, or was deserialized by gson.");
        }

        return ImmutableMap.copyOf(messagesToConfigs);
    }

    /**
     * Constructs a new {@linkplain ReactForRolesMessageConfig#empty(String, String) empty} {@link
     * ReactForRolesMessageConfig} object, inserts it into the configuration map using the specified Discord message ID
     * as the key, and returns the new {@code ReactForRolesMessageConfig} object. Note that the ID of the text channel
     * the message was sent in must also be provided; this will be stored in the message's configuration.
     *
     * @param messageId The ID of the message as a String. May not be null or empty.
     * @param textChannelId The ID of the text channel containing the message as a String. May not be null or empty.
     * @param messageText The text of the message. May not be null or empty.
     * @return A new {@link ReactForRolesMessageConfig} object for the react-for-roles message
     */
    public ReactForRolesMessageConfig addAndGetNewReactForRolesMessage(String messageId, String textChannelId,
            String messageText) {
        if (StringUtils.isEmpty(messageId)) {
            throw new IllegalArgumentException("messageId may not be null or empty");
        }

        if (this.messagesToConfigs.containsKey(messageId)) {
            throw new IllegalStateException("A message with this ID is already defined in the messagesToConfigs map.");
        }

        ReactForRolesMessageConfig reactForRolesMessageConfig = ReactForRolesMessageConfig.empty(messageText,
                textChannelId);
        this.messagesToConfigs.put(messageId, reactForRolesMessageConfig);
        return reactForRolesMessageConfig;
    }

    /**
     * Removes the configuration for the react-for-roles message with the specified message ID. Note that it is the
     * caller's responsibility to actually delete the message from Discrod.
     *
     * @param messageId The ID of the message to remove the configuration for, as a string. May not be empty or null.
     */
    public void removeReactForRolesMessageById(String messageId) {
        if (StringUtils.isEmpty(messageId)) {
            throw new IllegalArgumentException("messageId may not be null or empty");
        }

        if (!messagesToConfigs.containsKey(messageId)) {
            throw new IllegalArgumentException("The specified message ID does not exist.");
        }

        messagesToConfigs.remove(messageId);
    }
}
