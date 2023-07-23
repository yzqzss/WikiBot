package dev.digitaldragon.commands;

import dev.digitaldragon.archive.RunJob;
import dev.digitaldragon.util.CommandTask;
import dev.digitaldragon.util.EnvConfig;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.UUID;

public class WikiteamArchiveCommand extends ListenerAdapter {
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("mediawikiarchive")) {
            return;
        }
        if (Boolean.parseBoolean(EnvConfig.getConfigs().get("disable_mediawiki_archive"))) {
            event.reply("This command is disabled. This can happen for a number of reasons:\n- You're accidentally using the testing bot, when you should be using the main one\n- There is an ongoing technical issue, and archiving had to be temporarily halted")
                    .setEphemeral(true).queue();
            return;
        }

        String options = parseOptions(event);
        //validate server is okay
        boolean isTest = Boolean.parseBoolean(EnvConfig.getConfigs().get("is_test"));
        if (isTest) {
            System.out.println(options);
            return;
        }
        Guild testServer = event.getJDA().getGuildById("349920496550281226");
        if (testServer == null) {
            event.reply("Something went wrong.").queue();
            return;
        }
        TextChannel channel = (TextChannel) testServer.getGuildChannelById("1112606638017368124");
        if (channel == null) {
            event.reply("Something went wrong.").queue();
            return;
        }

        // Single command execution
        if (Objects.equals(event.getSubcommandName(), "single")) {
            String url = Objects.requireNonNull(event.getOption("url")).getAsString();
            String note = Objects.requireNonNull(event.getOption("explain")).getAsString();

            //ensure URL is good
            try {
                URL uri = new URL(url);
            } catch (MalformedURLException e) {
                event.reply("Invalid URL.").setEphemeral(true).queue();
                return;
            }

            event.reply("Launching job for <" + url + ">").queue();
            startJob(channel, url, note, event.getUser(), options);

        }

        if (Objects.equals(event.getSubcommandName(), "bulk")) {
            event.reply("Sorry, this functionality isn't implemented yet").setEphemeral(true).queue();
        }
    }

    public void startJob(TextChannel channel, String url, String note, User user, String options) {
        String threadName;
        int maxLength = 100;
        if (url.length() <= maxLength) {
            threadName = url;
        } else {
            threadName = url.substring(0, maxLength - 3) + "...";
        }

        CommandTask makeFileTask = new CommandTask("echo " + url + " > wiki.txt", 1, "CreateFile"); //todo this is a hacky way to do this
        makeFileTask.setAlwaysSuccessful(true);

        //CommandTask downloadTask = new CommandTask("launcher wiki.txt " + options, 2, "DownloadMediaWiki");
        CommandTask downloadTask = new CommandTask("dumpgenerator " + options + url, 2, "DownloadMediaWiki");
        downloadTask.setSuccessCode(0);
        downloadTask.setAlwaysSuccessful(false);

        CommandTask integrityCheckTask = new CommandTask("grep -E '<title(.*?)>' *.xml -c;grep -E '<page(.*?)>' *.xml -c;grep \"</page>\" *.xml -c;grep -E '<revision(.*?)>' *.xml -c;grep \"</revision>\" *.xml -c", 3, "IntegrityCheck");
        integrityCheckTask.setAlwaysSuccessful(true); //todo this just prints the information out, it doesn't halt uploading if there's no integrity.

        CommandTask uploadTask = new CommandTask("uploader -kf ~/.doku_uploader_ia_keys wiki.txt", 4, "UploadMediaWiki");
        uploadTask.setSuccessCode(0);
        uploadTask.setAlwaysSuccessful(false);

        channel.createThreadChannel(threadName).setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_HOUR)
                .queue(thread -> {
                    String jobId = UUID.randomUUID().toString();
                    thread.sendMessage(String.format("Running job on <%s> with WikiTeam3 <https://github.com/mediawiki-client-tools/mediawiki-scraper> (for %s). `%s` ```%s``` \n Job ID: %s", url, user.getAsTag(), options, note, jobId)).queue(message -> message.pin().queue());
                    RunJob.startArchive(url, note, user, thread, jobId, makeFileTask, downloadTask, integrityCheckTask, uploadTask);
                });
    }

    public String parseOptions(SlashCommandInteractionEvent event) {
        StringBuilder options = new StringBuilder();
        /*processIntRangeOption(event, "delay", 0, 200, "--generator-arg='--delay", options); // trailing ' is appended by int range option
        processIntRangeOption(event, "retry", 0, 50, "--generator-arg='--retries", options);
        processIntRangeOption(event, "api_chunksize", 1, 500, "--generator-arg='--api_chunksize", options);


        processBooleanOption(event, "xml", "--generator-arg='--xml'", options, true);
        processBooleanOption(event, "images", "--generator-arg='--images'", options, true);
        processBooleanOption(event, "bypass_compression", "--generator-arg='--bypass-cdn-image-compression'", options);
        processBooleanOption(event, "xml_api_export", "--generator-arg='--xmlapiexport'", options);
        processBooleanOption(event, "xml_revisions", "--generator-arg='--xmlrevisions'", options);
        processBooleanOption(event, "current_only", "--generator-arg='--curonly'", options);*/

        processIntRangeOption(event, "delay", 0, 200, "--delay", options); // trailing ' is appended by int range option
        processIntRangeOption(event, "retry", 0, 50, "--retries", options);
        processIntRangeOption(event, "api_chunksize", 1, 500, "--api_chunksize", options);


        processBooleanOption(event, "xml", "--xml", options, true);
        processBooleanOption(event, "images", "--images", options, true);
        processBooleanOption(event, "bypass_compression", "--bypass-cdn-image-compression", options);
        processBooleanOption(event, "xml_api_export", "--xmlapiexport", options);
        processBooleanOption(event, "xml_revisions", "--xmlrevisions", options);
        processBooleanOption(event, "current_only", "--curonly", options);

        return options.toString();
    }

    private void processIntRangeOption(SlashCommandInteractionEvent event, String option, int min, int max, String command, StringBuilder options) {
        if (event.getOption(option) == null)
            return;

        Integer optionValue =  event.getOption(option).getAsInt();
        if (optionValue >= min && optionValue <= max) {
            //options.append(command).append(" ").append(optionValue).append("' ");
            options.append(command).append(" ").append(optionValue).append(" ");
        }
    }

    private void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options) {
        processBooleanOption(event, option, command, options, false);
    }


    private void processBooleanOption(SlashCommandInteractionEvent event, String option, String command, StringBuilder options, boolean defaultValue) {
        boolean optionValue = event.getOption(option) != null ? event.getOption(option).getAsBoolean() : defaultValue;
        if (optionValue) {
            options.append(command).append(" ");
        }
    }
}
