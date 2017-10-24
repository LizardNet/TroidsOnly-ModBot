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

package com.troidsonly.modbot.commands.admin;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.entities.Icon;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.security.AccessControl;
import com.troidsonly.modbot.utils.Miscellaneous;

public class AdminHandler implements CommandHandler {
    public static final String E_PERMFAIL = "No u!  (You don't have permission to do this.)";

    private static final String CMD_QUIT = "quit";
    private static final String CMD_CHGNICK = "chgnick";
    private static final String CMD_USERNAME = "chgusername";
    private static final String CMD_SAY = "say";
    private static final String CMD_SETAVATAR = "setavatar";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_QUIT, CMD_CHGNICK, CMD_USERNAME, CMD_SAY, CMD_SETAVATAR);

    private static final String AVATAR_DEFAULT = "default";
    private static final Set<String> SETAVATAR_SUBCMDS = ImmutableSet.of(AVATAR_DEFAULT);

    private final AccessControl acl;

    public AdminHandler(AccessControl acl) {
        this.acl = acl;
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
                if (acl.hasPermission(event.getMember(), "quit")) {
                    event.getMessage().getChannel().sendMessage("tear in salami").complete();
                    event.getJDA().shutdown();
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_CHGNICK:
                if (acl.hasPermission(event.getMember(), "nick")) {
                    if (remainder.isEmpty()) {
                        Miscellaneous.respond(event, "You need to tell me what to change my nickname to!");
                    } else {
                        event.getGuild().getController().setNickname(event.getGuild().getMember(event.getJDA().getSelfUser()), remainder).complete();
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_USERNAME:
                if (acl.hasPermission(event.getMember(), "usercon")) {
                    if (remainder.isEmpty()) {
                        Miscellaneous.respond(event, "You need to tell me what to change my username to!");
                    } else {
                        Miscellaneous.completeActionWithErrorHandling(event, event.getJDA().getSelfUser().getManager().setName(remainder));
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
            case CMD_SAY:
                if (acl.hasPermission(event.getMember(), "say")) {
                    if (!remainder.isEmpty()) {
                        String[] args = remainder.split(" ");

                        if (Miscellaneous.isChannelLike(args[0])) {
                            if (args.length < 2) {
                                Miscellaneous.respond(event, "Error: Too few arguments. Syntax: say [#channel] [message]");
                            } else {
                                remainder = remainder.substring(args[0].length()).trim();
                            }

                            List<TextChannel> channels = event.getGuild().getTextChannelsByName(args[0].substring(1), true);

                            if (channels.isEmpty()) {
                                Miscellaneous.respond(event, "No channels matched the name " + args[0]);
                            } else if (channels.size() > 1) {
                                Miscellaneous.respond(event, "Multiple channels matched the name " + args[0]);
                            } else {
                                Miscellaneous.completeActionWithErrorHandling(event, channels.get(0).sendMessage(remainder));
                            }
                        } else {
                            Miscellaneous.completeActionWithErrorHandling(event, event.getChannel().sendMessage(remainder));
                        }
                    } else {
                        Miscellaneous.respond(event, "Error: You need to give me a message to send.  Syntax: say {[#channel]} [message]");
                    }
                } else {
                    // take that, smartass!
                    String actor = '@' + event.getMember().getEffectiveName();
                    if (E_PERMFAIL.equals(remainder) || (actor + ' ' + E_PERMFAIL).equals(remainder)) {
                        event.getChannel().sendMessage("*throws sand in " + event.getMember().getAsMention() + "'s face*").complete();
                    } else {
                        Miscellaneous.respond(event, E_PERMFAIL);
                    }
                }
                break;
            case CMD_SETAVATAR:
                if (acl.hasPermission(event.getMember(), "usercon")) {
                    if (commands.size() == 2 && commands.get(1).equals(AVATAR_DEFAULT)) {
                        if (Miscellaneous.completeActionWithErrorHandling(event, event.getJDA().getSelfUser().getManager().setAvatar(null))) {
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

                        if (Miscellaneous.completeActionWithErrorHandling(event, event.getJDA().getSelfUser().getManager().setAvatar(icon))) {
                            Miscellaneous.respond(event, "Done!");
                        }
                    }
                } else {
                    Miscellaneous.respond(event, E_PERMFAIL);
                }
                break;
        }
    }
}
