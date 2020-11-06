/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2018-2020 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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
 *   org.lizardirc.beancounter.Listeners
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.commands.dumpmessages;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import com.troidsonly.modbot.commands.log.MessageCache;
import com.troidsonly.modbot.utils.Miscellaneous;

public final class MessageHistoryDumper {
    private static final char UTF8_BOM = '\ufeff';

    private MessageHistoryDumper() {
        throw new IllegalStateException("MessageHistoryDumper class may not be instantiated.");
    }

    public static Path dumpMemberMessageHistory(User user, Guild guild, MessageCache messageCache) throws NoHistoryException, IOException {
        UserInfo userInfo = new UserInfo(user.getName() + '#' + user.getDiscriminator(),
            user.getId());

        List<TextChannel> channelList = Miscellaneous.asSortedList(guild.getTextChannels());
        List<ChannelHistory> channelHistories = new ArrayList<>();

        for (TextChannel channel : channelList) {
            List<MessageRecord> messageRecords = new ArrayList<>();
            List<Message> messages = messageCache.getMessagesByChannel(channel).stream()
                .filter(message -> message.getAuthor().equals(user))
                .collect(Collectors.toList());

            for (Message message : messages) {
                String fullMessage = Miscellaneous.getFullMessage(message).trim();

                if (!fullMessage.isEmpty()) {
                    messageRecords.add(new MessageRecord(message.getContentDisplay(), fullMessage, message.getTimeCreated(), message.getId()));
                }
            }

            if (!messageRecords.isEmpty()) {
                channelHistories.add(new ChannelHistory(channel.getName(), channel.getId(), Miscellaneous.asSortedList(messageRecords)));
            }
        }

        if (channelHistories.isEmpty()) {
            throw new NoHistoryException();
        }

        UserHistory userHistory = new UserHistory(userInfo, channelHistories);

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        String output = gson.toJson(userHistory, UserHistory.class);

        Path outputFile = Files.createTempFile("memberMessageHistory", ".json");

        try (PrintStream ps = new PrintStream(Files.newOutputStream(outputFile, StandardOpenOption.WRITE), false, "UTF-8")) {
            ps.print(UTF8_BOM);
            ps.println(output);
        }

        return outputFile;
    }
}
