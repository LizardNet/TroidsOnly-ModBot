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
 */

package com.troidsonly.modbot.commands.dumpmessages;

import java.time.OffsetDateTime;
import java.util.Objects;

import com.troidsonly.modbot.utils.Miscellaneous;

public class MessageRecord implements Comparable<MessageRecord> {
    private final String messageText;
    private final String rawMessageText;
    private final String rfc1123Timestamp;
    private final long unixEpochTimestamp;
    private final String messageId;

    public MessageRecord(String messageText, String rawMessageText, OffsetDateTime messageTime, String messageId) {
        this.messageText = Objects.requireNonNull(messageText);
        this.rawMessageText = Objects.requireNonNull(rawMessageText);
        unixEpochTimestamp = Objects.requireNonNull(messageTime).toEpochSecond();
        rfc1123Timestamp = Miscellaneous.unixEpochToRfc1123DateTimeString(unixEpochTimestamp);
        this.messageId = Objects.requireNonNull(messageId);
    }

    public String getMessageText() {
        return messageText;
    }

    public String getRawMessageText() {
        return rawMessageText;
    }

    public String getRfc1123Timestamp() {
        return rfc1123Timestamp;
    }

    public long getUnixEpochTimestamp() {
        return unixEpochTimestamp;
    }

    public String getMessageId() {
        return messageId;
    }

    @Override
    public int compareTo(MessageRecord o) {
        return Long.compare(this.unixEpochTimestamp, o.unixEpochTimestamp);
    }
}
