package gg.norisk.faqbot;

import gg.norisk.faqbot.handler.ChannelCreateHandler;
import gg.norisk.faqbot.handler.MessageContextInteractionHandler;
import gg.norisk.faqbot.handler.MessageReceivedHandler;
import gg.norisk.faqbot.handler.SlashCommandHandler;
import gg.norisk.faqbot.service.FaqService;
import gg.norisk.faqbot.service.MessageGeneratorService;
import gg.norisk.faqbot.service.WebRequestService;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel;
import net.dv8tion.jda.api.events.channel.ChannelCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main extends ListenerAdapter {
    // Create a state service / object to store the responded messages
    public static final Set<Long> RESPONDED_MESSAGES = new HashSet<>();
    private static SlashCommandHandler slashCommandHandler;
    private static MessageReceivedHandler messageReceivedHandler;
    private static ChannelCreateHandler channelCreateHandler;
    private static MessageContextInteractionHandler messageContextInteractionHandler;

    public static void main(String[] args) throws InterruptedException {
        HashSet<Long> supportForumId = new HashSet<>(Arrays.stream(System.getenv("SUPPORT_FORUM_ID").split(",")).map(Long::parseLong).toList());

        WebRequestService webRequestService = new WebRequestService(System.getenv("AI_URL"), System.getenv("AI_API_KEY"), System.getenv("AI_MODEL"));
        FaqService faqService = new FaqService(WebRequestService.loadFAQ());
        MessageGeneratorService messageGeneratorService = new MessageGeneratorService(webRequestService, faqService, System.getenv("AI_PROMPT"), System.getenv("AI_PROMPT_GENERAL"));

        slashCommandHandler = new SlashCommandHandler(faqService);
        messageReceivedHandler = new MessageReceivedHandler(faqService, messageGeneratorService);
        channelCreateHandler = new ChannelCreateHandler(faqService, supportForumId, messageGeneratorService);
        messageContextInteractionHandler = new MessageContextInteractionHandler(faqService, supportForumId, messageGeneratorService);

        JDA jda = JDABuilder.createLight(System.getenv("DISCORD_BOT_API_KEY"), List.of(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)).addEventListeners(new Main()).build();
        jda.awaitReady();
        jda.updateCommands().addCommands(
                Commands.message("AI Antwort"),
                Commands.slash("refresh", "Lädt das FAQ neu")
        ).queue();
    }

    @Override
    public void onChannelCreate(ChannelCreateEvent event) {
        if ((event.getChannel() instanceof ThreadChannel channel)) {
            channelCreateHandler.handleThreadChannelCreation(channel);
        }
    }

    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent messageReceivedEvent) {
        // Handle Galaxy bot interactions
        if (isGalaxyBotMessage(messageReceivedEvent)) {
            messageReceivedHandler.handleGalaxyBotMessage(messageReceivedEvent);
            return;
        }

        // Handle direct mentions
        if (isBotMentioned(messageReceivedEvent)) {
            messageReceivedHandler.handleMentionMessage(messageReceivedEvent);
        }
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent interactionEvent) {
        if (!isValidGuildCommand(interactionEvent)) return;

        if ("refresh".equals(interactionEvent.getName())) {
            slashCommandHandler.handleRefreshCommand(interactionEvent);
        } else {
            interactionEvent.deferReply(true).setContent("I can't handle that command right now :(").queue();
        }
    }

    @Override
    public void onMessageContextInteraction(@NotNull MessageContextInteractionEvent contextInteractionEvent) {
        messageContextInteractionHandler.handleMessageContextInteraction(contextInteractionEvent);
    }


    private boolean isValidGuildCommand(SlashCommandInteractionEvent event) {
        return event.getGuild() != null && event.getMember() != null;
    }

    private boolean isGalaxyBotMessage(MessageReceivedEvent event) {
        return 697498867754729482L == event.getMessage().getAuthor().getIdLong();
    }

    private boolean isBotMentioned(MessageReceivedEvent event) {
        return event.getMessage().getMentions().getUsers()
                .contains(event.getJDA().getSelfUser());
    }
}