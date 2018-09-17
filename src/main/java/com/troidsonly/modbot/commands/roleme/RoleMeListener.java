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

import java.awt.Color;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import com.troidsonly.modbot.commands.log.LogListener;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

public class RoleMeListener extends ListenerAdapter {

    private static final int SEND_JOIN_DM_DELAY_SECONDS = 7;

    private final PersistenceManager<RoleMeConfig> pm;
    private final AccessControl acl;
    private final LogListener logger;
    private final RoleMeConfig config;
    private final ScheduledExecutorService ses;

    private final RoleMeHandler commandHandler;

    public RoleMeListener(PersistenceWrapper<?> pm, AccessControl acl, LogListener logger,
            ScheduledExecutorService ses) {
        this.pm = Objects.requireNonNull(pm).getPersistenceManager("RoleMeConfig", RoleMeConfig.class);
        this.acl = Objects.requireNonNull(acl);
        this.logger = Objects.requireNonNull(logger);
        this.ses = Objects.requireNonNull(ses);

        config = this.pm.get().orElseGet(RoleMeConfig::ofEmpty);

        commandHandler = new RoleMeHandler(this);
    }

    public RoleMeHandler getCommandHandler() {
        return commandHandler;
    }

    PersistenceManager<RoleMeConfig> getPm() {
        return pm;
    }

    AccessControl getAcl() {
        return acl;
    }

    LogListener getLogger() {
        return logger;
    }

    RoleMeConfig getConfig() {
        return config;
    }

    synchronized void sync() {
        pm.persist(config);
        pm.sync();
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if (config.getAutoSendMessage() != null) {
            ses.schedule(() -> sendJoinDm(event), SEND_JOIN_DM_DELAY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private void sendJoinDm(GuildMemberJoinEvent event) {
        if (!event.getMember().getRoles().isEmpty()) {
            return;
        }

        PrivateChannel dm;

        try {
            dm = event.getUser().openPrivateChannel().complete();
        } catch (Exception e) {
            System.err.println(
                    "An error occurred when trying to open DM with " + Miscellaneous.qualifyName(event.getMember()));
            e.printStackTrace(System.err);
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("Error occurred while sending automatic welcome DM to user");
            embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getUser().getAvatarUrl());
            embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous
                    .unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
            embedBuilder.setDescription(e.toString());
            embedBuilder.setColor(new Color(0xCC0000));
            getLogger().sendToLog(embedBuilder.build(), event.getMember());
            return;
        }

        dm.sendMessage(config.getAutoSendMessage()).queue();
    }
}
