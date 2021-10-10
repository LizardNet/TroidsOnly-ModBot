/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2021 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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
 * The code in this file is modified from the following class(es) in the JDA project:
 *   net.dv8tion.jda.api.entities.MessageHistory
 * Please review <https://github.com/DV8FromTheWorld/JDA/blob/development/LICENSE> for licensing information.
 */

package com.troidsonly.modbot.commands.log;

import java.util.LinkedList;
import java.util.List;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.internal.JDAImpl;
import net.dv8tion.jda.internal.entities.EntityBuilder;
import net.dv8tion.jda.internal.requests.RestActionImpl;
import net.dv8tion.jda.internal.requests.Route.CompiledRoute;
import net.dv8tion.jda.internal.requests.Route.Messages;

public class WorkaroundMessageHistory extends MessageHistory {

    public WorkaroundMessageHistory(MessageChannel channel) {
        super(channel);
    }

    @Override
    public RestAction<List<Message>> retrievePast(int amount) {
        if (amount <= 100 && amount >= 1) {
            CompiledRoute route = Messages.GET_MESSAGE_HISTORY.compile(new String[]{this.channel.getId()})
                    .withQueryParams(new String[]{"limit", Integer.toString(amount)});
            if (!this.history.isEmpty()) {
                route = route.withQueryParams(new String[]{"before", String.valueOf(this.history.lastKey())});
            }

            JDAImpl jda = (JDAImpl) this.getJDA();
            return new RestActionImpl<>(jda, route, (response, request) -> {
                EntityBuilder builder = jda.getEntityBuilder();
                LinkedList<Message> messages = new LinkedList<>();
                DataArray historyJson = response.getArray();

                for (int i = 0; i < historyJson.length(); ++i) {
                    try {
                        messages.add(builder.createMessage(historyJson.getObject(i)));
                    } catch (IllegalArgumentException e) {
                        //noinspection UnnecessaryContinue
                        continue;
                    }
                }

                messages.forEach((msg) -> {
                    this.history.put(msg.getIdLong(), msg);
                });
                return messages;
            });
        } else {
            throw new IllegalArgumentException(
                    "Message retrieval limit is between 1 and 100 messages. No more, no less. Limit provided: "
                            + amount);
        }
    }
}
