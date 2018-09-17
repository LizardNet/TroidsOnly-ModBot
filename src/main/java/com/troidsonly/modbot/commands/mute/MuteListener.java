package com.troidsonly.modbot.commands.mute;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.ISnowflake;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GenericGuildEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import com.troidsonly.modbot.commands.dumpmessages.MessageHistoryDumper;
import com.troidsonly.modbot.commands.log.LogListener;
import com.troidsonly.modbot.commands.mute.MuteConfig.Mute;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

public class MuteListener extends ListenerAdapter {

    private final AccessControl acl;
    private final PersistenceManager<MuteConfig> pm;
    private final MuteConfig config;
    private final LogListener logListener;
    private final ScheduledExecutorService scheduledExecutorService;

    private final MuteHandler commandHandler;

    private JDA jda;
    private ScheduledFuture<?> pruneExpiredMutesJob;
    private boolean firstRun = true;

    public MuteListener(AccessControl acl, PersistenceWrapper<?> wrapper, LogListener logListener,
            ScheduledExecutorService scheduledExecutorService) {
        this.acl = acl;
        // Old name preserved as namespace for backwards comaptibility
        pm = wrapper.getPersistenceManager("CryoConfig", MuteConfig.class);
        this.logListener = logListener;
        this.scheduledExecutorService = scheduledExecutorService;

        config = pm.get().orElseGet(MuteConfig::empty);

        commandHandler = new MuteHandler(this);
    }

    public MuteHandler getCommandHandler() {
        return commandHandler;
    }

    AccessControl getAcl() {
        return acl;
    }

    LogListener getLogListener() {
        return logListener;
    }

    PersistenceManager<MuteConfig> getPm() {
        return pm;
    }

    MuteConfig getConfig() {
        return config;
    }

    synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }

    public String getMuteRoleId() {
        return config.getMuteRoleId();
    }

    @Override
    public void onReady(ReadyEvent event) {
        jda = event.getJDA();
        scheduleMutePruning();
    }

    @Override
    public synchronized void onGenericGuild(GenericGuildEvent event) {
        // FIXME
        if (!firstRun) {
            return;
        }

        if (getMuteRoleId() == null) {
            return;
        }

        Role muteRole = event.getGuild().getRoleById(getMuteRoleId());
        config.getMutedUserTracker().putAll(event.getGuild().getMembersWithRoles(muteRole).stream()
                .map(m -> m.getUser().getId())
                .filter(id -> !config.getMutedUserTracker().containsKey(id))
                .collect(Collectors.toMap(id -> id, id -> {
                    Mute mute = new Mute();
                    mute.setCreationTime(Instant.now().getEpochSecond());
                    mute.setComment("Added automatically as an existing but unknown mute when the bot was started.");
                    return mute;
                })));

        sync();
        firstRun = false;
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        String userId = event.getUser().getId();

        if (config.getMutedUserTracker().containsKey(userId)) {
            MuteConfig.Mute muteInfo = config.getMutedUserTracker().get(userId);

            if (!muteInfo.hasExpired()) {
                Role muteRole = event.getGuild().getRoleById(config.getMuteRoleId());
                event.getGuild().getController().addRolesToMember(event.getMember(), muteRole)
                        .reason("Automatically re-adding mute to user who left server while muted.")
                        .complete();

                String message = "Welcome back to " + event.getGuild().getName() + ". Because you were muted when you "
                        + "left the server last, and this mute has not expired, you have been automatically re-muted. "
                        + "Please contact the mod team if you have any questions.";
                String failure = null;

                try {
                    PrivateChannel dm = event.getUser().openPrivateChannel().complete();
                    dm.sendMessage(message).complete();
                } catch (Exception e) {
                    failure = "Failed to send message to user on join: `" + e.toString() + '`';
                }

                String mutedBy = "(unknown)";

                if (muteInfo.getCreatorUid() != null) {
                    User targetUser = event.getJDA().getUserById(muteInfo.getCreatorUid());
                    if (targetUser != null) {
                        mutedBy = targetUser.getName();
                    } else {
                        mutedBy = "UID: " + muteInfo.getCreatorUid();
                    }
                }

                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle("Muted user re-joined the server and has been re-muted automatically");
                embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null,
                        event.getUser().getAvatarUrl());
                embedBuilder.setFooter(
                        getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(
                                Instant.now().getEpochSecond()), null);

                embedBuilder.addField("Originally muted by", mutedBy, false);
                embedBuilder.addField("Originally muted at",
                        Miscellaneous.unixEpochToRfc1123DateTimeString(muteInfo.getCreationTime()), false);
                embedBuilder.addField("Muted until", muteInfo.getExpiryTime() == null ? "forever"
                        : Miscellaneous.unixEpochToRfc1123DateTimeString(muteInfo.getExpiryTime()), false);

                if (failure != null) {
                    embedBuilder.addField("Failed to send DM to user", failure, false);
                }

                embedBuilder.setColor(new Color(0xCC0000));
                logListener.sendToLog(embedBuilder.build(), event.getMember());
            }
        }
    }

    void scheduleMutePruning() {
        if (jda == null) {
            return;
        }

        Long nextExpiry = getNextExpiry();
        if (nextExpiry == null) {
            if (pruneExpiredMutesJob != null) {
                pruneExpiredMutesJob.cancel(false);
            }
            pruneExpiredMutesJob = null;
            return;
        }

        long delay = nextExpiry - Instant.now().getEpochSecond();
        if (delay < 0) {
            delay = 0;
        }

        pruneExpiredMutesJob = scheduledExecutorService.schedule(() -> {
            removeExpiredMutes();
            scheduleMutePruning();
        }, delay, TimeUnit.SECONDS);
    }

    private synchronized Long getNextExpiry() {
        return config.getMutedUserTracker().values().stream()
                .map(Mute::getExpiryTime)
                .filter(Objects::nonNull)
                .sorted()
                .findFirst()
                .orElse(null);
    }

    private synchronized void removeExpiredMutes() {
        if (jda == null) {
            return;
        }

        Role muteRole = jda.getRoleById(getMuteRoleId());
        Guild guild = muteRole.getGuild();

        Iterator<Entry<String, Mute>> iter = config.getMutedUserTracker().entrySet().iterator();
        while (iter.hasNext()) {
            removeExpiredMute(guild, muteRole, iter);
        }

        sync();
    }

    private void removeExpiredMute(Guild guild, Role muteRole, Iterator<Entry<String, Mute>> iter) {
        Entry<String, Mute> entry = iter.next();
        String targetUid = entry.getKey();
        Mute muteInfo = entry.getValue();

        Member targetMember = guild.getMemberById(targetUid);
        User targetUser = jda.getUserById(targetUid);
        if (targetMember != null) {
            guild.getController().removeRolesFromMember(targetMember, muteRole)
                    .reason("Removing mute role as the mute has expired.")
                    .complete();
        }

        iter.remove();

        EmbedBuilder embedBuilder = new EmbedBuilder();

        String mutedAt = "(unknown)";
        String mutedBy = "(unknown)";
        String muteComment = "(none)";

        if (muteInfo != null) {
            mutedAt = Miscellaneous.unixEpochToRfc1123DateTimeString(muteInfo.getCreationTime());

            if (muteInfo.getCreatorUid() != null) {
                User user = jda.getUserById(muteInfo.getCreatorUid());
                if (user != null) {
                    mutedBy = user.getName();
                }
            }

            if (muteInfo.getComment() != null) {
                muteComment = muteInfo.getComment();
            }
        }

        embedBuilder.setTitle("Mute against above user expired and was removed");
        if (targetUser != null) {
            embedBuilder.setAuthor(Miscellaneous.qualifyName(targetUser), null, targetUser.getAvatarUrl());
        } else {
            embedBuilder.setAuthor("UUID " + targetUid, null, null);
        }

        embedBuilder.addField("Was originally muted at: ", mutedAt, false);
        embedBuilder.addField("Was originally muted by: ", mutedBy, false);
        embedBuilder.addField("Mute comment:", muteComment, false);
        embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous
                .unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
        embedBuilder.setColor(new Color(0xCC00CC));

        logListener.sendToLog(embedBuilder.build(), targetUser, null);
    }

    @Override
    public synchronized void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if (event.getRoles().stream()
                .map(ISnowflake::getId)
                .anyMatch(id -> id.equals(getMuteRoleId()))
        ) {
            Mute muteInfo = new Mute();
            muteInfo.setCreationTime(Instant.now().getEpochSecond());
            muteInfo.setComment("Role manually added to user instead of mute command.");
            if (!config.getMutedUserTracker().containsKey(event.getUser().getId())) {
                config.getMutedUserTracker().put(event.getMember().getUser().getId(), muteInfo);
                sync();
            }

            EmbedBuilder embedBuilder = new EmbedBuilder();
            MessageBuilder dumpLogMessage = new MessageBuilder();
            Path dumpFilePath = null;

            try {
                dumpFilePath = MessageHistoryDumper
                        .dumpMemberMessageHistory(event.getUser(), event.getGuild(), logListener.getMessageCache());
                dumpLogMessage.append("Message history for muted user:");
            } catch (Exception e) {
                embedBuilder.addField("Failed to generate message dump", e.toString(), false);
            }

            embedBuilder.setTitle("Mute set against above user");
            embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getUser()), null, event.getUser().getAvatarUrl());
            embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous
                    .unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
            embedBuilder.setColor(new Color(0xCC0000));

            logListener.sendToLog(embedBuilder.build(), event.getUser(), null);

            if (dumpFilePath != null) {
                logListener.sendFileToLog(dumpFilePath, dumpLogMessage.build(), event.getUser(), null);
                try {
                    Files.deleteIfExists(dumpFilePath);
                } catch (IOException e) {
                    // Oh well
                }
            }
        }
    }

    @Override
    public synchronized void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event) {
        if (event.getRoles().stream()
                .map(ISnowflake::getId)
                .anyMatch(id -> id.equals(getMuteRoleId()))
        ) {
            String mutedAt = "(unknown)";
            String mutedBy = "(unknown)";
            String muteComment = "(none)";

            Mute muteInfo = config.getMutedUserTracker().remove(event.getUser().getId());
            sync();

            if (muteInfo != null) {
                mutedAt = Miscellaneous.unixEpochToRfc1123DateTimeString(muteInfo.getCreationTime());

                if (muteInfo.getCreatorUid() != null) {
                    User user = event.getJDA().getUserById(muteInfo.getCreatorUid());
                    if (user != null) {
                        mutedBy = user.getName();
                    }
                }

                if (muteInfo.getComment() != null) {
                    muteComment = muteInfo.getComment();
                }
            }

            EmbedBuilder embedBuilder = new EmbedBuilder();

            embedBuilder.setTitle("Mute against above user rescinded");
            embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getUser()), null, event.getUser().getAvatarUrl());
            embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous
                    .unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
            embedBuilder.addField("Was originally muted at: ", mutedAt, false);
            embedBuilder.addField("Was originally muted by: ", mutedBy, false);
            embedBuilder.addField("Mute comment:", muteComment, false);
            embedBuilder.setColor(new Color(0xCC00CC));

            logListener.sendToLog(embedBuilder.build(), event.getUser(), null);
        }
    }
}
