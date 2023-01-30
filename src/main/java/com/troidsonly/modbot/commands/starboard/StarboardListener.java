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

import java.awt.Color;
import java.time.Instant;
import java.util.Objects;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;
import com.troidsonly.modbot.utils.PotentialEmote;

public class StarboardListener extends ListenerAdapter {

    private final PersistenceManager<StarboardState> pm;
    private final AccessControl acl;
    private final StarboardCommandHandler commandHandler;

    private final StarboardState state;

    public StarboardListener(PersistenceWrapper<?> wrapper, AccessControl acl) {
        this.acl = Objects.requireNonNull(acl);

        this.commandHandler = new StarboardCommandHandler(this);
        pm = wrapper.getPersistenceManager("StarboardListener", StarboardState.class);
        state = pm.get().orElseGet(StarboardState::empty);
    }

    public StarboardCommandHandler getCommandHandler() {
        return commandHandler;
    }

    StarboardState getState() {
        return state;
    }

    AccessControl getAcl() {
        return acl;
    }

    synchronized void sync() {
        state.pruneOldMessages();
        pm.persist(state);
        pm.sync();
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent event) {
        String messageId = event.getMessageId();
        String channelId = event.getChannel().getId();

        StarboardConfig starboardConfig = getState().getConfigForChannel(channelId);

        if (!starboardConfig.isEnabled()) {
            return;
        }

        PotentialEmote reactionEmote = new PotentialEmote(event.getReactionEmote());
        PotentialEmote starboardEmote = new PotentialEmote(event.getGuild(),
                starboardConfig.getStarboardEmoteId().get());

        if (getState().getMessageStateFor(channelId, messageId).isPresent()) {
            handlePromotedMessage(event, getState().getMessageStateFor(channelId, messageId).get());
        } else {
            if (!reactionEmote.equals(starboardEmote)) {
                return;
            }

            handleRegularMessage(event);
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private synchronized void handleRegularMessage(GuildMessageReactionAddEvent event) {
        String messageId = event.getMessageId();
        String channelId = event.getChannel().getId();

        if (getState().getMessageStateFor(channelId, messageId).isPresent()) {
            return; // Defensive check to avoid race condition weirdness
        }

        StarboardConfig starboardConfig = getState().getConfigForChannel(channelId);

        TextChannel starboardChannel = event.getGuild()
                .getTextChannelById(starboardConfig.getTargetStarboardChannelId().get());
        if (starboardChannel == null) {
            throw new IllegalStateException("The configured Starboard channel no longer exists.");
        }

        PotentialEmote starboardEmote = new PotentialEmote(event.getGuild(),
                starboardConfig.getStarboardEmoteId().get());

        Message message = Objects.requireNonNull(event.getChannel())
                .retrieveMessageById(messageId)
                .complete();

        if (message.getTimeCreated().toEpochSecond()
                < Instant.now().getEpochSecond() - StarboardState.NINETY_DAYS_IN_SECONDS) {
            return;
        }

        int reactionCount = getReactionCount(message, starboardEmote);

        if (reactionCount < starboardConfig.getPromotionThreshold().getAsInt()) {
            return;
        }

        String starboardMessageId = starboardChannel.sendMessage(
                        generateStarboardPreamble(starboardEmote, event.getChannel(), reactionCount))
                .embed(generateStarboardEmbed(message))
                .complete()
                .getId();

        StarboardMessageState messageState = StarboardMessageState.of(starboardChannel.getId(), starboardMessageId,
                starboardEmote.getEmoteRaw());
        getState().addMessageState(channelId, messageId, messageState);
        sync();
    }

    private synchronized void handlePromotedMessage(GuildMessageReactionAddEvent event, StarboardMessageState messageState) {
        Message message = Objects.requireNonNull(event.getChannel())
                .retrieveMessageById(event.getMessageId())
                .complete();

        PotentialEmote emote = new PotentialEmote(event.getGuild(), messageState.getEmoteId());
        int reactionCount = getReactionCount(message, emote);

        String newPreamble = generateStarboardPreamble(emote, event.getChannel(), reactionCount);

        Objects.requireNonNull(event.getGuild()
                        .getTextChannelById(messageState.getStarboardChannelId()))
                .retrieveMessageById(messageState.getStarboardMessageId())
                .complete()
                .editMessage(newPreamble)
                .complete();
    }

    private static int getReactionCount(Message message, PotentialEmote potentialEmote) {
        MessageReaction reaction;

        if (potentialEmote.getEmoteObject().isPresent()) {
            reaction = message.getReactions().stream()
                    .filter(r -> r.getReactionEmote().getEmote().getId().equals(potentialEmote.getEmoteRaw()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "A reaction emote was added, but couldn't retrieve it from the Message"));
        } else {
            reaction = message.getReactions().stream()
                    .filter(r -> r.getReactionEmote().getEmoji().equals(potentialEmote.getEmoteRaw()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "A reaction emoji was added, but couldn't retrieve it from the Message"));
        }

        return reaction.getCount();
    }

    private static MessageEmbed generateStarboardEmbed(Message message) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setAuthor(Miscellaneous.qualifyName(message.getMember(), false), message.getJumpUrl(),
                message.getAuthor().getAvatarUrl());
        embedBuilder.setColor(new Color(0xDDAA00));
        embedBuilder.setDescription(message.getContentRaw());
        embedBuilder.setTimestamp(message.getTimeCreated());

        if (!message.getAttachments().isEmpty()) {
            for (Attachment attachment : message.getAttachments()) {
                if (attachment.isImage() && !attachment.isSpoiler()) {
                    embedBuilder.setImage(attachment.getUrl());
                    break;
                }
            }
        }

        embedBuilder.setFooter(message.getId());
        Field sourceField = new Field("Show original", "[Click here](" + message.getJumpUrl() + ')', true);
        embedBuilder.addField(sourceField);

        return embedBuilder.build();
    }

    private static String generateStarboardPreamble(PotentialEmote emote, TextChannel sourceChannel, int reactCount) {
        StringBuilder sb = new StringBuilder("From ")
                .append(sourceChannel.getAsMention())
                .append(" with **")
                .append(reactCount)
                .append("** ");

        if (emote.getEmoteObject().isPresent()) {
            sb.append(emote.getEmoteObject().get().getAsMention());
        } else {
            sb.append(emote.getEmoteRaw());
        }

        return sb.toString();
    }
}
