/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2017-2018 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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
 *   org.lizardirc.beancounter.commands.remind.ReminderCommandHandler
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.commands.filter;

import java.awt.Color;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.utils.Miscellaneous;

class FilterCommandHandler implements CommandHandler {
    static final String CMD_FILTER = "filter";
    static final Set<String> COMMANDS = ImmutableSet.of(CMD_FILTER);

    static final String SCMD_ADD = "add"; //Syntax: filter add [action] [regex] [expiry] [comment]
    static final String SCMD_REM = "remove"; //Syntax: filter remove [regex]
    static final String SCMD_LIST = "list"; //Syntax: filter list
    static final Set<String> SUBCOMMANDS = ImmutableSet.of(SCMD_ADD, SCMD_REM, SCMD_LIST);

    static final String PERM_FILTER = "filter";

    private static final String SYNTAX_HELP_1 = "```\n" + CMD_FILTER + ' ' + SCMD_ADD + " [action] [regex] [expiry] [comment]\n" +
        CMD_FILTER + ' ' + SCMD_REM + " [regex]\n" +
        CMD_FILTER + ' ' + SCMD_LIST + "\n```\n\n" +
        "Where [action] is one of these actions:\n" +
        "`log-only`: When the filter is tripped, only notify the mod log that it happened (all other actions below imply this one)\n" +
        "`warn-only`: When the filter is tripped, warn the user in a DM\n" +
        "`delete-silent`: When the filter is tripped, delete the message but don't tell the user who sent the message\n" +
        "`delete-and-warn`: When the filter is tripped, delete the message and warn the user what happened in a DM - all actions below this one also imply this\n" +
        "`delete-and-kick`: When the filter is tripped, kick the user from the server\n" +
        "`delete-and-cryo`: When the filter is tripped, cryo the user.  " +
        "Note that the cryo must be manually removed by a mod; the bot won't remove it automatically (yet, anyway)\n" +
        "`delete-and-ban`: When the filter is tripped, **ban** the user.  " +
        "This mode should only be used in exceptional circumstances, as the bot will issue a ban on the first offense!\n\n" +
        "It is *strongly recommended* that new filters be \"tested\" in `log-only` mode first, to avoid accidental actions.  Once a fiter has " +
        "been tested for a while and does what it's supposed to, it can be removed and re-added with a more restrictive mode.";
    private static final String SYNTAX_HELP_2 = "[regex] is a Perl-Compatible Regular Expression in the format `/regex/flags`.  The regex portion may contain spaces.  " +
        "The separator doesn't have to be `/`.\n\n" +
        "[expiry] is the time when the filter should remove itself, or 0 if it should remain until removed manually.  The value here can " +
        "either be the number of seconds until the filter removes itself, or a time specification in the format `1y2w3d4h5m6s`, meaning " +
        "\"one year, two weeks, three days, four hours, five minutes, and six seconds\".  All \"fields\" of a time specification in this format " +
        "are optional (e.g., 10d is equivalent to 0y0w10d0h0m0s).\n\n" +
        "[comment] is a required comment describing the filter; for actions where the user is warned, this comment is shown in the warning.";

    private final FilterListener parent;

