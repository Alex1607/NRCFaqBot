package gg.norisk.faqbot.handler;

import gg.norisk.faqbot.Main;
import gg.norisk.faqbot.service.FaqService;
import gg.norisk.faqbot.service.MessageGeneratorService;
import gg.norisk.faqbot.utils.DiscordUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.Command;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class MessageContextInteractionHandler {
    private static final Logger LOGGER = Logger.getLogger("MessageContextInteractionHandler");
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newVirtualThreadPerTaskExecutor();

    private final FaqService faqService;
    private final MessageGeneratorService messageGeneratorService;

    public void handleMessageContextInteraction(MessageContextInteractionEvent event) {
        JDA jda = event.getJDA();
        if (event.getMember() == null) {
            LOGGER.warning("Member is null");
            return;
        }
        if (DiscordUtils.missingRoles(event.getMember())) {
            LOGGER.log(Level.INFO, "The user {0}, tried to use the AI Antwort command", event.getMember().getUser().getAsTag());
            event.reply("You don't have permissions for this command!").setEphemeral(true).queue();
            return;
        }

        if (!jda.retrieveCommands().complete().stream().map(Command::getName).toList().contains(event.getName())
                || event.getTarget().getAuthor().equals(jda.getSelfUser())) {
            return;
        }
        Message message = event.getTarget();

        if (faqService.invalidFAQ()) {
            event.reply("FAQ konnte nicht geladen werden :(").setEphemeral(true).queue();
            return;
        }

        if (Main.RESPONDED_MESSAGES.contains(message.getIdLong())) {
            event.reply("Auf diese Nachricht wurde bereits eine AI Antwort gegeben!").setEphemeral(true).queue();
            return;
        }

        if (!event.getName().equals("AI Antwort")) {
            return;
        }
        event.deferReply(true).queue();

        EXECUTOR_SERVICE.execute(() -> {
            event.getMessageChannel().sendTyping().queue();
            if (messageGeneratorService.generateAndSendMessage(message)) {
                event.getHook().editOriginal("AI Antwort wurde erfolgreich gesendet").queue();
            } else {
                event.getHook().editOriginal("AI konnte keine Antwort finden").queue();
            }
        });
    }
}
