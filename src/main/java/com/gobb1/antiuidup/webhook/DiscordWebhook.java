package com.gobb1.antiuidup.webhook;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class DiscordWebhook {
    private DiscordWebhook() {}

    private static final Gson GSON = new GsonBuilder().create();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static void send(String webhookUrl, String content) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;

        try {
            String body = GSON.toJson(new Payload(content));
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            CLIENT.sendAsync(req, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {}
    }

    private record Payload(String content) {}
}
