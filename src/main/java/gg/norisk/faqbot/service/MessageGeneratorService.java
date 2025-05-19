package gg.norisk.faqbot.service;

import gg.norisk.faqbot.Main;
import gg.norisk.faqbot.utils.DiscordUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class MessageGeneratorService {

    private static final Logger LOGGER = Logger.getLogger("MessageGeneratorService");

    private static final String NO_ANSWER = "ich weiß es nicht";
    private final WebRequestService webRequestService;
    private final FaqService faqService;
    private final String prompt;
    private final String generalPrompt;

    public boolean generateAndSendMessage(Message message) {
        String response;
        try {
            String question = message.getContentRaw();
            Optional<MessageEmbed.Field> optionalReasonField = DiscordUtils.findReasonField(message);
            if (optionalReasonField.isPresent()) {
                question = optionalReasonField.get().getValue();
            }

            response = webRequestService.fetchModelResponse(buildThreadPrompt(question));
            response = sanitizeResponse(response);
            if (response.toLowerCase().contains(NO_ANSWER)) {
                LOGGER.info("AI doesn't know the answer");
                return false;
            }
        } catch (IOException | URISyntaxException | RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Failed to get response from AI", e);
            return false;
        }
        if (Main.RESPONDED_MESSAGES.contains(message.getIdLong())) {
            return false;
        }
        message.reply(response + "\n-# Diese Nachricht wurde mittels einer AI anhand des FAQs generiert.").queue();

        Main.RESPONDED_MESSAGES.add(message.getIdLong());
        return true;
    }

    public void generateAndSendThread(ThreadChannel channel) {
        channel.retrieveStartMessage().queue(message -> {
            String response;
            try {
                response = webRequestService.fetchModelResponse(buildThreadPrompt(channel.getName() + "\n" + message.getContentRaw()));
                response = sanitizeResponse(response);
                if (response.toLowerCase().contains(NO_ANSWER)) {
                    LOGGER.info("AI doesn't know the answer");
                    return;
                }
            } catch (IOException | URISyntaxException | RuntimeException e) {
                LOGGER.log(Level.SEVERE, "Failed to get response from AI", e);
                return;
            }
            message.reply(response + "\n-# Diese Nachricht wurde mittels einer AI anhand des FAQs generiert.").queue();
        });
    }

    public void processUserQuestion(MessageReceivedEvent event, String message) {
        event.getChannel().sendTyping().queue();

        try {
            String response = webRequestService.fetchModelResponse(generalPrompt.replace("!!!USER_QUESTION!!!", message));
            response = sanitizeResponse(response);
            event.getChannel().sendMessage(response + "\n-# Der Bot war für **diese Antwort** nicht im FAQ-Modus und ist sarkastisch veranlagt. __Bitte nimm diese Nachricht nicht zu ernst__. Dieses Feature ist zum Spaß und kann nur von Teammitgliedern genutzt werden.").queue();
        } catch (IOException | URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Failed to get response from AI", e);
        }
    }

    private String sanitizeResponse(String response) {
        return response.replace("@everyone", "@\u200Beveryone")
                .replace("@here", "@\u200Bhere")
                .replaceAll("<think>[\\d\\D]*</think>", "");
    }

    private String buildThreadPrompt(String question) {
        return prompt.replace("!!!USER_QUESTION!!!", question)
                .replace("!!!FAQ_TEXT!!!", faqService.getFaqText());
    }

    public String generateFaqResponse(String question) throws IOException, URISyntaxException {
        if (faqService.invalidFAQ()) {
            return null;
        }

        String response = webRequestService.fetchModelResponse(buildThreadPrompt(question));
        response = sanitizeResponse(response);

        if (response.toLowerCase().contains(NO_ANSWER)) {
            return null;
        }

        return response;
    }
}
