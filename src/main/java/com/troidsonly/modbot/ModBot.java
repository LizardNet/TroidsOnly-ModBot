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
 *   org.lizardirc.beancounter.Beancounter
 * Please review <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/AUTHORS.txt> for authorship information
 * and <https://github.com/LizardNet/LizardIRC-Beancounter/blob/master/LICENSE.txt> for licensing information.
 */

package com.troidsonly.modbot;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

public class ModBot {
    public static final String PROJECT_NAME = "TroidsOnly/ModBot";
    public static final String PERMFAIL_MESSAGE = "Adam does not authorize you to use this command."; // TODO: Make this configurable, or use a localization file system
    private final JDABuilder botBuilder;

    public ModBot(Properties properties) {
        String token = properties.getProperty("authToken");

        if (token == null) {
            System.err.println("Authentication token must be provided in configuration file.");
            System.exit(1);
        }

        Listeners listeners = new Listeners(properties, constructExecutorService());

        listeners.register();

        botBuilder = new JDABuilder(AccountType.BOT)
            .setToken(token);

        listeners.getAllListeners().forEach(botBuilder::addEventListener);
    }

    public void run() throws LoginException, InterruptedException, RateLimitedException {
        botBuilder.buildBlocking();
    }

    public static void main(String[] args) {
        Path configurationFile = Paths.get("config.props");

        // Expect 0 or 1 arguments.  If present, argument is the location of the startup configuration file to use
        if (args.length > 1) {
            System.err.println("Error: Too many arguments.");
            System.err.println("Usage: java -jar modbot.jar [configurationFile]");
            System.err.println("Where: configurationFile is the optional path to a startup configuration file.");
            System.exit(2);
        } else if (args.length == 1) {
            configurationFile = Paths.get(args[0]);
        }

        System.out.println("Reading configuration file " + configurationFile + "....");
        Properties properties = new Properties();
        try (InputStream is = Files.newInputStream(configurationFile)) {
            properties.load(is);
        } catch (NoSuchFileException e) {
            System.err.println("Error: Could not find configuration file " + configurationFile + " (NoSuchFileException). A default configuration file has been created for you at that location.");
            System.err.println("The bot will now terminate to give you an opportunity to edit the configuration file.");
            try (InputStream defaultConfig = ModBot.class.getResourceAsStream("/default.config.props")) {
                Files.copy(defaultConfig, configurationFile);
            } catch (IOException e1) {
                System.err.println("Error while writing out default configuration file.  Stack trace follows.");
                e1.printStackTrace();
            }
            System.exit(3);
        } catch (IOException e) {
            System.err.println("Error: Could not read configuration file " + configurationFile + ".  A stack trace follows.  The bot will now terminate.");
            e.printStackTrace();
            System.exit(3);
        }

        System.out.println("Constructing bot...");
        ModBot modBot = new ModBot(properties);

        System.out.println("Launching bot...");
        try {
            modBot.run();
        } catch (Exception e) {
            System.err.println("An error occured during startup: " + e.toString());
            e.printStackTrace(System.err);
            System.err.println("Unrecoverable error - shutting down.");
            System.exit(1);
        }
    }

    private ExecutorService constructExecutorService() {
        BasicThreadFactory factory = new BasicThreadFactory.Builder()
            .namingPattern("primaryExecutorPool-thread%d")
            .daemon(true)
            .build();
        ThreadPoolExecutor ret = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), factory);
        ret.allowCoreThreadTimeOut(true);
        return ret;
    }
}
