/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2021-2023 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;
import com.troidsonly.modbot.utils.PotentialEmote;

public class ReactForRolesListener extends ListenerAdapter {

    private final PersistenceManager<ReactForRolesConfig> pm;
    private final AccessControl acl;
    private final ReactForRolesHandler commandHandler;

    private final ReactForRolesConfig config;

    public ReactForRolesListener(PersistenceWrapper<?> wrapper, AccessControl acl) {
        this.acl = Objects.requireNonNull(acl);

        this.commandHandler = new ReactForRolesHandler(this);
        pm = wrapper.getPersistenceManager("ReactForRolesListener", ReactForRolesConfig.class);
        config = pm.get().orElseGet(ReactForRolesConfig::empty);
    }

    public ReactForRolesHandler getCommandHandler() {
        return commandHandler;
    }

    ReactForRolesConfig getConfig() {
        return config;
    }

    AccessControl getAcl() {
        return acl;
    }

    synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        getConfig().getMessagesToConfigs()
                .forEach((key, value) -> alignState(event.getGuild(), key, value));
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        if (!getConfig().getMessagesToConfigs().containsKey(event.getMessageId())) {
            return;
        }

        if (event.getJDA().getSelfUser().getIdLong() == event.getMember().getIdLong()) {
            return;
        }

        String messageId = event.getMessageId();
        Member member = event.getMember();
        PotentialEmote emote = new PotentialEmote(event.getReactionEmote());

        String roleId = getMessageConfigForId(messageId).getEmoteIdToRoleId()
                .get(emote.getEmoteRaw());

        if (roleId != null) {
            toggleRoleForMember(event.getGuild(), member, roleId);
        }

