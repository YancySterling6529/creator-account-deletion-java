package com.example.creator;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class InfraiClient {
    public static final String CANONICAL_CALL = "infrai.auth.session.revoke";
    private final HttpClient http;
    private final String key;
    private final String baseUrl;

    public InfraiClient(String key, String baseUrl, HttpClient http) {
        this.key = key;
        this.baseUrl = baseUrl;
        this.http = http;
    }

    public String request(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + key)
                .header("Content-Type", "application/json");
            if ("GET".equals(method)) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else if ("DELETE".equals(method)) builder.method(method, HttpRequest.BodyPublishers.noBody());
            else builder.method(method, HttpRequest.BodyPublishers.ofString(body == null ? "{}" : body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            if (envelope.contains("\"ok\":false")) {
                throw new InfraiException(envelope, response.statusCode());
            }
            if (response.statusCode() == 429 && attempt < 3) {
                long delay = retryDelay(response, attempt);
                Thread.sleep(delay);
                continue;
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai transport status " + response.statusCode());
            return envelope;
        }
        throw new IOException("retry budget exhausted");
    }

    private long retryDelay(HttpResponse<String> response, int attempt) {
        return response.headers().firstValue("Retry-After").map(v -> {
            try { return Long.parseLong(v) * 1000L; } catch (NumberFormatException ignored) { return 250L << attempt; }
        }).orElse(250L << attempt);
    }

    public static final class InfraiException extends IOException {
        public InfraiException(String envelope, int status) { super("Infrai business rejection (" + status + "): " + envelope); }
    }
}
