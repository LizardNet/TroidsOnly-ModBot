/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2023 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.admin;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.utils.Miscellaneous;

class AdminHandler implements CommandHandler {

    public static final String E_PERMFAIL = ModBot.PERMFAIL_MESSAGE;

    private static final String CMD_QUIT = "quit";
    private static final String CMD_CHGNICK = "setnick";
    private static final String CMD_REMOVENICK = "removenick";
    private static final String CMD_USERNAME = "setusername";
    private static final String CMD_SAY = "say";
    private static final String CMD_SETAVATAR = "setavatar";
    private static final String CMD_SETPLAYING = "setplaying";
    private static final String CMD_REMOVEPLAYING = "removeplaying";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_QUIT, CMD_CHGNICK, CMD_REMOVENICK, CMD_USERNAME,
            CMD_SAY, CMD_SETAVATAR, CMD_SETPLAYING, CMD_REMOVEPLAYING);

    private static final String AVATAR_DEFAULT = "default";
    private static final Set<String> SETAVATAR_SUBCMDS = ImmutableSet.of(AVATAR_DEFAULT);

    private static final String REGEX_DESPOOF_STRING = "[^\\w]+";
    private static final Pattern PATTERN_DESPOOF_STRING = Pattern.compile(REGEX_DESPOOF_STRING);

    private final AdminListener parent;

    public AdminHandler(AdminListener parent) {
        this.parent = parent;
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_SETAVATAR)) {
            return SETAVATAR_SUBCMDS;
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        remainder = remainder.trim();

        switch (commands.get(0)) {
            case CMD_QUIT:
                if (parent.getAcl().hasPermission(event.getMember(), "quit")) {
                    event.getMessage().getChannel().sendMessage("tear in salami").complete();
                    event.getJDA().shutdown();
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_CHGNICK:
                if (parent.getAcl().hasPermission(event.getMember(), "nick")) {
                    if (remainder.isEmpty()) {
                        Miscellaneous.respond(event, "You need to tell me what to change my nickname to!");
                    } else {
                        if (Miscellaneous.completeActionWithErrorHandling(event,
                                event.getGuild().modifyNickname(event.getGuild().getSelfMember(), remainder))) {
                            Miscellaneous.respond(event, "Changed my nickname!");
                        }
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_REMOVENICK:
                if (parent.getAcl().hasPermission(event.getMember(), "nick")) {
                    if (Miscellaneous.completeActionWithErrorHandling(event,
                            event.getGuild().modifyNickname(event.getGuild().getSelfMember(), null))) {
                        Miscellaneous.respond(event, "Removed my nickname.");
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_USERNAME:
                if (parent.getAcl().hasPermission(event.getMember(), "usercon")) {
                    if (remainder.isEmpty()) {
                        Miscellaneous.respond(event, "You need to tell me what to change my username to!");
                    } else {
                        if (Miscellaneous.completeActionWithErrorHandling(event,
                                event.getJDA().getSelfUser().getManager().setName(remainder))) {
                            Miscellaneous.respond(event, "Changed my username!");
                        }
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_SAY:
                if (parent.getAcl().hasPermission(event.getMember(), "say")) {
                    if (!remainder.isEmpty()) {
                        String[] args = remainder.split(" ");
                        String message;

                        if (Miscellaneous.isChannelLike(args[0])) {
                            if (args.length < 2) {
                                Miscellaneous.respond(event,
                                        "Error: Too few arguments. Syntax: say [#channel] [message]");
                                return;
                            }

                            TextChannel targetChannel = Miscellaneous.resolveTextChannelName(event, args[0]);
                            if (targetChannel != null) {
                                message = event.getMessage().getContentRaw()
                                        .substring(commands.get(0).length() + targetChannel.getAsMention().length() + 1)
                                        .trim();
                                Miscellaneous.completeActionWithErrorHandling(event,
                                        targetChannel.sendMessage(message));
                            }
                        } else {
                            message = event.getMessage().getContentRaw().substring(commands.get(0).length()).trim();
                            Miscellaneous.completeActionWithErrorHandling(event,
                                    event.getChannel().sendMessage(message));
                        }
                    } else {
                        Miscellaneous.respond(event,
                                "Error: You need to give me a message to send.  Syntax: say {[#channel]} [message]");
                    }
                } else {
                    // take that, smartass!

                    String actor;
                    if (event.getMember() != null) {
                        actor = '@' + event.getMember().getEffectiveName();
                    } else {
                        actor = "";
                    }
                    String remainderDespoofed = PATTERN_DESPOOF_STRING.matcher(remainder).replaceAll("");

                    if (PATTERN_DESPOOF_STRING.matcher(E_PERMFAIL).replaceAll("").equals(remainderDespoofed)
                            || PATTERN_DESPOOF_STRING.matcher(actor + ' ' + E_PERMFAIL).replaceAll("")
                            .equals(remainderDespoofed)) {
                        event.getChannel()
                                .sendMessage("*throws sand in " + event.getMember().getAsMention() + "'s face*")
                                .complete();
                    } else {
                        Miscellaneous.respond(event, E_PERMFAIL);
                    }
                }
                break;
            case CMD_SETAVATAR:
                if (parent.getAcl().hasPermission(event.getMember(), "usercon")) {
                    if (commands.size() == 2 && commands.get(1).equals(AVATAR_DEFAULT)) {
                        if (Miscellaneous.completeActionWithErrorHandling(event,
                                event.getJDA().getSelfUser().getManager().setAvatar(null))) {
                            Miscellaneous.respond(event, "Avatar reset to Discord default");
                        }
                    } else {
                        URL url;

                        try {
                            url = new URL(remainder);
                        } catch (Exception e) {
                            Miscellaneous.respond(event, "Error: Invalid URL.");
                            return;
                        }

                        Icon icon;

                        try (InputStream is = Miscellaneous.getHttpInputStream(url)) {
                            icon = Icon.from(is);
                        } catch (Exception e) {
                            Miscellaneous.respond(event, "Unable to comply due to an exception: " + e.toString());
                            Miscellaneous.respond(event, "My console will have more information about this error.");
                            e.printStackTrace(System.err);
                            return;
                        }

                        if (Miscellaneous.completeActionWithErrorHandling(event,
                                event.getJDA().getSelfUser().getManager().setAvatar(icon))) {
                            Miscellaneous.respond(event, "Done!");
                        }
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_SETPLAYING:
                if (parent.getAcl().hasPermission(event.getMember(), "usercon")) {
                    if (remainder.isEmpty()) {
                        Miscellaneous.respond(event, "You need to tell me what I'm playing!");
                    } else {
                        event.getJDA().getPresence().setActivity(Activity.playing(remainder));
                        parent.persistNowPlaying(remainder);
                        Miscellaneous.respond(event, "Now playing information set!");
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_REMOVEPLAYING:
                if (parent.getAcl().hasPermission(event.getMember(), "usercon")) {
                    event.getJDA().getPresence().setActivity(null);
                    parent.persistNowPlaying(null);
                    Miscellaneous.respond(event, "Now playing information removed.");
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
        }
    }
}