        event.getReaction().removeReaction(event.getUser()).complete();
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent event) {
        if (!getConfig().getMessagesToConfigs().containsKey(event.getMessageId())) {
            return;
        }

        PotentialEmote emote = new PotentialEmote(event.getReactionEmote());
        handleReactionTotalRemoval(event, emote);
    }

    @Override
    public void onGuildMessageReactionRemoveAll(GuildMessageReactionRemoveAllEvent event) {
        if (!getConfig().getMessagesToConfigs().containsKey(event.getMessageId())) {
            return;
        }

        ReactForRolesMessageConfig messageConfig = getMessageConfigForId(event.getMessageId());
        Set<String> removedEmoteIds = messageConfig.getEmoteIdToRoleId().keySet();
        for (String emoteId : removedEmoteIds) {
            PotentialEmote pe = new PotentialEmote(event.getGuild(), emoteId);
            removeReactionToRoleMappingFromMessage(event.getGuild(), event.getMessageId(), pe, false);
        }
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        if (!getConfig().getMessagesToConfigs().containsKey(event.getMessageId())) {
            return;
        }

        deleteReactForRolesMessage(event.getGuild(), event.getMessageId(), null);
    }

    private synchronized ReactForRolesMessageConfig getMessageConfigForId(String messageId) {
        Objects.requireNonNull(messageId);

        ReactForRolesMessageConfig messageConfig = getConfig().getMessagesToConfigs().get(messageId);
        if (messageConfig == null) {
            throw new IllegalArgumentException("The provided messageId (`" + messageId
                    + "`) does not appear to be a valid react-for-roles message.");
        }

        return messageConfig;
    }

    private TextChannel getTextChannelForId(Guild guild, String textChannelId) {
        Objects.requireNonNull(guild);
        Objects.requireNonNull(textChannelId);

        TextChannel textChannel = guild.getTextChannelById(textChannelId);
        if (textChannel == null) {
            throw new IllegalStateException("Could not identify text channel corresponding to ID `" + textChannelId
                    + "`");
        }

        return textChannel;
    }

    private void alignState(Guild guild, String messageId, ReactForRolesMessageConfig messageConfig) {
        try {
            TextChannel textChannel = getTextChannelForId(guild, messageConfig.getTextChannelId());
            Message message = textChannel.retrieveMessageById(messageId).complete();
            String originalText = message.getContentRaw();
            String expectedText = messageConfig.renderMessage(guild);

            if (!originalText.equals(expectedText)) {
                message.editMessage(expectedText).queue();
            }

            for (MessageReaction reaction : message.getReactions()) {
                PotentialEmote pe = new PotentialEmote(reaction);

                if (messageConfig.getEmoteIdToRoleId().containsKey(pe.getEmoteRaw())) {
                    String roleId = messageConfig.getEmoteIdToRoleId().get(pe.getEmoteRaw());

                    List<User> reactionUsers = reaction.retrieveUsers().complete();
                    for (User user : reactionUsers) {
                        if (guild.getJDA().getSelfUser().getIdLong() == user.getIdLong()) {
                            continue;
                        }

                        toggleRoleForUser(guild, user, roleId);
                        reaction.removeReaction(user).complete();
                    }
                } else {
                    if (pe.getEmoteObject().isPresent()) {
                        message.clearReactions(pe.getEmoteObject().get()).queue();
                    } else {
                        message.clearReactions(pe.getEmoteRaw()).queue();
                    }
                }
            }

            List<PotentialEmote> missingEmotes = messageConfig.getEmoteIdToRoleId()
                    .keySet()
                    .stream()
                    .map(emoteId -> new PotentialEmote(guild, emoteId))
                    .collect(Collectors.toList());
            List<PotentialEmote> presentEmotes = message.getReactions()
                    .stream()
                    .map(PotentialEmote::new)
                    .collect(Collectors.toList());
            missingEmotes.removeAll(presentEmotes);

            for (PotentialEmote emote : missingEmotes) {
                removeReactionToRoleMappingFromMessage(guild, messageId, emote, false);
            }
        } catch (Exception e) {
            System.err.println("Failed to align state for message ID " + messageId + " in text channel "
                    + messageConfig.getTextChannelId() + ": " + e);
            e.printStackTrace(System.err);
        }
    }

    private void toggleRoleForUser(Guild guild, User user, String roleId) {
        Member member = guild.getMember(user);

        if (member == null) {
            throw new IllegalArgumentException(
                    "Attempted to toggle a role for a user that does not exist (" + user.getId() + ')');
        }

        toggleRoleForMember(guild, member, roleId);
    }

    private void toggleRoleForMember(Guild guild, Member member, String roleId) {
        Role role = guild.getRoleById(roleId);

        if (role == null) {
            throw new IllegalArgumentException("Attempted to use a role that does not exist (" + roleId + ')');
        }

        try {
            if (member.getRoles().contains(role)) {
                guild.removeRoleFromMember(member, role).reason("Requested via react-for-roles message").complete();
                PrivateChannel pc = guild.getJDA().openPrivateChannelById(member.getUser().getId()).complete();
                pc.sendMessage("Successfully removed the " + role.getName() + " role from you as requested.")
                        .complete();
            } else {
                guild.addRoleToMember(member, role).reason("Requested via react-for-roles message").complete();
                PrivateChannel pc = guild.getJDA().openPrivateChannelById(member.getUser().getId()).complete();
                pc.sendMessage("Successfully granted you the " + role.getName() + " role as requested.").complete();
            }
        } catch (ErrorResponseException e) {
            System.err.println("Could not send confirmation message to user: " + e);
            e.printStackTrace(System.err);
        }
    }

    private void handleReactionTotalRemoval(GuildMessageReactionRemoveEvent event, PotentialEmote emote) {
        String messageId = event.getMessageId();
        TextChannel textChannel = event.getChannel();
        Message message = textChannel.retrieveMessageById(messageId).complete();
        Set<PotentialEmote> existentReactions = message.getReactions()
                .stream()
                .map(PotentialEmote::new)
                .collect(Collectors.toSet());

        if (!existentReactions.contains(emote)) {
            try {
                removeReactionToRoleMappingFromMessage(event.getGuild(), messageId, emote, false);
            } catch (Exception e) {
                // Oh well
            }
        }
    }

    synchronized String createNewReactForRolesMessage(Guild guild, TextChannel textChannel, String message) {
        Message discordMessage = textChannel.sendMessage("This is not a text message").complete();
        String messageId = discordMessage.getId();

        ReactForRolesMessageConfig messageConfig = getConfig()
                .addAndGetNewReactForRolesMessage(messageId, textChannel.getId(), message);
        sync();

        discordMessage.editMessage(messageConfig.renderMessage(guild)).complete();
        return messageId;
    }

    synchronized void updateReactForRolesMessage(Guild guild, String messageId, String newMessage) {
        ReactForRolesMessageConfig messageConfig = getMessageConfigForId(messageId);
        TextChannel textChannel = getTextChannelForId(guild, messageConfig.getTextChannelId());
        Message discordMessage = textChannel.retrieveMessageById(messageId).complete();

        messageConfig.setMessageText(newMessage);
        sync();
        discordMessage.editMessage(messageConfig.renderMessage(guild)).complete();
    }

    synchronized void deleteReactForRolesMessage(Guild guild, String messageId, Member deletedBy) {
        ReactForRolesMessageConfig messageConfig = getMessageConfigForId(messageId);

        if (deletedBy != null) {
            TextChannel textChannel = getTextChannelForId(guild, messageConfig.getTextChannelId());
            Message discordMessage = textChannel.retrieveMessageById(messageId).complete();

            discordMessage.delete()
                    .reason("Requested by " + Miscellaneous.qualifyName(deletedBy))
                    .complete();
        }

        getConfig().removeReactForRolesMessageById(messageId);
        sync();
    }

    synchronized void addReactionToRoleMappingToMessage(Guild guild, String messageId, PotentialEmote potentialEmote,
            String roleId) {
        ReactForRolesMessageConfig messageConfig = getMessageConfigForId(messageId);
        TextChannel textChannel = getTextChannelForId(guild, messageConfig.getTextChannelId());
        Message discordMessage = textChannel.retrieveMessageById(messageId).complete();

        try {
            if (potentialEmote.getEmoteObject().isPresent()) {
                discordMessage.addReaction(potentialEmote.getEmoteObject().get()).complete();
            } else {
                discordMessage.addReaction(potentialEmote.getEmoteRaw()).complete();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to add reaction. Is the specified emote valid? " + e, e);
        }

        messageConfig.addEmoteToRoleIdMapping(potentialEmote.getEmoteRaw(), roleId);
        sync();
        String newMessage = messageConfig.renderMessage(guild);
        discordMessage.editMessage(newMessage).complete();
    }

    synchronized void removeReactionToRoleMappingFromMessage(Guild guild, String messageId,
            PotentialEmote potentialEmote, boolean clearEmotes) {
        ReactForRolesMessageConfig messageConfig = getMessageConfigForId(messageId);
        TextChannel textChannel = getTextChannelForId(guild, messageConfig.getTextChannelId());
        Message discordMessage = textChannel.retrieveMessageById(messageId).complete();

        if (!messageConfig.getEmoteIdToRoleId().containsKey(potentialEmote.getEmoteRaw())) {
            throw new IllegalArgumentException("React-for-roles message with ID `" + messageId + "` does not appear to "
                    + "define a mapping that corresponds to emote " + (potentialEmote.getEmoteObject().isPresent()
                    ? potentialEmote.getEmoteObject().get().getAsMention() : potentialEmote.getEmoteRaw()));
        }

        if (clearEmotes) {
            if (potentialEmote.getEmoteObject().isPresent()) {
                discordMessage.clearReactions(potentialEmote.getEmoteObject().get()).complete();
            } else {
                discordMessage.clearReactions(potentialEmote.getEmoteRaw()).complete();
            }
        }

        messageConfig.removeEmoteToRoleIdMapping(potentialEmote.getEmoteRaw());
        sync();

        discordMessage.editMessage(messageConfig.renderMessage(guild)).complete();
    }
}
