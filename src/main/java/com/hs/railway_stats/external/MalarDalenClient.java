package com.hs.railway_stats.external;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.core.util.StringUtil;

@Component
public class MalarDalenClient implements RestClient {

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
            TripRequest requestBody;
            
            if (!StringUtil.isNullOrEmpty(nextToken)) {
                requestBody = new TripRequest(
                    originId,
                    destinationId,
                    nextToken,
                    null,
                    false,
                    false
                );
            } else {
                var start = OffsetDateTime.of(
                    LocalDate.now(),
                    LocalTime.of(1, 0, 0),
                    ZoneId.systemDefault().getRules().getOffset(java.time.Instant.now())
                );
                requestBody = new TripRequest(
                    originId,
                    destinationId,
                    nextToken,
                    start.toString(),
                    false,
            false
                );
            }
            return requestBody;
        }
}