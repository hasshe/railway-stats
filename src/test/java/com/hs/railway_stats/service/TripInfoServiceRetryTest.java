package com.hs.railway_stats.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.hs.railway_stats.dto.Leg;
import com.hs.railway_stats.dto.StopTime;
import com.hs.railway_stats.dto.TranslationDto;
import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.TripResponse;
import com.hs.railway_stats.dto.Trip;
import com.hs.railway_stats.external.RestClient;
import com.hs.railway_stats.repository.TripInfoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "tripinfo.retry.max-attempts=3",
        "tripinfo.retry.initial-interval-ms=10",
        "tripinfo.retry.multiplier=1.0",
        "tripinfo.retry.max-interval-ms=10"
})
class TripInfoServiceRetryTest {

    private static final ZoneId STOCKHOLM = ZoneId.of("Europe/Stockholm");

    @Autowired
    private TripInfoService tripInfoService;

    @MockitoBean
    private RestClient restClient;

    @MockitoBean
    private TripInfoRepository tripInfoRepository;

    @MockitoBean
    private TripInfoMetricService tripInfoMetricService;

    @MockitoBean
    private TranslationService translationService;

    @MockitoBean(name = "tripInfoCache")
    private Cache<String, List<TripInfoResponse>> tripInfoCache;

    @Test
    void collectTripInformation_retriesOnFailureAndExhaustsAttempts() {
        // Every attempt throws — all 3 attempts fire, then @Recover swallows the final exception
        when(translationService.getTranslationByName(anyString()))
                .thenThrow(new RuntimeException("simulated external failure"));

        assertThatNoException()
                .isThrownBy(() -> tripInfoService.collectTripInformation("Uppsala C", "Stockholm C"));

        // Once per attempt × 3
        verify(translationService, times(3)).getTranslationByName("Uppsala C");
    }

    @Test
    void collectTripInformation_stopsRetryingAfterSuccess() throws Exception {
        TranslationDto uppsalaDto = new TranslationDto(1, "Uppsala C", "UPP");
        TranslationDto stockholmDto = new TranslationDto(2, "Stockholm C", "STH");

        // Attempt 1: origin lookup throws → retry
        // Attempt 2: both lookups succeed, restClient returns a response that terminates the loop
        when(translationService.getTranslationByName(eq("Uppsala C")))
                .thenThrow(new RuntimeException("transient failure"))
                .thenReturn(uppsalaDto);
        when(translationService.getTranslationByName(eq("Stockholm C")))
                .thenReturn(stockholmDto);

        // Build a TripResponse whose last trip/leg has plannedDateTime at 23:59 today (Stockholm)
        // → isLastTrainOfDay returns true → loop exits after a single restClient call
        OffsetDateTime endOfDay = LocalDate.now(STOCKHOLM)
                .atTime(23, 59)
                .atZone(STOCKHOLM)
                .toOffsetDateTime();
        StopTime origin = new StopTime("Uppsala C", endOfDay, null);
        StopTime destination = new StopTime("Stockholm C", endOfDay, null);
        Leg leg = new Leg(origin, destination);
        Trip trip = new Trip(false, List.of(leg));
        TripResponse terminatingResponse = new TripResponse(List.of(trip), null);

        when(restClient.callSearch(1L, 2L, null)).thenReturn(terminatingResponse);

        assertThatNoException()
                .isThrownBy(() -> tripInfoService.collectTripInformation("Uppsala C", "Stockholm C"));

        // Uppsala C: 1 (throw on attempt 1) + 1 (succeed on attempt 2) = 2 total — NOT 3
        verify(translationService, times(2)).getTranslationByName("Uppsala C");
        // restClient called exactly once (only on the successful attempt 2)
        verify(restClient, times(1)).callSearch(1L, 2L, null);
    }
}
