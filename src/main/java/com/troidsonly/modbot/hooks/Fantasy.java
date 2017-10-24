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
 *   org.lizardirc.beancounter.hooks.Fantasy
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.hooks;

import java.lang.reflect.Field;

import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.impl.MessageImpl;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.EventListener;

public class Fantasy extends Decorator {
    private final String fantasyPrefix;
    private final int fantasyLength;

    public Fantasy(EventListener childListener, String fantasyPrefix) {
        super(childListener);
        this.fantasyPrefix = fantasyPrefix;
        this.fantasyLength = fantasyPrefix.length();
    }

    @Override
    public void onEvent(Event event) {
        if (event instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent me = (GuildMessageReceivedEvent) event;
            String newMessage = processMessage(me.getMessage());
            if (newMessage != null) {
                ((MessageImpl) me.getMessage()).setContent(processMessage(me.getMessage()));
                doFuckery((MessageImpl) me.getMessage());
                super.onEvent(me);
            }
        } else if (event instanceof PrivateMessageReceivedEvent) {
            PrivateMessageReceivedEvent me = (PrivateMessageReceivedEvent) event;
            String newMessage = processMessage(me.getMessage());
            if (newMessage != null) {
                ((MessageImpl) me.getMessage()).setContent(processMessage(me.getMessage()));
                doFuckery((MessageImpl) me.getMessage());
                super.onEvent(me);
            } else {
                super.onEvent(me);
            }
        }
    }

    private String processMessage(Message message) {
        if (message.getContent().startsWith(fantasyPrefix)) {
            return message.getRawContent().substring(fantasyLength);
        } else if (message.isMentioned(message.getJDA().getSelfUser()) && message.getRawContent().startsWith("<@" + message.getJDA().getSelfUser().getId() + '>')) {
            return message.getRawContent().substring(message.getJDA().getSelfUser().getId().length() + 3).trim();
        }
        return null;
    }

    private void doFuckery(MessageImpl message) {
        // wheeeeeeeeeeeeeee reflection!
        // I'm a professional - don't try this at home, kids!

        Class<? extends MessageImpl> clazz = message.getClass();
        Field field;
        try {
            field = clazz.getDeclaredField("subContent");
            field.setAccessible(true);
            field.set(message, null);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Fuckery failed: " + e.toString());
            e.printStackTrace(System.err); // o no
        }
    }
}
