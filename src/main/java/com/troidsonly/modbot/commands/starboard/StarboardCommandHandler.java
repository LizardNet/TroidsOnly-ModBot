/*
 * TROIDSONLY/MODBOT
 * By the Metroid Community Discord Server's Development Team (see AUTHORS.txt file)
 *
 * Copyright (C) 2023 by the Metroid Community Discord Server's Development Team. Some rights reserved.
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

package com.troidsonly.modbot.commands.starboard;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

import com.troidsonly.modbot.hooks.CommandHandler;
import com.troidsonly.modbot.ModBot;
import com.troidsonly.modbot.utils.Miscellaneous;
import com.troidsonly.modbot.utils.PotentialEmote;

class StarboardCommandHandler implements CommandHandler {

    private static final String CMD_STARBOARD = "starboard";
    private static final Set<String> COMMANDS = ImmutableSet.of(CMD_STARBOARD);

    private static final String STARBOARD_GLOBAL = "global";
    private static final String STARBOARD_CHANNEL = "channel";
    private static final String STARBOARD_EXCLUDE = "exclude";
    private static final Set<String> STARBOARD_SUB_COMMANDS = ImmutableSet.of(STARBOARD_GLOBAL, STARBOARD_CHANNEL,
            STARBOARD_EXCLUDE);

    // Sub-commands for STARBOARD_GLOBAL and STARBOARD_CHANNEL
    private static final String OP_GET = "get";
    private static final String OP_SET = "set";
    private static final String OP_DISABLE = "disable";
    private static final Set<String> PRIMARY_OPERATIONS = ImmutableSet.of(OP_GET, OP_SET, OP_DISABLE);

    // Sub-commands for STARBOARD_EXCLUDE
    private static final String OP_LIST = "list";
    private static final String OP_ADD = "add";
    private static final String OP_REMOVE = "remove";
    private static final Set<String> EXCLUDE_OPERATIONS = ImmutableSet.of(OP_LIST, OP_ADD, OP_REMOVE);

    private static final String PERM_STARBOARD_CONFIG = "starboard_config";

    private static final String HELP_USAGE = "Syntax:\n" +
            "For the server-wide (global) starboard:\n" +
            "- View settings: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_GET + "`\n" +
            "- Enable starboard or change settings: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_SET +
            " [#starboardChannel] [emote] [threshold]`\n" +
            "    - Where `#starboardChannel` is the channel promoted messages should be copied to\n" +
            "    - `emote` is the emoji or emote to track for the purposes of the starboard\n" +
            "    - and `threshold` is an integer greater than or equal to 1 representing the number of reactions of " +
            "`emote` needed for the message to be promoted to the starboard\n" +
            "- Disable starboard: `" + CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + ' ' + OP_DISABLE + "`\n\n" +
            "The Starboard can be enabled on a per-channel basis independent of the global setting. If the global " +
            "starboard is enabled, the per-channel setting (if enabled) overrides the global one. To set the per-" +
            "channel setting, use `" + CMD_STARBOARD + ' ' + STARBOARD_CHANNEL + " [#channelName]` instead of `" +
            CMD_STARBOARD + ' ' + STARBOARD_GLOBAL + "` for the above commands. Disabling the per-channel Starboard " +
            "will cause it to fall-back to using the global configuration.\n\n" +
            "If the global starboard is enabled, you can also specifically exclude channels from the starboard:\n" +
            "- List exclusions: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_LIST + "`\n" +
            "- Add exclusion: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_ADD + " [#channelName]`\n" +
            "- Remove exclusion: `" + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_REMOVE + " [#channelName]`\n" +
            "Excluding a channel will also disable any per-channel starboard for that channel.\n\n" +
            "All changes do **not** apply retroactively (e.g., disabling a starboard does not remove its messages " +
            "from the starboard).\n\n" +
            "(Hint: If you're getting this error for a command that requires a channel name, try specifying the full " +
            "channel name, including the leading `#`, and make sure the bot has permissions to see the channel.)";

    private final StarboardListener parent;

    public StarboardCommandHandler(StarboardListener parent) {
        this.parent = Objects.requireNonNull(parent);
    }

    @Override
    public Set<String> getSubCommands(GuildMessageReceivedEvent event, List<String> commands) {
        if (commands.isEmpty()) {
            return COMMANDS;
        }

        if (commands.size() == 1 && commands.get(0).equals(CMD_STARBOARD)) {
            return STARBOARD_SUB_COMMANDS;
        }

        if (commands.size() == 2 && commands.get(0).equals(CMD_STARBOARD)) {
            switch (commands.get(1)) {
                case STARBOARD_GLOBAL:
                    return PRIMARY_OPERATIONS;
                case STARBOARD_CHANNEL:
                    return Miscellaneous.getAllTextChannels(event);
                case STARBOARD_EXCLUDE:
                    return EXCLUDE_OPERATIONS;
            }
        }

        if (commands.size() == 3 && commands.get(0).equals(CMD_STARBOARD)) {
            switch (commands.get(1)) {
                case STARBOARD_CHANNEL:
                    return PRIMARY_OPERATIONS;
                case STARBOARD_EXCLUDE:
                    return Miscellaneous.getAllTextChannels(event);
            }
        }

        return Collections.emptySet();
    }

    @Override
    public void handleCommand(GuildMessageReceivedEvent event, List<String> commands, String remainder) {
        if (commands.isEmpty() || !commands.get(0).equals(CMD_STARBOARD)) {
            return;
        }

        if (!parent.getAcl().hasPermission(event.getMember(), PERM_STARBOARD_CONFIG)) {
            Miscellaneous.respond(event, ModBot.PERMFAIL_MESSAGE);
            return;
        }

        try {
            if (commands.size() <= 2) {
                sendErrorWithSyntaxHelp("Too few arguments", event);
                return;
            }

            if (commands.size() == 3) {
                switch (commands.get(1)) {
                    case STARBOARD_GLOBAL:
                        switch (commands.get(2)) {
                            case OP_GET:
                                printGlobalStarboardConfig(event);
                                return;
                            case OP_SET:
                                String[] args = remainder.split(" ");

                                if (args.length != 3) {
                                    sendErrorWithSyntaxHelp(
                                            "Incorrect number of arguments after `" + OP_SET + "`: Exactly 3 required",
                                            event);
                                    return;
                                }

                                setGlobalStarboardConfig(event, args);
                                return;
                            case OP_DISABLE:
                                disableGlobalStarboard(event);
                                return;
                        }
                        break;
                    case STARBOARD_CHANNEL:
                        sendErrorWithSyntaxHelp("Too few arguments", event);
                        return;
                    case STARBOARD_EXCLUDE:
                        switch (commands.get(2)) {
                            case OP_LIST:
                                printExclusions(event);
                                return;
                            case OP_ADD:
                            case OP_REMOVE:
                                sendErrorWithSyntaxHelp("Too few arguments", event);
                                return;
                        }
                        break;
                }
            }

            if (commands.size() == 4) {
                if (commands.get(1).equals(STARBOARD_CHANNEL)) {
                    String channelName = commands.get(2);

                    switch (commands.get(3)) {
                        case OP_GET:
                            printStarboardConfigForChannel(event, channelName);
                            return;
                        case OP_SET:
                            String[] args = remainder.split(" ");

                            if (args.length != 3) {
                                sendErrorWithSyntaxHelp(
                                        "Incorrect number of arguments after `" + OP_SET + "`: Exactly 3 required",
                                        event);
                                return;
                            }

                            setStarboardConfigForChannel(event, channelName, args);
                            return;
                        case OP_DISABLE:
                            disableStarboardForChannel(event, channelName);
                            return;
                    }
                } else if (commands.get(1).equals(STARBOARD_EXCLUDE)) {
                    String channelName = commands.get(3);

                    switch (commands.get(2)) {
                        case OP_LIST:
                            printExclusions(event);
                            return;
                        case OP_ADD:
                            addExclusion(event, channelName);
                            return;
                        case OP_REMOVE:
                            removeExclusion(event, channelName);
                            return;
                    }
                }
            }

            throw new RuntimeException("Undefined command behavior; this is a bug!");
        } catch (Exception e) {
            sendErrorWithSyntaxHelp("Failed to run the command: " + e, event);
            throw e;  // So it gets logged
        }
    }

    private static void sendErrorWithSyntaxHelp(String errorMessage, GuildMessageReceivedEvent event) {
        Objects.requireNonNull(errorMessage);

        String message = errorMessage + ". " + HELP_USAGE;
        Miscellaneous.respond(event, message);
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    private static String configToString(GuildMessageReceivedEvent event, StarboardConfig starboardConfig) {
        StringBuilder sb = new StringBuilder("Starboard is ");

        if (starboardConfig.isEnabled()) {
            TextChannel destinationChannel = event.getGuild()
                    .getTextChannelById(starboardConfig.getTargetStarboardChannelId().get());
            PotentialEmote pe = new PotentialEmote(event.getGuild(), starboardConfig.getStarboardEmoteId().get());

            if (destinationChannel == null) {
                throw new IllegalStateException(
                        "destinationChannel is null. This means that the Starboard is configured to point to a "
                                + "channel that no longer exists. Please reconfigure the Starboard."
                );
            }

            sb.append("**enabled**. Promoted messages are sent to #")
                    .append(destinationChannel.getName())
                    .append(" when they receive **")
                    .append(starboardConfig.getPromotionThreshold().getAsInt())
                    .append("** or more reactions with ");

            if (pe.getEmoteObject().isPresent()) {
                sb.append(pe.getEmoteObject().get().getAsMention());
            } else {
                sb.append(pe.getEmoteRaw());
            }
        } else {
            sb.append("**disabled**.");
        }

        return sb.toString();
    }

    private static StarboardConfig configFromStringArgs(GuildMessageReceivedEvent event, String[] args) {
        String starboardChannelName = args[0];
        String emoteStr = args[1];
        String thresholdStr = args[2];

        TextChannel starboardChannel = Miscellaneous.resolveTextChannelName(event, starboardChannelName);

        if (starboardChannel == null) {
            throw new IllegalArgumentException("Could not identify target Starboard channel");
        }

        PotentialEmote emote = new PotentialEmote(emoteStr, event.getGuild().getEmoteCache());
        int threshold = Integer.parseInt(thresholdStr);

        if (threshold < 1) {
            throw new IllegalArgumentException("threshold value must be >= 1");
        }

        return StarboardConfig.of(true, starboardChannel.getId(), emote.getEmoteRaw(), threshold);
    }

    /**
     * Print the global Starboard configuration.
     *
     * @param event The event the bot is responding to.
     */
    private void printGlobalStarboardConfig(GuildMessageReceivedEvent event) {
        StarboardConfig starboardConfig = parent.getState().getGlobalConfig();
        String starboardConfigStr = configToString(event, starboardConfig);

        String addendum =
                "\n\nRemember that this configuration may be overridden by channel-specific configuration. Use `"
                        + CMD_STARBOARD
                        + ' '
                        + STARBOARD_CHANNEL
                        + " [#channelName] "
                        + OP_GET
                        + "` to view the effective configuration for a given channel.";

        Miscellaneous.respond(event, "The global Starboard configuration is: " + starboardConfigStr + addendum);
    }

    /**
     * Sets the global starboard configuration to the given values.
     *
     * @param event The event the bot is responding to
     * @param args An array of Strings representing the values parsed out from the original command text.
     */
    private synchronized void setGlobalStarboardConfig(GuildMessageReceivedEvent event, String[] args) {
        StarboardConfig starboardConfig = configFromStringArgs(event, args);
        parent.getState().setGlobalConfig(starboardConfig);
        parent.sync();
        Miscellaneous.respond(event, "Successfully set the global Starboard configuration.");
    }

    private synchronized void disableGlobalStarboard(GuildMessageReceivedEvent event) {
        StarboardConfig starboardConfig = StarboardConfig.of(false, null, null, null);
        parent.getState().setGlobalConfig(starboardConfig);
        parent.sync();
        Miscellaneous.respond(event,
                "Successfully disabled the global Starboard. Remember that individual channels may have "
                        + "their own Starboards, and they remain enabled if so."
        );
    }

    private void printStarboardConfigForChannel(GuildMessageReceivedEvent event, String channelName) {
        TextChannel textChannel = Miscellaneous.resolveTextChannelName(event, channelName);

        if (textChannel == null) {
            throw new IllegalStateException("Could not resolve the specified channel name. This should never happen!");
        }

        StarboardConfig starboardConfig = parent.getState().getConfigForChannel(textChannel.getId());
        String starboardConfigStr = configToString(event, starboardConfig);

        StringBuilder response = new StringBuilder("The _effective_ configuration for #")
                .append(textChannel.getName())
                .append(" is: ")
                .append(starboardConfigStr)
                .append("\n\nReason: ");

        if (parent.getState().isChannelExcluded(textChannel.getId())) {
            response.append("This channel was explicitly excluded from the Starboard using the `")
                    .append(CMD_STARBOARD)
                    .append(' ')
                    .append(STARBOARD_EXCLUDE)
                    .append("` command.");
        } else if (parent.getState().channelHasOverride(textChannel.getId())) {
            response.append("This channel has a configuration that overrides the global configuration.");
        } else {
            response.append("This channel is falling back to the global configuration.");
        }

        Miscellaneous.respond(event, response.toString());
    }

    /**
     * Sets the per-channel Starboard configuration for the indicated channel to the given values.
     *
     * @param event The event the bot is responding to
     * @param channelName The name of the channel to apply the configuration to
     * @param args An array of Strings representing the values parsed out from the original command text.
     */
    private synchronized void setStarboardConfigForChannel(GuildMessageReceivedEvent event, String channelName,
            String[] args) {
        TextChannel channelToSet = Miscellaneous.resolveTextChannelName(event, channelName);
        if (channelToSet == null) {
            throw new IllegalStateException(
                    "Could not resolve channel to apply the configuration to. This should never happen!");
        }

        if (parent.getState().isChannelExcluded(channelToSet.getId())) {
            Miscellaneous.respond(event,
                    "Warning: This channel is excluded from the Starboard. Please remove the exclusion " +
                            "before proceeding:\n" + '`' + CMD_STARBOARD + ' ' + STARBOARD_EXCLUDE + ' ' + OP_REMOVE +
                            ' ' + channelName + "`."
            );
            return;
        }

        parent.getState().setPerChannelConfig(channelToSet.getId(), configFromStringArgs(event, args));
        parent.sync();
        Miscellaneous.respond(event, "Successfully set the Starboard configuration override for " + channelName + '.');
    }

    /**
     * Disables the per-channel Starboard configuration for the indicated channel.
     *
     * @param event The event the bot is responding to
     * @param channelName The name of the channel to disable the per-channel configuration in
     */
    private synchronized void disableStarboardForChannel(GuildMessageReceivedEvent event, String channelName) {
        TextChannel textChannel = Miscellaneous.resolveTextChannelName(event, channelName);

        if (textChannel == null) {
            throw new IllegalStateException("Could not resolve the specified channel name. This should never happen!");
        }

        parent.getState().setPerChannelConfig(textChannel.getId(), StarboardConfig.of(false, null, null, null));
        parent.sync();
        Miscellaneous.respond(event,
                "Successfully disabled the Starboard configuration override for " + channelName + '.');
    }

    private void printExclusions(GuildMessageReceivedEvent event) {

        String response = "These channels have been excluded from the Starboard: ";
        response += parent.getState().getExcludedChannelIds()
                .stream()
                .map(chId -> event.getGuild().getTextChannelById(chId))
                .filter(Objects::nonNull)
                .map(GuildChannel::getName)
                .map(name -> "#" + name)
                .collect(Collectors.joining(", "));

        Miscellaneous.respond(event, response);
    }

    private void addExclusion(GuildMessageReceivedEvent event, String channelName) {
        TextChannel textChannel = Miscellaneous.resolveTextChannelName(event, channelName);

        if (textChannel == null) {
            throw new IllegalStateException("Could not resolve the specified channel name. This should never happen!");
        }

        parent.getState().addChannelExclusion(textChannel.getId());
        parent.sync();
        Miscellaneous.respond(event, "Successfully added " + channelName + " to the Starboard exclusions list.");
    }

    private void removeExclusion(GuildMessageReceivedEvent event, String channelName) {
        TextChannel textChannel = Miscellaneous.resolveTextChannelName(event, channelName);

        if (textChannel == null) {
            throw new IllegalStateException("Could not resolve the specified channel name. This should never happen!");
        }

        parent.getState().removeChannelExclusion(textChannel.getId());
        parent.sync();
        Miscellaneous.respond(event, "Successfully removed " + channelName + " from the Starboard exclusions list.");
    }
}
