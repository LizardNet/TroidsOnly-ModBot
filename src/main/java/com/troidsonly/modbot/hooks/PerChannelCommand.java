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
 *
 * The code in this file is modified from the following class(es) in LizardIRC/Beancounter:
 *   org.lizardirc.beancounter.hooks.PerChannelCommand
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.hooks;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.channel.text.GenericTextChannelEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class PerChannelCommand implements CommandHandler {
    private final LoadingCache<TextChannel, ? extends CommandHandler> childListeners;

    public PerChannelCommand(Function<TextChannel, ? extends CommandHandler> childFunction) {
        childListeners = CacheBuilder.newBuilder()
            .build(CacheLoader.from(childFunction));
    }

    public PerChannelCommand(Supplier<? extends CommandHandler> childSupplier) {
        childListeners = CacheBuilder.newBuilder()
            .build(CacheLoader.from(childSupplier));
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        TextChannel channel = getChannel(event);
        if (channel != null) {
            try {
                return childListeners.get(channel).getSubCommands(event, commands);
            } catch (ExecutionException e) {
                e.printStackTrace(); // TODO log this properly
            }
        }
        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        TextChannel channel = getChannel(event);
        if (channel != null) {
            try {
                childListeners.get(channel).handleCommand(event, commands, remainder);
            } catch (ExecutionException e) {
                e.printStackTrace(); // TODO log this properly
            }
        }
    }

    private TextChannel getChannel(Event event) {
        if (event instanceof GenericTextChannelEvent) {
            GenericTextChannelEvent gce = (GenericTextChannelEvent) event;
            return gce.getChannel();
        }
        return null;
    }
}
