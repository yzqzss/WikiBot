package dev.digitaldragon.interfaces.telegram;

import dev.digitaldragon.interfaces.api.UpdatesWebsocket;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.events.JobAbortEvent;
import dev.digitaldragon.jobs.events.JobFailureEvent;
import dev.digitaldragon.jobs.events.JobQueuedEvent;
import dev.digitaldragon.jobs.events.JobSuccessEvent;
import dev.digitaldragon.util.EnvConfig;
import net.badbird5907.lightning.annotation.EventHandler;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

public class TelegramJobListener {
    @EventHandler
    public void onJobSuccess(JobSuccessEvent event) {
        Job job = event.getJob();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(EnvConfig.getConfigs().get("telegram_chat_id"));
        sendMessage.setText(String.format("@%s Success! Job for %s completed.%n", job.getUserName(), job.getName()) +
                "ID: " + job.getId() + "\n" +
                "Archive URL: " + job.getArchiveUrl() + "\n" +
                "Explanation: " + job.getExplanation() + "\n" +
                "Logs: " + job.getLogsUrl());
        TelegramClient.getBot().tryToExecute(sendMessage);
    }

    @EventHandler
    public void onJobFailure(JobFailureEvent event) {
        Job job = event.getJob();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(EnvConfig.getConfigs().get("telegram_chat_id"));
        sendMessage.setText(String.format("@%s Job for %s failed with exit code %s.%n", job.getUserName(), job.getName(), job.getFailedTaskCode()) +
                "ID: " + job.getId() + "\n" +
                "Explanation: " + job.getExplanation() + "\n" +
                "Logs: " + job.getLogsUrl());
        TelegramClient.getBot().tryToExecute(sendMessage);
    }

    @EventHandler
    public void onJobAbort(JobAbortEvent event) {
        Job job = event.getJob();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(EnvConfig.getConfigs().get("telegram_chat_id"));
        sendMessage.setText(String.format("@%s Job for %s was aborted.%n", job.getUserName(), job.getName()) +
                "ID: " + job.getId() + "\n" +
                "Explanation: " + job.getExplanation() + "\n" +
                "Logs: " + job.getLogsUrl());
        TelegramClient.getBot().tryToExecute(sendMessage);
    }

    @EventHandler
    public void onJobQueued(JobQueuedEvent event) {
        Job job = event.getJob();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(EnvConfig.getConfigs().get("telegram_chat_id"));
        //        IRCClient.sendMessage(job.getUserName(), "Queued job! (" + job.getType() + "). You will be notified when it finishes. Use !status " + job.getId() + " for details.");

        sendMessage.setText(String.format("@%s Job for %s queued!%nYou will be notified when it finishes. Use !status %s for details.", job.getUserName(), job.getName(), job.getId()));
        TelegramClient.getBot().tryToExecute(sendMessage);
    }
}
