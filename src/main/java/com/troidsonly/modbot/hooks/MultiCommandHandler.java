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
 *
 * The code in this file is modified from the following class(es) in LizardIRC/Beancounter:
 *   org.lizardirc.beancounter.hooks.MultiCommandHandler
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.hooks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class MultiCommandHandler implements CommandHandler {
    private final Set<CommandHandler> subCommandHandlers;

    // Mmm, generics
    private Map<GuildMessageReceivedEvent, Map<List<String>, Set<CommandHandler>>> listenerMap = new WeakHashMap<>();

    public MultiCommandHandler() {
        subCommandHandlers = new HashSet<>();
    }

    public MultiCommandHandler(Collection<CommandHandler> c) {
        subCommandHandlers = new HashSet<>(c);
    }

    public synchronized void add(CommandHandler listener) {
        subCommandHandlers.add(listener);
    }

    public synchronized void remove(CommandHandler listener) {
        subCommandHandlers.remove(listener);
    }

    public synchronized Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        Set<CommandHandler> listeners = subCommandHandlers;
        if (listenerMap.containsKey(event)) {
            listeners = listenerMap.get(event).get(ImmutableList.copyOf(commands));
        } else {
            listenerMap.put(event, new HashMap<>());
        }

        Map<List<String>, Set<CommandHandler>> map = listenerMap.get(event);
        for (CommandHandler listener : listeners) {
            for (String str : listener.getSubCommands(event, commands)) {
                List<String> newCommands = append(commands, str);
                if (!map.containsKey(newCommands)) {
                    map.put(newCommands, new HashSet<>());
                }
                map.get(newCommands).add(listener);
            }
        }
        return map.keySet().stream()
            .filter(l -> l.size() == commands.size() + 1)
            .map(l -> l.get(l.size() - 1)) // last element of each
            .collect(Collectors.toSet());
    }

    public synchronized void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (!listenerMap.containsKey(event)) {
            throw new IllegalStateException("Listener map not populated");
        }

        Set<CommandHandler> listeners = listenerMap.get(event).get(commands);
        if (listeners == null) {
            throw new IllegalStateException("No possible handlers for command");
        } else if (listeners.size() != 1) {
            throw new IllegalStateException(listeners.size() + " possible handlers for command");
        }

        listenerMap.remove(event);

        for (CommandHandler listener : listeners) {
            listener.handleCommand(event, commands, remainder);
        }
    }

    private List<String> append(List<String> commands, String tail) {
        List<String> ret = new ArrayList<>(commands);
        ret.add(tail);
        return Collections.unmodifiableList(ret);
    }
}
