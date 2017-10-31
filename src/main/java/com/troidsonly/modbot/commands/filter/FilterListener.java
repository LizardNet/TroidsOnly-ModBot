/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.filter;

import java.awt.Color;
import java.time.Instant;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import com.troidsonly.modbot.commands.cryo.CryoHandler;
import com.troidsonly.modbot.commands.log.LogListener;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.persistence.PersistenceManager;
import com.troidsonly.modbot.persistence.PersistenceWrapper;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

public class FilterListener extends ListenerAdapter {
    private final AccessControl acl;
    private final LogListener logger;
    private final PersistenceManager<FilterRepository> pm;
    private final CryoHandler cryoHandler;
    private final ExecutorService executorService;
    private final String fantasyString;

    private final FilterCommandHandler filterCommandHandler;
    private final FilterRepository filterRepository;

    private JDA jda = null;

    public FilterListener(AccessControl acl, LogListener logger, PersistenceWrapper<?> wrapper, CryoHandler cryoHandler, ExecutorService executorService, String fantasyString) {
        this.acl = acl;
        this.logger = logger;
        pm = wrapper.getPersistenceManager("FilterListener", FilterRepository.class);
        this.cryoHandler = cryoHandler;
        this.executorService = executorService;
        this.fantasyString = fantasyString;

        filterCommandHandler = new FilterCommandHandler(this);
        filterRepository = pm.get().orElseGet(FilterRepository::empty);
        filterRepository.setGetFilterListCallback(this::doExpiryChecks);
    }

    public CommandHandler getCommandHandler() {
        return filterCommandHandler;
    }

    @Override
    public void onReady(ReadyEvent event) {
        jda = event.getJDA();
    }

    synchronized void sync() {
        pm.persist(filterRepository);
        pm.sync();
    }

    FilterRepository getFilterRepository() {
        return filterRepository;
    }

    CryoHandler getCryoHandler() {
        return cryoHandler;
    }

    AccessControl getAcl() {
        return acl;
    }

    LogListener getLogger() {
        return logger;
    }

    ExecutorService getExecutorService() {
        return executorService;
    }

    private void doExpiryChecks(List<RegexFilter> filterList) {
        if (jda == null) {
            return;
        }

        Collections.sort(filterList);

        synchronized (filterRepository) {
            Iterator<RegexFilter> itr = filterList.iterator();

            while (itr.hasNext()) {
                RegexFilter filter = itr.next();

                if (filter.getExpiry() != null && filter.getExpiry() < Instant.now().getEpochSecond()) {
                    EmbedBuilder embedBuilder = new EmbedBuilder();
                    User addingUser = jda.getUserById(filter.getCreatorUid());

                    embedBuilder.setTitle("Filter expired and automatically removed");
                    embedBuilder.setDescription('`' + filter.getRegex() + '`');
                    embedBuilder.addField("Performing action", filter.getAction().toString(), false);
                    embedBuilder.addField("Originally added by", addingUser.getName() + '#' + addingUser.getDiscriminator(), false);
                    embedBuilder.addField("Originally added at", Miscellaneous.unixEpochToRfc1123DateTimeString(filter.getCreationTime()), false);
                    embedBuilder.addField("Original comment", filter.getComment(), false);
                    embedBuilder.setTimestamp(Instant.now());
                    embedBuilder.setColor(new Color(0xAAAAAA));

                    logger.sendToLog(embedBuilder.build(), (User) null, null);
                    itr.remove();
                } else {
                    break;
                }
            }
        }

        sync();
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        applyFilters(Miscellaneous.getFullMessage(event.getMessage()), event);
    }

    private void applyFilters(String fullMessage, GuildMessageReceivedEvent event) {
        Member member = event.getMember();

        if (member.getUser().equals(jda.getSelfUser())) {
            // Ignore the bot outright
            return;
        }

        if ((fullMessage.startsWith(fantasyString + FilterCommandHandler.CMD_FILTER) || fullMessage.startsWith(FilterCommandHandler.CMD_FILTER)) && acl.hasPermission(member, FilterCommandHandler.PERM_FILTER)) {
            // The message appears to be a filter command from an authorized user; also ignore these outright
            return;
        }

        FilterRunner runner = new FilterRunner(this, event, member, fullMessage);
        executorService.submit(runner);
    }
}