    public FilterCommandHandler(FilterListener parent) {
        this.parent = parent;
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_FILTER)) {
            return SUBCOMMANDS;
        }

        if (commands.size() == 2 && commands.get(0).equals(CMD_FILTER) && commands.get(1).equals(SCMD_ADD)) {
            return ImmutableSet.copyOf(FilterAction.getFromStringMap().keySet());
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty()) {
            return;
        }

        if (parent.getAcl().hasPermission(event.getMember(), PERM_FILTER)) {
            if (commands.get(0).equals(CMD_FILTER) && commands.size() >= 2) {
                remainder = remainder.trim();

                switch (commands.get(1)) {
                    case SCMD_ADD:
                        if (commands.size() >= 3) {
                            FilterAction action = FilterAction.getFromStringMap().get(commands.get(2));
                            Matcher matcher = RegexFilter.PATTERN_VALID_FILTER.matcher(remainder);

                            if (matcher.find()) {
                                String regex = matcher.group();

                                remainder = remainder.substring(regex.length()).trim();

                                if (remainder.isEmpty()) {
                                    Miscellaneous.respond(event, "Too few arguments!  Syntax: `" + CMD_FILTER + ' ' +
                                        SCMD_ADD + " [action] [regex] [expiry] [comment]` - or just run the `" + CMD_FILTER +
                                        "` with no arguments to see full help.");
                                    return;
                                }

                                String args[] = remainder.split(" ");

                                Long expiry;
                                try {
                                    long expirySeconds = Long.parseLong(args[0]);

                                    if (expirySeconds == 0L) {
                                        expiry = null;
                                    } else {
                                        expiry = Instant.now().getEpochSecond() + expirySeconds;
                                    }
                                } catch (NumberFormatException e) {
                                    try {
                                        expiry = processTimeSpec(args[0]).toInstant().getEpochSecond();
                                    } catch (Exception e1) {
                                        Miscellaneous.respond(event, "Could not process time spec \"" + args[0] + "\": " + e.toString());
                                        return;
                                    }
                                }

                                remainder = remainder.substring(args[0].length()).trim();

                                if (remainder.isEmpty()) {
                                    Miscellaneous.respond(event, "Too few arguments!  Syntax: `" + CMD_FILTER + ' ' +
                                        SCMD_ADD + " [action] [regex] [expiry] [comment]` - or just run the `" + CMD_FILTER +
                                        "` with no arguments to see full help.");
                                    return;
                                }

                                RegexFilter newFilter;

                                try {
                                    newFilter = new RegexFilter(regex, event.getAuthor().getId(), Instant.now().getEpochSecond(), expiry, action, remainder);
                                } catch (Exception e) {
                                    Miscellaneous.respond(event, "Failed to construct filter: " + e.toString());
                                    return;
                                }

                                synchronized (parent.getFilterRepository()) {
                                    List<RegexFilter> filterList = parent.getFilterRepository().getFilterList();

                                    if (filterList.contains(newFilter)) {
                                        Miscellaneous.respond(event, "I already have a filter with that regular expression.  If you wish to change a filter's settings, please delete then re-add it.");
                                        return;
                                    }

                                    EmbedBuilder embedBuilder = new EmbedBuilder();
                                    embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getMember().getUser().getAvatarUrl());
                                    embedBuilder.setTitle("Added a filter");
                                    embedBuilder.setDescription('`' + newFilter.getRegex() + '`');
                                    embedBuilder.addField("With comment", newFilter.getComment(), false);
                                    embedBuilder.addField("Added at", Miscellaneous.unixEpochToRfc1123DateTimeString(newFilter.getCreationTime()), false);
                                    embedBuilder.addField("To expire", expiry == null ? "Never" : "at " + Miscellaneous.unixEpochToRfc1123DateTimeString(newFilter.getExpiry()), false);
                                    embedBuilder.addField("Performing action", newFilter.getAction().toString(), false);
                                    embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
                                    embedBuilder.setColor(new Color(0xFF8800));

                                    parent.getLogger().sendToLog(embedBuilder.build(), event.getMember());

                                    filterList.add(newFilter);
                                    parent.sync();
                                }
                                Miscellaneous.respond(event, "Filter added!");
                            } else {
                                Miscellaneous.respond(event, "Did not detect a valid regex specification.\n" +
                                    "Syntax: `" + CMD_FILTER + ' ' + SCMD_ADD + " [action] [regex] [expiry] [comment]` - or just run the `" + CMD_FILTER + "` with no arguments to see full help.");
                            }
                        } else {
                            Miscellaneous.respond(event, "Unrecognized action - action must be one of: `" + Miscellaneous.getStringRepresentation(FilterAction.getFromStringMap().keySet(), "`, `") + "`\n" +
                                "Syntax: `" + CMD_FILTER + ' ' + SCMD_ADD + " [action] [regex] [expiry] [comment]`");
                        }
                        break;
                    case SCMD_REM:
                        if (remainder.isEmpty()) {
                            Miscellaneous.respond(event, "You need to tell me what filter to delete!  Syntax: `" + CMD_FILTER + ' ' + SCMD_REM + " [regex]`");
                        } else if (!RegexFilter.isValidRegex(remainder)) {
                            Miscellaneous.respond(event, '`' + remainder + "` doesn't seem to be a valid regex specification.");
                        } else {
                            RegexFilter temp = new RegexFilter(remainder, "", 0, null, FilterAction.LOG_ONLY, "");
                            synchronized (parent.getFilterRepository()) {
                                List<RegexFilter> filterList = parent.getFilterRepository().getFilterList();
                                int index = filterList.indexOf(temp);

                                if (index != -1) {
                                    RegexFilter oldFilter = filterList.get(index);
                                    filterList.remove(index);

                                    EmbedBuilder embedBuilder = new EmbedBuilder();
                                    embedBuilder.setAuthor(Miscellaneous.qualifyName(event.getMember()), null, event.getMember().getUser().getAvatarUrl());
                                    embedBuilder.setTitle("Filter removed");
                                    embedBuilder.setDescription('`' + remainder + '`');
                                    embedBuilder.addField("Performing action", oldFilter.getAction().toString(), false);
                                    embedBuilder.addField("Originally added by", event.getGuild().getMemberById(oldFilter.getCreatorUid()).getEffectiveName(), false);
                                    embedBuilder.addField("Originally added at", Miscellaneous.unixEpochToRfc1123DateTimeString(oldFilter.getCreationTime()), false);
                                    embedBuilder.addField("Original comment", oldFilter.getComment(), false);
                                    embedBuilder.setFooter(getClass().getSimpleName() + " | " + Miscellaneous.unixEpochToRfc1123DateTimeString(Instant.now().getEpochSecond()), null);
                                    embedBuilder.setColor(new Color(0xFF8800));

                                    parent.getLogger().sendToLog(embedBuilder.build(), event.getMember());

                                    Miscellaneous.respond(event, "Filter removed");
                                    parent.sync();
                                } else {
                                    Miscellaneous.respond(event, "Could not find a matching filter to be removed.");
                                }
                            }
                        }
                        break;
                    case SCMD_LIST:
                        synchronized (parent.getFilterRepository()) {
                            List<RegexFilter> filters = parent.getFilterRepository().getFilterList();

                            StringBuilder output = new StringBuilder("I have the following filters saved:\n");

                            filters.stream()
                                .map(filter -> regexFilterToString(event, filter))
                                .forEach(filter -> output.append("* ").append(filter).append("\n"));

                            event.getChannel().sendMessage(output.toString()).queue();
                        }
                        break;
                }
            } else {
                Miscellaneous.respond(event, "Not enough arguments for this command.  Syntax:\n" + SYNTAX_HELP_1);
                Miscellaneous.respond(event, SYNTAX_HELP_2);
            }
        } else {
            Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
        }
    }

    private String regexFilterToString(GuildMessageReceivedEvent event, RegexFilter filter) {
        Objects.requireNonNull(filter);

        StringBuilder sb = new StringBuilder("Regex: `")
            .append(filter.getRegex())
            .append("`; Action: ")
            .append(filter.getAction().toString())
            .append("; created by ")
            .append(event.getGuild().getMemberById(filter.getCreatorUid()).getEffectiveName())
            .append(" at ")
            .append(Miscellaneous.unixEpochToRfc1123DateTimeString(filter.getCreationTime()))
            .append("; ");

        if (filter.getExpiry() == null) {
            sb.append("never expires; ");
        } else {
            sb.append("expires ")
                .append(Miscellaneous.unixEpochToRfc1123DateTimeString(filter.getExpiry()))
                .append("; ");
        }

        sb.append("Comment: `")
            .append(filter.getComment())
            .append('`');

        return sb.toString();
    }

    private ZonedDateTime processTimeSpec(String timespec) throws IllegalArgumentException {
        PeriodFormatter formatter = new PeriodFormatterBuilder()
            .appendYears().appendSuffix("y")
            .appendWeeks().appendSuffix("w")
            .appendDays().appendSuffix("d")
            .appendHours().appendSuffix("h")
            .appendMinutes().appendSuffix("m")
            .appendSeconds().appendSuffix("s")
            .toFormatter();

        ZonedDateTime retval = ZonedDateTime.now().plusSeconds(formatter.parsePeriod(timespec).toDurationFrom(org.joda.time.Instant.now()).getStandardSeconds());
        if (retval.getYear() > 9999) {
            throw new IllegalArgumentException("Specified time is too far in the future (past year 9999). Go away.");
        }
        return retval;
    }

}
