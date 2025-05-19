package gg.norisk.faqbot.handler;

import gg.norisk.faqbot.service.FaqService;
import gg.norisk.faqbot.service.MessageGeneratorService;
import gg.norisk.faqbot.service.WebRequestService;
import gg.norisk.faqbot.utils.DiscordUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SlashCommandHandler {
    private static final Logger LOGGER = Logger.getLogger("SlashCommandHandler");
    private final FaqService faqService;
    private final MessageGeneratorService messageGeneratorService;

    public void handleRefreshCommand(SlashCommandInteractionEvent event) {
        if (DiscordUtils.missingRoles(event.getMember())) {
            event.reply("You don't have permissions for this command!").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();
        String faqText = WebRequestService.loadFAQ();
        LOGGER.log(Level.INFO, "FAQ was reloaded by {0}, it now it {1} chars long",
                new Object[]{event.getMember().getUser().getAsTag(), faqText.length()});
        event.getHook().editOriginal("FAQ wurde neu geladen").queue();

        faqService.setFaqText(faqText);
    }

    public void handleFaqCommand(SlashCommandInteractionEvent event) {
        if (DiscordUtils.missingRoles(event.getMember())) {
            event.reply("You don't have permissions for this command!").setEphemeral(true).queue();
            return;
        }

        if (faqService.invalidFAQ()) {
            event.reply("FAQ is not available at the moment. Please try again later.").setEphemeral(true).queue();
            return;
        }

        String question = event.getOption("question").getAsString();
        if (question == null || question.trim().isEmpty()) {
            event.reply("Please provide a question.").setEphemeral(true).queue();
            return;
        }

        event.deferReply(true).queue();

        try {
            String response = messageGeneratorService.generateFaqResponse(question);

            if (response == null) {
                event.getHook().editOriginal("I couldn't find an answer to that question in the FAQ.").queue();
                return;
            }

            event.getChannel().sendMessage(response + "\n-# Diese Nachricht wurde mittels einer AI anhand des FAQs generiert.").queue();
            LOGGER.log(Level.INFO, "FAQ question answered: {0}", question);
        } catch (IOException | URISyntaxException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to get response from AI", e);
            event.getHook().editOriginal("Failed to get a response. Please try again later.").queue();
        }
    }

}
