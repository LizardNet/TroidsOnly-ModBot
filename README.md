# TroidsOnly/ModBot

This project is a Java 8 Discord bot intended to assist in moderation tasks on the Metroid Community Discord server,
as well as provide some fun functions on the side for regular users.

## Building
To build the bot from source, you'll need the Java SE 8 JDK and Apache Maven (version 3.0.0 or later recommended).

Simply clone the repository, run `mvn package`, and run the resulting jarfile!  The bot will automatically create a
default configuration file for you then terminate, so you have a chance to set things up; then just run the bot again!

Note that due to recent changes to the bot's shading configuration, it is no longer compatible with being directly run
in most IDEs (for example, using the "_Application_" configuration in IntelliJ IDEA no longer works). Instead, if you
are running the bot within an IDE, you will need to use Maven to build the jarfile and run the jarfile.

More information about running the bot may be put here, eventually.

## Contributing
As an open source project, you are welcome to contribute to the bot's development!

### Bug reports/feature requests/other issues
If you would like to file a bug report, request a feature, or report some other issue, please use the [Issues section
of our GitHub repository][github-issues].

### Code
If you would like to contribute code, there are a couple different ways you could go about doing this.

* If you are familiar with Gerrit Code Review, you can use [LizardNet Code Review][lizardnet-code-review] to clone
  the code, and submit patches directly to us that way.  Note though that you'll need a LizardWiki account to log in to
  LizardNet Code Review; [this page][lizardnet-code-review-login] has more information on that (if you don't have a
  LizardWiki account, you can easily request one be created for you).
* Alternatively, just clone the [GitHub repository][github] and submit a pull request.  Note, though, that the GitHub
  repository is just a read-only mirror of the [LizardNet Code Review repository][lizardnet-repository], so all pull
  requests will be copied to Gerrit by a developer for you before merging into the mainline.

## Licensing/Authors
TroidsOnly/ModBot is licensed under the GNU GPL v3+.  For more information, please see the LICENSE.txt file.  For
authors information, please see the AUTHORS.txt file.

[github]: https://github.com/LizardNet/TroidsOnly-ModBot
[github-issues]: https://github.com/LizardNet/TroidsOnly-ModBot/issues
[lizardnet-code-review]: https://gerrit.fastlizard4.org
[lizardnet-repository]: https://git.fastlizard4.org/gitblit/summary/?r=TroidsOnly/ModBot.git
[lizardnet-code-review-login]: https://fastlizard4.org/wiki/LizardNet_Code_Review
