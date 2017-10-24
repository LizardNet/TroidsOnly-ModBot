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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
}
