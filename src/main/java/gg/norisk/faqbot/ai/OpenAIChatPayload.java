package gg.norisk.faqbot.ai;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenAIChatPayload {

    private String model;
    private float temperature;

    @SerializedName("top_p")
    private float topP;

    @SerializedName("frequency_penalty")
    private float frequencyPenalty;

    @SerializedName("max_tokens")
    private Integer maxTokens;
    
    @Singular
    private List<Message> messages;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}