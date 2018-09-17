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
 *   org.lizardirc.beancounter.utils.Miscellaneous
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.events.guild.GenericGuildEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.requests.RestAction;

import com.troidsonly.modbot.ModBot;

public final class Miscellaneous {
    private Miscellaneous() {
        throw new IllegalStateException("This class may not be instantiated.");
    }

    public static String getBotNickname(GenericGuildEvent event) {
        return event.getGuild().getMember(event.getJDA().getSelfUser()).getNickname();
    }

    public static String getBotUsername(Event event) {
        return event.getJDA().getSelfUser().getName();
    }

    public static String getBotEffectiveName(GenericGuildEvent event) {
        return event.getGuild().getMember(event.getJDA().getSelfUser()).getEffectiveName();
    }

    public static String getStringRepresentation(Collection<String> set) {
        return getStringRepresentation(set, ", ");
    }

    public static String getStringRepresentation(Collection<String> set, String separator) {
        if (set.isEmpty()) {
            return "(none)";
        } else {
            return set.stream().collect(Collectors.joining(separator));
        }
    }

    public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<>(c);
        Collections.sort(list);
        return list;
    }

    public static void respond(GuildMessageReceivedEvent event, String message) {
        event.getChannel().sendMessage(event.getMember().getAsMention() + ' ' + message).complete();
    }

    public static boolean isChannelLike(String arg) {
        // Determine if the argument string appears to be a text channel - i.e., does it start with #?
        return arg.startsWith("#");
    }

    public static String generateHttpUserAgent() {
        String artifactVersion = ModBot.class.getPackage().getImplementationVersion();
        if (artifactVersion == null) {
            artifactVersion = "";
        } else {
            artifactVersion = '/' + artifactVersion;
        }

        String projectName = ModBot.PROJECT_NAME;
        projectName = projectName.replace('/', '-');

        return projectName + artifactVersion + " (compatible; +https://www.troidsonly.com)";
    }

    public static InputStream getHttpInputStream(URL url) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestProperty("User-Agent", generateHttpUserAgent());
            httpURLConnection.connect();

            if (httpURLConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException("Error making HTTP request: Got return status " + httpURLConnection.getResponseCode());
            }

            return httpURLConnection.getInputStream();
    }

    public static boolean completeActionWithErrorHandling(GuildMessageReceivedEvent event, RestAction<?> action) {
        try {
            action.complete(false);
            return true;
        } catch (Exception e) {
            Miscellaneous.respond(event, ":frowning: Unable to comply due to an exception: " + e.toString());
            Miscellaneous.respond(event, "My console will have more information about this error.");
            e.printStackTrace(System.err);
            return false;
        }
    }

    public static TextChannel resolveTextChannelName(GuildMessageReceivedEvent event, String channelName) {
        if (!isChannelLike(channelName)) {
            throw new IllegalArgumentException("channelName does not appear to be a valid channel name");
        }

        List<TextChannel> channels = event.getGuild().getTextChannelsByName(channelName.substring(1), true);

        if (channels.isEmpty()) {
            Miscellaneous.respond(event, "No channels matched the name " + channelName);
            return null;
        } else if (channels.size() > 1) {
            Miscellaneous.respond(event, "Multiple channels matched the name " + channelName);
            return null;
        } else {
            return channels.get(0);
        }
    }

    public static String qualifyName(Member member) {
        StringBuilder sb = new StringBuilder(member.getUser().getName())
            .append('#')
            .append(member.getUser().getDiscriminator());

        if (member.getNickname() != null) {
            sb.append(" / Nickname: ")
                .append(member.getNickname());
        }

        sb.append(" / UID: ")
            .append(member.getUser().getId());

        return sb.toString();
    }

    public static String qualifyName(User user) {
        StringBuilder sb = new StringBuilder(user.getName())
            .append('#')
            .append(user.getDiscriminator());

        sb.append(" / UID: ")
            .append(user.getId());

        return sb.toString();
    }

    public static String getFullMessage(Message message) {
        StringBuilder output = new StringBuilder(message.getRawContent());

        if (message.getAttachments().size() >= 1) {
            for (Message.Attachment attachment : message.getAttachments()) {
                output.append(" ")
                    .append(attachment.getUrl());
            }
        }

        return output.toString();
    }

    public static String unixEpochToRfc1123DateTimeString(long epochSeconds) {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static Set<String> getAllRoles(GuildMessageReceivedEvent event, boolean preventNotification) {
        return event.getGuild().getRoles().stream()
            .map(Role::getName)
            .map(role -> {
                if (preventNotification && "@everyone".equals(role)) {
                    role = "everyone";
                }

                return role;
            })
            .collect(Collectors.toSet());
    }
}
