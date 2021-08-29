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
 */

package com.troidsonly.modbot.commands.reactforroles;

import java.util.Objects;
import java.util.Optional;

import net.dv8tion.jda.api.entities.Emote;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageReaction;
import net.dv8tion.jda.api.entities.MessageReaction.ReactionEmote;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import org.apache.commons.lang3.StringUtils;

public class PotentialEmote {

    private final Emote emoteObject;
    private final String emoteRaw;

    public PotentialEmote(String input, SnowflakeCacheView<Emote> emoteCache) {
        if (StringUtils.isEmpty(input)) {
            throw new IllegalArgumentException("input may not be null or empty");
        }

        Objects.requireNonNull(emoteCache);

        String emoteId = input.replaceAll(":", "");
        Optional<Emote> emote = emoteCache.stream()
                .filter(e -> e.getName().equalsIgnoreCase(emoteId))
                .findFirst();

        if (emote.isPresent()) {
            emoteRaw = emote.get().getId();
            emoteObject = emote.get();
        } else {
            emoteRaw = input;
            emoteObject = null;
        }
    }

    public PotentialEmote(MessageReaction messageReaction) {
        this(Objects.requireNonNull(messageReaction).getReactionEmote());
    }

    public PotentialEmote(ReactionEmote re) {
        Objects.requireNonNull(re);

        if (re.isEmoji()) {
            emoteObject = null;
            emoteRaw = re.getEmoji();
        } else {
            emoteObject = re.getEmote();
            emoteRaw = emoteObject.getId();
        }
    }

    public PotentialEmote(Guild guild, String emojiOrEmoteId) {
        Long emoteId;
        try {
            emoteId = Long.parseLong(emojiOrEmoteId);
        } catch (NumberFormatException e) {
            emoteId = null;
        }

        emoteRaw = emojiOrEmoteId;

        if (emoteId == null) {
            emoteObject = null;
        } else {
            emoteObject = guild.getEmoteById(emoteId);
        }
    }

    public Optional<Emote> getEmoteObject() {
        return Optional.ofNullable(emoteObject);
    }

    public String getEmoteRaw() {
        return emoteRaw;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(emoteRaw.toLowerCase());
    }

    protected boolean canEqual(Object other) {
        return other instanceof PotentialEmote;
    }

    @Override
    public boolean equals(Object other) {
        boolean result = false;

        if (other instanceof PotentialEmote) {
            PotentialEmote that = (PotentialEmote) other;

            result = that.canEqual(this)
                    && this.emoteRaw.equalsIgnoreCase(that.emoteRaw);
        }

        return result;
    }
}
