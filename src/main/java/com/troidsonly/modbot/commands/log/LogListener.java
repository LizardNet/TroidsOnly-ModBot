/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2018 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildBanEvent;
import net.dv8tion.jda.core.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberNickChangeEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import com.troidsonly.modbot.commands.dumpmessages.MessageHistoryDumper;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

public class LogListener extends ListenerAdapter {
    private final PersistenceManager<LoggerConfig> pm;
    private final LogCommandHandler commandHandler;
    private final LoggerConfig config;
    private final AccessControl acl;

    private JDA jda = null;
    private MessageCache messageCache = null;

    public LogListener(PersistenceWrapper<?> wrapper, AccessControl acl) {
        pm = wrapper.getPersistenceManager("LogListener", LoggerConfig.class);
        this.acl = acl;

        commandHandler = new LogCommandHandler(this);
        config = pm.get().orElseGet(LoggerConfig::empty);
    }

    public CommandHandler getCommandHandler() {
        return commandHandler;
    }

    LoggerConfig getConfig() {
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
    public void onReady(ReadyEvent event) {
        jda = event.getJDA();

        if (config.getEnabled()) {
            enableLog();
        }
    }

    synchronized void enableLog() {
        config.setEnabled(true);
        sync();
        TextChannel primaryLogChannel = jda.getTextChannelById(config.getPrimaryLogChannelId());
        messageCache = new MessageCache(primaryLogChannel);
        primaryLogChannel.sendMessage("Logging enabled.").queue();
    }

    synchronized void disableLog() {
        config.setEnabled(false);
        sync();
        if (config.getPrimaryLogChannelId() != null) {
            TextChannel primaryLogChannel = jda.getTextChannelById(config.getPrimaryLogChannelId());
            primaryLogChannel.sendMessage("Logging disabled; destroying MessageCache.").queue();
        }

        messageCache = null;
    }

    public void sendToLog(MessageEmbed message) {
        sendToLog(message, (User) null, null);
    }

    public void sendToLog(MessageEmbed message, Member logUser) {
        sendToLog(message, logUser, null);
    }

    public void sendToLog(MessageEmbed message, Member logUser, TextChannel logChannel) {
        sendToLog(message, logUser.getUser(), logChannel);
    }

    private TextChannel selectLogTarget(User logUser, TextChannel logChannel) {
        TextChannel target;

        if (logUser != null && config.getUserIdToLogChannelIdFilters().containsKey(logUser.getId())) {
            target = jda.getTextChannelById(config.getUserIdToLogChannelIdFilters().get(logUser.getId()));
        } else if (logChannel != null && config.getChannelIdToLogChannelIdFilters().containsKey(logChannel.getId())) {
            target = jda.getTextChannelById(config.getChannelIdToLogChannelIdFilters().get(logChannel.getId()));
        } else {
            target = jda.getTextChannelById(config.getPrimaryLogChannelId());
        }

        return target;
    }

    public void sendToLog(MessageEmbed message, User logUser, TextChannel logChannel) {
        Objects.requireNonNull(message);

        if (config.getEnabled() && jda != null) {
            TextChannel target = selectLogTarget(logUser, logChannel);

            target.sendMessage(message).queue();
        }
    }

    public void sendFileToLog(Path file, Message message, User logUser, TextChannel logChannel) {
        Objects.requireNonNull(file);

        if (config.getEnabled() && jda != null) {
            TextChannel target = selectLogTarget(logUser, logChannel);

            target.sendFile(file.toFile(), message).complete();
        }
    }

    public void sendToLog(String message) {
        if (config.getEnabled() && jda != null) {
            TextChannel target = jda.getTextChannelById(config.getPrimaryLogChannelId());
            target.sendMessage(message).queue();
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        if (messageCache != null) {
            messageCache.addMessage(event.getMessage());
        }
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        if (messageCache != null) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            Message newMessage = event.getMessage();
            Optional<Message> oldMessage = messageCache.getMessageById(event.getMessageId(), event.getChannel());

            embedBuilder.setTitle("Edited message in #" + event.getChannel().getName());
            embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getMember().getUser().getAvatarUrl());

            if (oldMessage.isPresent()) {
                if (Miscellaneous.getFullMessage(oldMessage.get()).equals(Miscellaneous.getFullMessage(newMessage))) {
                    // Texts are the same - we can ignore this, but still update the message cache.
                    messageCache.updateMessage(newMessage);
                    return;
                }

                embedBuilder.addField("Old Message", Miscellaneous.getFullMessage(oldMessage.get()), false);
            } else {
                embedBuilder.addField("Old message not available", "Could not find the old message text in the message " +
                    "cache; it was probably very old", false);
            }

            embedBuilder.addField("New Message", Miscellaneous.getFullMessage(newMessage), false);
            embedBuilder.addField("Message ID", newMessage.getId(), false);
            embedBuilder.setColor(new Color(0x55AAFF));
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter(getClass().getSimpleName(), null);

            messageCache.updateMessage(newMessage);

            sendToLog(embedBuilder.build(), event.getMember(), event.getChannel());
        }
    }

