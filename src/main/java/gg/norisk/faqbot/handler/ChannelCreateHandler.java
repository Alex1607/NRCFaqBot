package gg.norisk.faqbot.handler;

import gg.norisk.faqbot.service.FaqService;
import gg.norisk.faqbot.service.MessageGeneratorService;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RequiredArgsConstructor
public class ChannelCreateHandler {

    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();
    private final FaqService faqService;
    private final Set<Long> supportForumIds;
    private final MessageGeneratorService messageGeneratorService;

    public void handleThreadChannelCreation(ThreadChannel channel) {
        if (faqService.invalidFAQ()) {
            return;
        }
        if (supportForumIds.contains(channel.getParentChannel().asForumChannel().getIdLong())) {
            EXECUTOR_SERVICE.execute(() -> {
                channel.sendTyping().queue();
                messageGeneratorService.generateAndSendThread(channel);
            });
        }
    }
}
