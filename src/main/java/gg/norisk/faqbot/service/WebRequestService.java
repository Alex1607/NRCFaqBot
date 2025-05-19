package gg.norisk.faqbot.service;

import com.google.gson.Gson;
import gg.norisk.faqbot.ai.ChatCompletionResponse;
import gg.norisk.faqbot.ai.OpenAIChatPayload;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class WebRequestService {
    private static final Logger LOGGER = Logger.getLogger("WebRequestService");
    private static final Gson GSON = new Gson();

    private final String llmApiUrl;
    private final String authToken;
    private final String model;

    public static String loadFAQ() {
        try {
            URL url = new URL("https://raw.githubusercontent.com/Alex1607/NoRiskFAQ/refs/heads/master/faq.txt");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load FAQ", e);
            return "";
        }
    }

    public String fetchModelResponse(String prompt) throws IOException, URISyntaxException {
        Instant start = Instant.now();
        HttpURLConnection connection = setupConnection();

        String payload = createRequestPayload(prompt);
        sendRequest(connection, payload);

        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            handleErrorResponse(connection);
            throw new RuntimeException("Failed to get response from AI");
        }

        String response = readResponse(connection);
        logRequestTime(start);
        return response;
    }

    private HttpURLConnection setupConnection() throws IOException, URISyntaxException {
        URL url = new URI(llmApiUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + authToken);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        return connection;
    }

    private String createRequestPayload(String prompt) {
        return GSON.toJson(
                OpenAIChatPayload.builder().model(model)
                        .temperature(0.6f)
                        .topP(0.5f)
                        .frequencyPenalty(1.5f)
                        .maxTokens(1000)
                        .message(new OpenAIChatPayload.Message("user", prompt))
        );
    }

    private void sendRequest(HttpURLConnection connection, String payload) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleErrorResponse(HttpURLConnection connection) throws IOException {
        try (InputStream is = connection.getErrorStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            String error = new BufferedReader(reader).lines().collect(Collectors.joining("\n"));
            LOGGER.warning(() -> MessageFormat.format("Failed to get response from API, error: {0}", error));
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        try (InputStream is = connection.getInputStream();
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            ChatCompletionResponse response = GSON.fromJson(reader, ChatCompletionResponse.class);
            if (response.getChoices().isEmpty()) {
                LOGGER.warning("No response from AI");
                throw new RuntimeException("No response from AI");
            }
            return response.getChoices().getFirst().getMessage().getContent();
        }
    }

    private void logRequestTime(Instant start) {
        LOGGER.info(() -> "AI Prompt Request took " +
                Instant.now().minusMillis(start.toEpochMilli()).toEpochMilli());
    }
}
