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
 *   org.lizardirc.beancounter.hooks.Fantasy
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.hooks;

import java.lang.reflect.Field;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.internal.entities.AbstractMessage;
import net.dv8tion.jda.internal.entities.ReceivedMessage;

public class Fantasy extends Decorator {
    private final String fantasyPrefix;
    private final int fantasyLength;

    public Fantasy(EventListener childListener, String fantasyPrefix) {
        super(childListener);
        this.fantasyPrefix = fantasyPrefix;
        this.fantasyLength = fantasyPrefix.length();
    }

    @Override
    public void onEvent(GenericEvent event) {
        if (event instanceof GuildMessageReceivedEvent) {
            GuildMessageReceivedEvent me = (GuildMessageReceivedEvent) event;
            String newMessage = processMessage(me.getMessage());
            if (newMessage != null) {
                doFuckery((ReceivedMessage) me.getMessage(), processMessage(me.getMessage()));
                super.onEvent(me);
            }
        } else if (event instanceof PrivateMessageReceivedEvent) {
            PrivateMessageReceivedEvent me = (PrivateMessageReceivedEvent) event;
            String newMessage = processMessage(me.getMessage());
            if (newMessage != null) {
                doFuckery((ReceivedMessage) me.getMessage(), processMessage(me.getMessage()));
                super.onEvent(me);
            } else {
                super.onEvent(me);
            }
        }
    }

    private String processMessage(Message message) {
        if (message.getContentDisplay().startsWith(fantasyPrefix)) {
            return message.getContentRaw().substring(fantasyLength);
        } else if (message.isMentioned(message.getJDA().getSelfUser()) && message.getContentRaw().startsWith("<@" + message.getJDA().getSelfUser().getId() + '>')) {
            return message.getContentRaw().substring(message.getJDA().getSelfUser().getId().length() + 3).trim();
        }
        return null;
    }

    private void doFuckery(ReceivedMessage message, String newContent) {
        // wheeeeeeeeeeeeeee reflection!
        // I'm a professional - don't try this at home, kids!

        Class<? extends ReceivedMessage> clazz = message.getClass();
        Field field;
        try {
            field = clazz.getDeclaredField("altContent");
            field.setAccessible(true);
            field.set(message, null);

            field = clazz.getDeclaredField("strippedContent");
            field.setAccessible(true);
            field.set(message, null);

            field = AbstractMessage.class.getDeclaredField("content");
            field.setAccessible(true);
            field.set(message, newContent);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Fuckery failed: " + e.toString());
            e.printStackTrace(System.err); // o no
        }
    }
}
