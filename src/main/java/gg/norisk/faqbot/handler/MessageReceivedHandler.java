package gg.norisk.faqbot.handler;

import gg.norisk.faqbot.Main;
import gg.norisk.faqbot.service.FaqService;
import gg.norisk.faqbot.service.MessageGeneratorService;
import gg.norisk.faqbot.utils.DiscordUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


@RequiredArgsConstructor
public class MessageReceivedHandler {

    private static final Logger LOGGER = Logger.getLogger("MessageReceivedHandler");
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();
    private final FaqService faqService;
    private final MessageGeneratorService messageGeneratorService;

    public void handleGalaxyBotMessage(MessageReceivedEvent event) {
        Optional<MessageEmbed.Field> reasonField = DiscordUtils.findReasonField(event.getMessage());

        if (reasonField.isEmpty()) {
            return;
        }

        if (faqService.invalidFAQ()) {
            LOGGER.warning("FAQ is empty");
            return;
        }

        if (Main.RESPONDED_MESSAGES.contains(event.getMessage().getIdLong())) {
            LOGGER.info("Already responded to this message");
            return;
        }

        // Check if the message is in the specified category
        long categoryId = 1193510089219391609L;

        if (!event.isFromGuild() ||
                !(event.getChannel() instanceof TextChannel textChannel &&
                        textChannel.getParentCategory() != null &&
                        textChannel.getParentCategory().getIdLong() == categoryId)) {
            LOGGER.info("Message not in the specified category");
            return;
        }

        EXECUTOR_SERVICE.execute(() -> {
            event.getChannel().sendTyping().queue();
            messageGeneratorService.generateAndSendMessage(event.getMessage());
        });
    }

    public void handleMentionMessage(MessageReceivedEvent event) {
        if (DiscordUtils.missingSarcasticModeRoles(event.getMember())) {
            LOGGER.log(Level.INFO, "User {0} attempted to use sarcastic mode without required role", event.getMember().getUser().getAsTag());
            return;
        }

        String message = buildMessageContent(event);

        EXECUTOR_SERVICE.execute(() -> messageGeneratorService.processUserQuestion(event, message));
    }

    private String buildMessageContent(MessageReceivedEvent event) {
        StringBuilder messageBuilder = new StringBuilder();

        // Add referenced message if it exists
        if (event.getMessage().getReferencedMessage() != null) {
            Message referencedMessage = event.getMessage().getReferencedMessage();
            messageBuilder.append(formatUserTag(referencedMessage.getAuthor()))
                    .append(":\n")
                    .append(referencedMessage.getContentRaw())
                    .append("\n\n");
        }

        // Add current message
        messageBuilder.append(formatUserTag(event.getMessage().getAuthor()))
                .append(":\n")
                .append(event.getMessage().getContentRaw().replace(
                        event.getJDA().getSelfUser().getAsMention(), ""));

        return messageBuilder.toString();
    }

    private String formatUserTag(User user) {
        return user.getAsTag().replace("#0000", "");
    }
}
