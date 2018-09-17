/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2018 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.roleme;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

class RoleMeConfig {

    private String roleMeChannelId;
    private String roleIdToGrant;
    private String welcomeChannelId;
    private List<String> welcomeMessages;
    private String autoSendMessage;

    public static RoleMeConfig ofEmpty() {
        RoleMeConfig emptyConfig = new RoleMeConfig();
        emptyConfig.roleMeChannelId = null;
        emptyConfig.roleIdToGrant = null;
        emptyConfig.welcomeChannelId = null;
        emptyConfig.welcomeMessages = Collections.emptyList();
        emptyConfig.autoSendMessage = null;
        return emptyConfig;
    }

    /**
     * @return The channel to accept the roleme command in.  If this is null, the entire module is disabled until set.
     */
    public String getRoleMeChannelId() {
        return roleMeChannelId;
    }

    /**
     * Sets the channel to accept the roleme command in.  The bot will require full permissions in the channel so it
     * can read messages and delete them after being processed.  This can be set to null; doing so has the effect of
     * disabling the roleme module until it is set to a non-null value again.  This must be a valid Discord Channel
     * ID.<p>
     *
     * Note that log entries related to the roleme module will appear to come from this channel for log filtering and
     * redirection purposes.
     *
     * @param roleMeChannelId Sets the roleme channel to the given Discord Channel ID, or null to disable
     */
    public void setRoleMeChannelId(String roleMeChannelId) {
        this.roleMeChannelId = roleMeChannelId;
    }

    /**
     * @return The role ID to be assigned to a validated user when they issue the roleme command, or null to disable the
     * module until set.
     */
    public String getRoleIdToGrant() {
        return roleIdToGrant;
    }

    /**
     * The role to be granted when the roleme command is given and the usage of the command is validated (e.g., nickname
     * or username doesn't violate any name filters, user is not previously muted, etc.).  This must be a valid Discord
     * Role ID on the given server, or it can be set to null to disable the roleme module until it is set again.
     *
     * @param roleIdToGrant The role to be granted when the roleme command is called as a valid Discord Role ID, or null
     * to disable the module.
     */
    public void setRoleIdToGrant(String roleIdToGrant) {
        this.roleIdToGrant = roleIdToGrant;
    }

    /**
     * @return The channel a welcome message should be sent to welcoming a new user to the server, after they have used
     * the roleme command, or null to indicate that the autowelcoime feature is disabled.
     */
    public String getWelcomeChannelId() {
        return welcomeChannelId;
    }

    /**
     * Sets the channel that a welcome message will be sent to to welcome a user to the server after they (successfully)
     * use the roleme command.  This must be a valid Discord Channel ID on the server the bot is on, or this can be set
     * to null to disable this autowelcome feature.  This is intended to be a "spammer-safe" reimplementation of
     * Discord's automatic public welcome messages.;
     *
     * @param welcomeChannelId The Discord Channel ID of the channel to send welcome messages to, or null to disable this
     * feature
     */
    public void setWelcomeChannelId(String welcomeChannelId) {
        this.welcomeChannelId = welcomeChannelId;
    }

    /**
     * @return A list of strings representing welcome messages to be sent to the welcome channel when a user "roles-up".
     * These are format strings where the first parameter ({@code %1$s}) is a mention of the user being welcomed.  If
     * the set is empty, disable the feature; if the list contains more than one string, select one at random.  This
     * method may not return null.
     */
    public List<String> getWelcomeMessages() {
        return welcomeMessages;
    }

    /**
     * Sets the strings that define welcome messages to be sent to the welcome channel when a user "roles-up".  These
     * are format strings where the first parameter ({@code %1$s)} is a mention of the user being welcomed.  If this is
     * set to the empty list, disable the feature; if the list contains more than one string, one will be selected at
     * random.  This may not be set to null.
     *
     * @param welcomeMessages The list of strings representing welcome messages.  May not be null.
     */
    public void setWelcomeMessages(List<String> welcomeMessages) {
        this.welcomeMessages = Objects.requireNonNull(welcomeMessages);
    }

    /**
     * @return The message to be DM'd to new users who were not previously muted (and for whom they will be re-muted)
     * and for whom a roleme command would be accepted from.  If this is null, no message is sent to new users when
     * they join the server.
     */
    public String getAutoSendMessage() {
        return autoSendMessage;
    }

    /**
     * Sets the message to be DM'd to new users when they join the server.  The message is only sent to users who are
     * not known to the bot to have been previously muted (and who will be automatically re-muted) and who don't have
     * user/nicknames that trip the name filter; i.e., this message is only sent to users from which a roleme command
     * would be accepted by the bot.  This can also be set to null to disable the feature.
     *
     * @param autoSendMessage The message to be sent to new users when they join the server, or null to disable the
     * feature
     */
    public void setAutoSendMessage(String autoSendMessage) {
        this.autoSendMessage = autoSendMessage;
    }
}
