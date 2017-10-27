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
 *   org.lizardirc.beancounter.commands.sed.SedListener
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot.commands.filter;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// WARNING - This class has a natural ordering that is inconsistent with its equals()!
class RegexFilter implements Comparable<RegexFilter> {
    // There are several substring types that we match.
    // A) any character except \
    // B) 0 or more copies of either an escaped character, or something other than \
    // C) The delimiter, as a backreference
    // D) 0 or more characters a through z and A through Z, followed by a space
    // We match A. This is the delimiter.
    // We then match B. This is the regex.
    // We match C.
    // We match D. These are the options.
    private static final String REGEX_B = "((?:\\\\.|[^\\\\])*)";
    private static final String REGEX_D = "([A-Za-z]*)";
    private static final String REGEX_AB = "^([^\\\\\\sA-Za-z0-9])" + REGEX_B;
    private static final String REGEX_CD = "\\1" + REGEX_D;
    private static final String REGEX_VALID_FILTER = REGEX_AB + REGEX_CD;
    private static final Pattern PATTERN_OPTIONS = Pattern.compile("i*");

    static final Pattern PATTERN_VALID_FILTER = Pattern.compile(REGEX_VALID_FILTER);

    private String regex;
    private String creatorUid;
    private long creationTime; // Unix epoch seconds
    private Long expiry; // null means never expires
    private FilterAction action;
    private String comment;

    private transient Pattern pattern = null;

    public RegexFilter(String regex, String creatorUid, long creationTime, Long expiry, FilterAction action, String comment) {
        this.regex = Objects.requireNonNull(regex);
        this.creatorUid = Objects.requireNonNull(creatorUid);
        this.creationTime = creationTime;
        this.expiry = expiry;
        this.action = Objects.requireNonNull(action);
        this.comment = Objects.requireNonNull(comment);

        pattern = generatePattern(this.regex);
    }

    public String getCreatorUid() {
        return creatorUid;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getComment() {
        return comment;
    }

    public Pattern getPattern() {
        if (pattern == null) {
            pattern = generatePattern(regex);
        }

        return pattern;
    }

    public Long getExpiry() {
        return expiry;
    }

    public String getRegex() {
        return regex;
    }

    public FilterAction getAction() {
        return action;
    }

    private static Pattern generatePattern(String regex) {
        Matcher m = PATTERN_VALID_FILTER.matcher(regex);

        if (!m.find()) {
            throw new IllegalArgumentException("regex is not valid");
        }

        String actualRegexPart = m.group(2);
        String options = m.group(3);

        if (!PATTERN_OPTIONS.matcher(options).matches()) {
            throw new IllegalArgumentException("Invalid options '" + options + "'");
        }

        int flags = 0;
        if (options.contains("i")) {
            flags = Pattern.CASE_INSENSITIVE;
        }

        return Pattern.compile(actualRegexPart, flags);
    }

    public static boolean isValidRegex(String regex) {
        return PATTERN_VALID_FILTER.matcher(regex).find();
    }

    @Override
    public int compareTo(RegexFilter o) {
        Objects.requireNonNull(o);

        if (expiry == null) {
            if (o.expiry == null) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (o.expiry == null) {
                return -1;
            } else {
                return Long.compare(expiry, o.expiry);
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(regex);
    }

    public boolean canEqual(Object other) {
        return other instanceof RegexFilter;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof RegexFilter) {
            RegexFilter that = (RegexFilter) other;

            return that.canEqual(this)
                && regex.equals(that.regex);
        }

        return false;
    }
}
