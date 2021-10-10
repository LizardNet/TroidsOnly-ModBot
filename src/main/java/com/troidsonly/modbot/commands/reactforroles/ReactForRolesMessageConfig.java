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
import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import org.apache.commons.lang3.StringUtils;

/**
 * This class maintains the configuration for each defined react-for-roles message. This allows the bot to support
 * multiple react-for-roles messages in a server, each with their own mapping of emotes to the roles granted by each
 * emote.<p>
 *
 * Instances of this class should not be constructed directly. Instead, please use the {@link #empty(String, String)}
 * method to get fresh instances. This is to allow JSON serialization to work correctly.
 */
class ReactForRolesMessageConfig {

    private static final String INSTRUCTIONAL_TEXT = "\n\nReact to this message with these emotes to receive the "
            + "corresponding role. Your reaction will be removed once the bot processes your request. You can remove "
            + "the role from yourself by reacting a second time.\n";


    private String messageText;
    private String textChannelId;
    private Map<String, String> emoteIdToRoleId;

    /**
     * Constructs a new, empty configuration object.
     *
     * @param messageText The main text of the react-to-roles message. May not be empty or null.
     * @param textChannelId The ID of the text channel the message was sent in. May not be empty or null.
     * @return A new, fresh {@code ReactForRolesMessageConfig} object.
     */
    public static ReactForRolesMessageConfig empty(String messageText, String textChannelId) {
        if (StringUtils.isEmpty(textChannelId)) {
            throw new IllegalArgumentException("textChannelId may not be empty or null");
        }

        ReactForRolesMessageConfig reactForRolesMessageConfig = new ReactForRolesMessageConfig();
        reactForRolesMessageConfig.setMessageText(messageText);
        reactForRolesMessageConfig.textChannelId = textChannelId;
        reactForRolesMessageConfig.emoteIdToRoleId = new HashMap<>();
        return reactForRolesMessageConfig;
    }

    /**
     * The main text of the react-to-roles message. This allows the bot to ensure that the message is always consistent.
     * A legend of which emotes map to what roles will always be appended to the message, and the bot will always ensure
     * that it has reacted with those roles to the message.
     *
     * @return The main text of the react-to-roles message.
     */
    public String getMessageText() {
        return messageText;
    }

    /**
     * Sets the message text of the message. Note that this only updates our saved view of the message; it's the
     * caller's responsibility to actually edit the message in Discord.
     *
     * @param messageText The new text of the message. May not be null or the empty string.
     */
    public void setMessageText(String messageText) {
        if (StringUtils.isEmpty(messageText)) {
            throw new IllegalArgumentException("messageText may not be empty or null");
        }

        this.messageText = messageText;
    }

    /**
     * Gets the ID of the text channel the message this configuration describes was originally sent in.
     *
     * @return The ID of the text channel containing this react-for-roles message. Will never be null or the empty
     * string.
     */
    public String getTextChannelId() {
        return textChannelId;
    }

    /**
     * A map of emote IDs (as strings) to the ID of the role (as a string) to be granted with a user reacts with that
     * emote, and removed when the user removes their reaction. Note that the returned {@link Map} is an {@linkplain
     * ImmutableMap immutable} copy of the underlying map.
     *
     * @return A map of emote IDs to role IDs. Both are strings. Never returns null. Returned collection is immutable.
     */
    public Map<String, String> getEmoteIdToRoleId() {
        if (emoteIdToRoleId == null) {
            throw new IllegalStateException("emoteIdToRoleId is null. Verify that this ReactForRolesMessageConfig "
                    + "object was created using the empty() method, or was deserialized by gson.");
        }

        return ImmutableMap.copyOf(emoteIdToRoleId);
    }

    /**
     * Adds a new emote-to-role mapping to the configuration. Note that, to avoid consistency issues, this method does
     * not permit replacing an already existing emote-to-role mapping.
     *
     * @param emoteId The ID of the emote to add to the configuration. May not be empty string or null.
     * @param roleId The ID of the role this emote should control. May not be empty string or null.
     */
    public void addEmoteToRoleIdMapping(String emoteId, String roleId) {
        if (StringUtils.isEmpty(emoteId)) {
            throw new IllegalArgumentException("emoteId may not be null or empty");
        }

        if (StringUtils.isEmpty(roleId)) {
            throw new IllegalArgumentException("roleId may not be null or empty");
        }

        if (emoteIdToRoleId.containsKey(emoteId)) {
            throw new IllegalStateException(
                    "emote-to-role map already contains this emote ID. Replacing an existing key is not allowed.");
        }

        emoteIdToRoleId.put(emoteId, roleId);
    }

    /**
     * Removes an emote from the emote-to-role mapping of the configuration. It is the caller's responsibility to ensure
     * that this doesn't cause any state inconsistencies, such as by ensuring that the role is removed from all users
     * before removing it from the mapping.
     *
     * @param emoteId The ID of the emote to remove from the mapping, as a string. May not be empty or null.
     */
    public void removeEmoteToRoleIdMapping(String emoteId) {
        if (StringUtils.isEmpty(emoteId)) {
            throw new IllegalArgumentException("emoteId may not be null or empty");
        }

        if (!emoteIdToRoleId.containsKey(emoteId)) {
            throw new IllegalArgumentException("The specified emote has not been configured for this message.");
        }

        emoteIdToRoleId.remove(emoteId);
    }

    /**
     * Renders the full message to display on Discord, corresponding with this {@code ReactForRolesMessageConfig}
     * object.<p>
     *
     * The rendered message includes the full message text, as well as an explanation of what the message is and how it
     * works, as well as what emotes grant/revoke what roles.
     *
     * @param guild The {@link Guild} object representing the Discord server. This will be used to resolve role IDs to
     * role names and emote IDs to emotes.
     * @return The rendered message for display on Discord. Will never be empty or null.
     */
    public String renderMessage(Guild guild) {
        StringBuilder sb = new StringBuilder(getMessageText() + INSTRUCTIONAL_TEXT);

        for (Map.Entry<String, String> emoteIdToRoleIdEntry : emoteIdToRoleId.entrySet()) {
            Role role = guild.getRoleById(emoteIdToRoleIdEntry.getValue());
            Emote emote = null;
            String emoteId = emoteIdToRoleIdEntry.getKey();

            if (role == null) {
                continue;
            }

            try {
                long emoteIdLong = Long.parseLong(emoteId);
                emote = guild.getEmoteById(emoteIdLong);
            } catch (NumberFormatException e) {
                // It's not a snowflake ID, continue
            }

            if (emote == null) {
                sb.append(emoteId);
            } else {
                sb.append(emote.getAsMention());
            }

            sb.append(" ⇒ ")
                    .append(role.getName())
                    .append('\n');
        }

        return sb.toString().trim();
    }
}