    @Override
    public void onGuildMessageDelete(GuildMessageDeleteEvent event) {
        if (messageCache != null) {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            Optional<Message> oldMessage = messageCache.getMessageById(event.getMessageId(), event.getChannel());
            Member member;

            embedBuilder.setTitle("Deleted message in #" + event.getChannel().getName());

            if (oldMessage.isPresent()) {
                embedBuilder.setDescription(Miscellaneous.getFullMessage(oldMessage.get()));
                member = oldMessage.get().getMember();
                if (member != null) {
                    embedBuilder.setAuthor(Miscellaneous.qualifyName(member), null, member.getUser().getAvatarUrl());
                } else {
                    embedBuilder.setAuthor(Miscellaneous.qualifyName(oldMessage.get().getAuthor()), null, oldMessage.get().getAuthor().getAvatarUrl());
                }
            } else {
                embedBuilder.setDescription("Unfortunately, I could not find the deleted message in my MessageCache, so I can't " +
                    "show any information about it.  This probably means it was old.");
                member = null;
            }

            embedBuilder.addField("Message ID", event.getMessageId(), false);
            embedBuilder.setColor(new Color(0xFF8800));
            embedBuilder.setTimestamp(Instant.now());
            embedBuilder.setFooter(getClass().getSimpleName(), null);

            sendToLog(embedBuilder.build(), member, event.getChannel());
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("User joined the server");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0x00CC00));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("User left the server");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0xFFFF00));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onGuildMemberNickChange(GuildMemberNickChangeEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (event.getNewNick() == null) {
            embedBuilder.setTitle("User removed their nickname");
            embedBuilder.addField("Old nickname", event.getPrevNick() == null ? "(none)" : event.getPrevNick(),
                false);
        } else if (event.getPrevNick() == null) {
            embedBuilder.setTitle("User added a nickname");
        } else {
            embedBuilder.setTitle("User changed nickname");
            embedBuilder.setDescription("New nickname is given above.");
            embedBuilder.addField("Old nickname", event.getPrevNick(), false);
        }

        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0x55AAFF));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onUserNameUpdate(UserNameUpdateEvent event) {
        String oldUsername = event.getOldName() + '#' + event.getOldDiscriminator();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        TextChannel primaryLogChannel = jda.getTextChannelById(config.getPrimaryLogChannelId());

        embedBuilder.setTitle("User changed their username");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(primaryLogChannel.getGuild().getMember(event.getUser())), null,
            event.getUser().getAvatarUrl());
        embedBuilder.setDescription("New username is given above.");
        embedBuilder.addField("Old username", oldUsername, false);

        if (!event.getOldDiscriminator().equals(event.getUser().getDiscriminator())) {
            embedBuilder.addField("Attention:", "Discriminator changed!", false);
        }

        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0x55AAFF));

        sendToLog(embedBuilder.build(), event.getUser(), null);
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        List<String> roles = event.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toList());

        embedBuilder.setTitle("User granted role(s)");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setDescription(Miscellaneous.getStringRepresentation(roles));
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0x00CC00));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        List<String> roles = event.getRoles().stream()
            .map(Role::getName)
            .collect(Collectors.toList());

        embedBuilder.setTitle("Role(s) revoked from user");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setDescription(Miscellaneous.getStringRepresentation(roles));
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0xFF8800));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onGuildBan(GuildBanEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        MessageBuilder dumpLogMessage = new MessageBuilder();
        Path dumpFilePath = null;

        try {
            dumpFilePath = MessageHistoryDumper.dumpMemberMessageHistory(event.getUser(), event.getGuild(), messageCache);
            dumpLogMessage.append("Message history for banned user:");
        } catch (Exception e) {
            embedBuilder.addField("Failed to generate message dump", e.toString(), false);
        }

        embedBuilder.setTitle("Ban set against above user");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getUser()), null, event.getUser().getAvatarUrl());
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0xCC0000));

        sendToLog(embedBuilder.build(), event.getUser(), null);

        if (dumpFilePath != null) {
            sendFileToLog(dumpFilePath, dumpLogMessage.build(), event.getUser(), null);
            try {
                Files.deleteIfExists(dumpFilePath);
            } catch (IOException e) {
                // Oh well
            }
        }
    }

    @Override
    public void onGuildUnban(GuildUnbanEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("Ban against above user rescinded");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getUser()), null, event.getUser().getAvatarUrl());
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0xCC00CC));

        sendToLog(embedBuilder.build(), event.getUser(), null);
    }

    @Override
    public void onMessageBulkDelete(MessageBulkDeleteEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        List<String> targeted = event.getMessageIds().stream()
            .map(messageId -> messageCache.getMessageById(messageId, event.getChannel()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(Message::getAuthor)
            .map(Miscellaneous::qualifyName)
            .collect(Collectors.toList());

        embedBuilder.setTitle("Message bulk delete logged in channel #" + event.getChannel().getName());
        embedBuilder.addField("Affected users", Miscellaneous.getStringRepresentation(targeted), false);
        embedBuilder.setTimestamp(Instant.now());
        embedBuilder.setFooter(getClass().getSimpleName(), null);
        embedBuilder.setColor(new Color(0xCC0000));

        sendToLog(embedBuilder.build(), (User) null, event.getChannel());
    }

    public MessageCache getMessageCache() {
        return messageCache;
    }
}
