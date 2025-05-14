package gg.norisk.faqbot.handler;

import gg.norisk.faqbot.service.FaqService;
import gg.norisk.faqbot.service.WebRequestService;
import gg.norisk.faqbot.utils.DiscordUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

import java.util.logging.Level;
import java.util.logging.Logger;

@RequiredArgsConstructor
public class SlashCommandHandler {
    private static final Logger LOGGER = Logger.getLogger("SlashCommandHandler");
    private final FaqService faqService;

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
}
