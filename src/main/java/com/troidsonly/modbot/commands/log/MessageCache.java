/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2020 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.log;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.EvictingQueue;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

@SuppressWarnings("UnstableApiUsage")
public class MessageCache {
    private static final int QUEUE_SIZE = 1000;

    private final LoadingCache<TextChannel, Queue<Message>> messageCache;

    public MessageCache(TextChannel primaryLoggingChannel) {
        primaryLoggingChannel.sendMessage("Initializing message cache; " + QUEUE_SIZE +
            " messages per channel will be saved in the EvictingQueue").queue();

        messageCache = CacheBuilder.newBuilder()
            .build(CacheLoader.from(() -> EvictingQueue.create(QUEUE_SIZE)));

        primaryLoggingChannel.sendMessage("Enumerating and requesting history for all text channels...").queue();
        List<TextChannel> channels = primaryLoggingChannel.getGuild().getTextChannels();

        final int messagesToRetrieve = Math.min(QUEUE_SIZE, 100);

        for (TextChannel channel : channels) {
            try {
                List<Message> messages = new WorkaroundMessageHistory(channel)
                        .retrievePast(messagesToRetrieve)
                        .complete();
                messageCache.getUnchecked(channel).addAll(messages);
                primaryLoggingChannel.sendMessage("Retrieved " + messages.size() + " messages for #" +
                    channel.getName() + " and added them to the message cache").queue();
            } catch (Exception e) {
                primaryLoggingChannel.sendMessage("Failed to retrieve history for #" + channel.getName() +
                ": `" + e.toString() + "`; continuing with next channel.").queue();
            }
        }

        primaryLoggingChannel.sendMessage("MessageCache ready.").queue();
    }

    public void addMessage(Message message) {
        messageCache.getUnchecked(message.getTextChannel()).add(message);
    }

    public void updateMessage(Message message) {
        messageCache.getUnchecked(message.getTextChannel()).remove(message); // This way we only keep the most recent version of the message around
        addMessage(message);
    }

    public Optional<Message> getMessageById(String messageId, TextChannel channel) {
        return messageCache.getUnchecked(channel).stream()
            .filter(message -> message.getId().equals(messageId))
            .findFirst();
    }

    public List<Message> getMessagesByChannel(TextChannel channel) {
        return new ArrayList<>(messageCache.getUnchecked(channel));
    }
}
