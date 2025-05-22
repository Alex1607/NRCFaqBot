package gg.norisk.faqbot.utils;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class DiscordUtils {
    private static final List<Long> SUPPORTER_ID = Arrays.stream(System.getenv("SUPPORTER_ID").split(",")).map(Long::parseLong).toList();
    private static final List<Long> SARCASTIC_MODE_ID = Arrays.stream(System.getenv("SARCASTIC_MODE_ID") != null ? System.getenv("SARCASTIC_MODE_ID").split(",") : new String[0]).map(Long::parseLong).toList();

    DiscordUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static boolean missingRoles(Member member) {
        return !SUPPORTER_ID.isEmpty() && member.getRoles().stream()
                .noneMatch(role -> SUPPORTER_ID.contains(role.getIdLong()));
    }

    public static boolean missingSarcasticModeRoles(Member member) {
        return !SARCASTIC_MODE_ID.isEmpty() && member.getRoles().stream()
                .noneMatch(role -> SARCASTIC_MODE_ID.contains(role.getIdLong()));
    }

    public static Optional<MessageEmbed.Field> findReasonField(Message message) {
        return message.getEmbeds().stream()
                .map(MessageEmbed::getFields)
                .filter(fields -> fields.stream()
                        .anyMatch(field -> "Grund".equals(field.getName())))
                .flatMap(Collection::stream)
                .filter(field -> "Grund".equals(field.getName()))
                .findFirst();
    }

}
