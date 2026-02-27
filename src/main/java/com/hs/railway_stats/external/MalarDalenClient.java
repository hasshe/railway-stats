package com.hs.railway_stats.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.railway_stats.dto.ClaimRequest;
import com.hs.railway_stats.dto.TripRequest;
import com.hs.railway_stats.dto.TripResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Component
public class MalarDalenClient implements RestClient {

    public static final String ZONE_ID = "Europe/Stockholm";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private static final String BASE_URL =
            "https://v2.api.transithub.se/travelplanner/api/v2/trip";

    public MalarDalenClient() {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    @Override
    public TripResponse callSearch(long originId, long destinationId, String nextToken)
            throws IOException, InterruptedException {

        TripRequest requestBody = getRequestBody(originId, destinationId, nextToken);

        String json = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = getRequest(json);

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(
                    "API call failed with status: " + response.statusCode()
            );
        }
        return objectMapper.readValue(response.body(), TripResponse.class);
    }

    private HttpRequest getRequest(String json) {
        return HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
    }

    private TripRequest getRequestBody(long originId, long destinationId, String nextToken) {
        var start = OffsetDateTime.of(
                LocalDate.now(),
                LocalTime.of(1, 0, 0),
                ZoneId.of(ZONE_ID).getRules().getOffset(Instant.now())
        );
        return new TripRequest(
                originId,
                destinationId,
                nextToken,
                start.toString(),
                false,
                false);
    }

    @Override
    public void callClaim(ClaimRequest body) throws IOException, InterruptedException {
        String url = "https://evf-regionsormland.preciocloudapp.net/api/Claims";
        String json = objectMapper.writeValueAsString(body);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new RuntimeException("Claim API call failed with status: " + response.statusCode());
        }
    }
}