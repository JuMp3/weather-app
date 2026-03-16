package com.weather.app.service;

import com.weather.app.model.GeoLocation;
import com.weather.app.model.GeocodingResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
public class GeocodingService {

    private static final Logger log = LoggerFactory.getLogger(GeocodingService.class);

    @Value("${weather.geocoding.url}")
    private String geocodingBaseUrl;

    private final HttpClient unTrustedHttpClient;
    private final ObjectMapper objectMapper;

    public GeocodingService(@Qualifier("unTrustedHttpClient") HttpClient unTrustedHttpClient, ObjectMapper objectMapper) {
        this.unTrustedHttpClient = unTrustedHttpClient;
        this.objectMapper = objectMapper;
    }

    public List<GeoLocation> searchCities(String query, int count) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }

        String encoded = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8);
        String url = geocodingBaseUrl + "/search?name=" + encoded
                + "&count=" + count
                + "&language=it"
                + "&format=json";

        log.debug("Geocoding request: {}", url);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = unTrustedHttpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Geocoding API returned status {}", response.statusCode());
                return Collections.emptyList();
            }

            GeocodingResponse geocodingResponse = objectMapper.readValue(
                    response.body(), GeocodingResponse.class);

            List<GeoLocation> results = geocodingResponse.getResults();
            return results != null ? results : Collections.emptyList();

        } catch (IOException | InterruptedException e) {
            log.error("Error calling geocoding API", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return Collections.emptyList();
        }
    }

    public GeoLocation findCity(String name, Double lat, Double lon) {
        List<GeoLocation> results = searchCities(name, 10);
        if (lat != null && lon != null) {
            return results.stream()
                    .filter(g -> Math.abs(g.getLatitude() - lat) < 0.01
                            && Math.abs(g.getLongitude() - lon) < 0.01)
                    .findFirst()
                    .orElse(results.isEmpty() ? null : results.getFirst());
        }
        return results.isEmpty() ? null : results.getFirst();
    }
}
