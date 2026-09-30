package com.premierleague.premier_league_service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Thin client for Google's Gemini "generateContent" REST endpoint.
 * Uses the free-tier-eligible Gemini API (an API key from
 * https://aistudio.google.com/apikey is enough — no billing account needed).
 */
@Component
public class GeminiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public GeminiClient(
            @Value("${gemini.api.key:}") String apiKey,
            @Value("${gemini.model:gemini-flash-latest}") String model
    ) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String generateText(String prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set. Get a free key from https://aistudio.google.com/apikey " +
                    "and set it in your .env file.");
        }

        GenerateContentRequest request = new GenerateContentRequest(
                List.of(new Content(List.of(new Part(prompt))))
        );

        GenerateContentResponse response;
        try {
            response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GenerateContentResponse.class);
        } catch (RestClientException e) {
            throw new IllegalStateException(
                    "Gemini API is temporarily unavailable (often free-tier capacity limits) — please retry shortly.", e);
        }

        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            throw new IllegalStateException("Gemini returned no candidates for this prompt.");
        }

        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            throw new IllegalStateException("Gemini returned an empty response.");
        }

        return content.parts().get(0).text();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateContentRequest(List<Content> contents) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateContentResponse(List<Candidate> candidates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {}
}
