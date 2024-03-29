/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2023 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormat;

import com.troidsonly.modbot.commands.dumpmessages.MessageHistoryDumper;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

@SuppressWarnings({"NullableProblems", "ConstantConditions"})
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
        if (logUser == null) {
            sendToLog(message, (User) null, logChannel);
        } else {
            sendToLog(message, logUser.getUser(), logChannel);
        }
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

            target.sendMessage(message)
                    .addFile(file.toFile())
                    .complete();
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
            embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);

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
            embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);

            sendToLog(embedBuilder.build(), member, event.getChannel());
        }
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("User joined the server");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);

        Instant userCreatedTime = Instant.from(event.getUser().getTimeCreated());
        org.joda.time.Instant userCreatedInstant = new org.joda.time.Instant(userCreatedTime.toEpochMilli());
        Period userCreatedAgo = new Period(userCreatedInstant, (org.joda.time.Instant) null);
        String userCreatedAgoHumanReadable = PeriodFormat.getDefault().print(userCreatedAgo);

        embedBuilder.addField("User created",
                Miscellaneous.unixEpochToRfc1123DateTimeString(userCreatedTime.getEpochSecond()) +
                        ", " + userCreatedAgoHumanReadable + " ago", false);

        try {
            if (userCreatedAgo.toStandardWeeks().getWeeks() < 1) {
                embedBuilder.addField("Caution", "User created less than one week ago!", false);
            }
        } catch (UnsupportedOperationException e) {
            // Oh well
        }

        embedBuilder.setColor(new Color(0x00CC00));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        embedBuilder.setTitle("User left the server");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
        embedBuilder.setColor(new Color(0xFFFF00));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onGuildMemberUpdateNickname(GuildMemberUpdateNicknameEvent event) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (event.getNewNickname() == null) {
            embedBuilder.setTitle("User removed their nickname");
            embedBuilder.addField("Old nickname", event.getOldNickname() == null ? "(none)" : event.getOldNickname(),
                false);
        } else if (event.getOldNickname() == null) {
            embedBuilder.setTitle("User added a nickname");
        } else {
            embedBuilder.setTitle("User changed nickname");
            embedBuilder.setDescription("New nickname is given above.");
            embedBuilder.addField("Old nickname", event.getOldNickname(), false);
        }

        embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
        embedBuilder.setColor(new Color(0x55AAFF));

        sendToLog(embedBuilder.build(), event.getMember());
    }

    @Override
    public void onUserUpdateName(UserUpdateNameEvent event) {
        String oldUsername = event.getOldName();
        EmbedBuilder embedBuilder = new EmbedBuilder();
        TextChannel primaryLogChannel = jda.getTextChannelById(config.getPrimaryLogChannelId());

        embedBuilder.setTitle("User changed their username");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(primaryLogChannel.getGuild().getMember(event.getUser())), null,
            event.getUser().getAvatarUrl());
        embedBuilder.setDescription("New username is given above.");
        embedBuilder.addField("Old username", oldUsername, false);

        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
        embedBuilder.setColor(new Color(0x55AAFF));

        sendToLog(embedBuilder.build(), event.getUser(), null);
    }

    @Override
    public void onUserUpdateDiscriminator(UserUpdateDiscriminatorEvent event) {
        String oldDiscriminator = event.getOldDiscriminator();

        // Don't log if the discriminator didn't actually change. This appears to be common with users who have now
        // migrated to Discord usernames. Likely a temporary workaround until the JDA upgrade is done.
        if (oldDiscriminator.equals(event.getNewDiscriminator())) {
            return;
        }

        EmbedBuilder embedBuilder = new EmbedBuilder();
        TextChannel primaryLogChannel = jda.getTextChannelById(config.getPrimaryLogChannelId());

        embedBuilder.setTitle("User changed their discriminator, or migrated to Discord usernames");
        embedBuilder.setAuthor(Miscellaneous.qualifyName(primaryLogChannel.getGuild().getMember(event.getUser())), null,
                event.getUser().getAvatarUrl());
        embedBuilder.setDescription("New discriminator if any is given above.");
        embedBuilder.addField("Old discriminator was", "#" + oldDiscriminator, false);

        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(
                Instant.now().getEpochSecond()), null);
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
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
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
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
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
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
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
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
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
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
        embedBuilder.setColor(new Color(0xCC0000));

        sendToLog(embedBuilder.build(), (User) null, event.getChannel());
    }

    public MessageCache getMessageCache() {
        return messageCache;
    }
}
