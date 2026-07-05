package com.hs.railway_stats.util;

import com.hs.railway_stats.repository.TranslationRepository;
import com.hs.railway_stats.repository.TripInfoMetricRepository;
import com.hs.railway_stats.repository.TripInfoRepository;
import com.hs.railway_stats.repository.entity.Translation;
import com.hs.railway_stats.repository.entity.TripInfoMetric;
import com.hs.railway_stats.repository.entity.TripInfo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Database seeder for development/testing.
 * Run with: --seed-db flag to populate the database
 */
@Component
public class DbSeeder implements CommandLineRunner {

    private final TranslationRepository translationRepository;
    private final TripInfoRepository tripInfoRepository;
    private final TripInfoMetricRepository tripInfoMetricRepository;
    private final ApplicationContext applicationContext;
    private final Random random = new Random(42); // Fixed seed for reproducibility

    public DbSeeder(TranslationRepository translationRepository,
                    TripInfoRepository tripInfoRepository,
                    TripInfoMetricRepository tripInfoMetricRepository,
                    ApplicationContext applicationContext) {
        this.translationRepository = translationRepository;
        this.tripInfoRepository = tripInfoRepository;
        this.tripInfoMetricRepository = tripInfoMetricRepository;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) throws Exception {
        boolean shouldSeed = hasFlag(args, "--seed-db");
        boolean shouldClear = hasFlag(args, "--clear-db");

        if (!shouldSeed && !shouldClear) {
            return;
        }

        // Clear existing data
        clearDatabase();

        if (shouldClear && !shouldSeed) {
            System.out.println("✅ Database cleared");
            SpringApplication.exit(applicationContext, () -> 0);
            return;
        }

        System.out.println("Seeding database...");

        // Seed stations
        seedStations();

        // Seed trips
        seedTrips();

        System.out.println("✅ Database seeding complete!");
        
        // Exit application after seeding
        SpringApplication.exit(applicationContext, () -> 0);
    }

    private boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (flag.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private void clearDatabase() {
        tripInfoRepository.deleteAll();
        tripInfoMetricRepository.deleteAll();
        translationRepository.deleteAll();
    }

    private void seedStations() {
        Translation uppsala = Translation.builder()
                .stationId(1)
                .stationName("uppsala c")
                .claimsStationId("Uppsala C")
                .build();

        Translation stockholm = Translation.builder()
                .stationId(2)
                .stationName("stockholm c")
                .claimsStationId("Stockholm C")
                .build();

        translationRepository.save(uppsala);
        translationRepository.save(stockholm);

        System.out.println("✓ Seeded 2 stations (Uppsala C, Stockholm C)");
    }

    private void seedTrips() {
        ZoneId zone = ZoneId.of("Europe/Stockholm");
        ZonedDateTime baseDate = ZonedDateTime.now(zone).minusDays(30).withHour(0).withMinute(0).withSecond(0);

        int tripCount = 0;
        Map<MetricKey, MetricSeed> metricSeeds = new HashMap<>();

        // Generate 60 days of trips
        for (int day = 0; day < 60; day++) {
            ZonedDateTime dayStart = baseDate.plusDays(day);

            // Generate trips for each hour of the day (multiple departure times)
            for (int hour = 5; hour < 23; hour++) {
                // 2-3 trains per hour
                int tripsThisHour = random.nextInt(2) + 2;

                for (int i = 0; i < tripsThisHour; i++) {
                    int minute = random.nextInt(60);
                    ZonedDateTime departureTime = dayStart.withHour(hour).withMinute(minute);

                    // Randomly alternate between Uppsala->Stockholm and Stockholm->Uppsala
                    int originId = random.nextBoolean() ? 1 : 2;
                    int destinationId = 3 - originId; // Toggle between 1 and 2

                    // Random outcomes:
                    // 5% cancelled
                    // 15% on time
                    // 30% 20-39 min late (50% refund)
                    // 25% 40-59 min late (75% refund)
                    // 25% 60+ min late (full refund)
                    int randOutcome = random.nextInt(100);
                    TripInfo trip;

                    if (randOutcome < 5) {
                        // Cancelled
                        trip = TripInfo.builder()
                                .originId(originId)
                                .destinationId(destinationId)
                                .originalDepartureTime(departureTime)
                                .canceled(1)
                                .minutesLate(0)
                                .build();
                    } else if (randOutcome < 20) {
                        // On time
                        trip = TripInfo.builder()
                                .originId(originId)
                                .destinationId(destinationId)
                                .originalDepartureTime(departureTime)
                                .canceled(0)
                                .minutesLate(0)
                                .actualArrivalTime(departureTime.plusMinutes(30))
                                .build();
                    } else if (randOutcome < 50) {
                        // 20-39 minutes late
                        int delay = random.nextInt(20) + 20;
                        trip = TripInfo.builder()
                                .originId(originId)
                                .destinationId(destinationId)
                                .originalDepartureTime(departureTime)
                                .canceled(0)
                                .minutesLate(delay)
                                .actualArrivalTime(departureTime.plusMinutes(30 + delay))
                                .build();
                    } else if (randOutcome < 75) {
                        // 40-59 minutes late
                        int delay = random.nextInt(20) + 40;
                        trip = TripInfo.builder()
                                .originId(originId)
                                .destinationId(destinationId)
                                .originalDepartureTime(departureTime)
                                .canceled(0)
                                .minutesLate(delay)
                                .actualArrivalTime(departureTime.plusMinutes(30 + delay))
                                .build();
                    } else {
                        // 60+ minutes late
                        int delay = random.nextInt(40) + 60;
                        trip = TripInfo.builder()
                                .originId(originId)
                                .destinationId(destinationId)
                                .originalDepartureTime(departureTime)
                                .canceled(0)
                                .minutesLate(delay)
                                .actualArrivalTime(departureTime.plusMinutes(30 + delay))
                                .build();
                    }

                    tripInfoRepository.save(trip);
                    accumulateMetric(metricSeeds, trip, departureTime.toLocalDate());
                    tripCount++;
                }
            }
        }

        persistMetrics(metricSeeds);

        System.out.println("✓ Seeded " + tripCount + " trips across 60 days");
        System.out.println("✓ Seeded " + metricSeeds.size() + " statistics rows");
    }

    private void accumulateMetric(Map<MetricKey, MetricSeed> metricSeeds, TripInfo trip, LocalDate date) {
        LocalTime scheduledDepartureTime = trip.getOriginalDepartureTime().toLocalTime();
        MetricKey key = new MetricKey(trip.getOriginId(), trip.getDestinationId(), scheduledDepartureTime);
        MetricSeed seed = metricSeeds.computeIfAbsent(key, ignored -> new MetricSeed());

        seed.totalTrips++;
        seed.averageMinutesLateSum += Math.max(0, trip.getMinutesLate() != null ? trip.getMinutesLate() : 0);

        if (trip.getCanceled() != null && trip.getCanceled() == 1) {
            seed.canceledTripDates.add(date);
        }

        if ((trip.getCanceled() != null && trip.getCanceled() == 1) || (trip.getMinutesLate() != null && trip.getMinutesLate() >= 20)) {
            seed.totalReimbursableTrips++;
            if (random.nextInt(100) < 70) {
                seed.totalReimbursementsRequested++;
            }
        }
    }

    private void persistMetrics(Map<MetricKey, MetricSeed> metricSeeds) {
        List<TripInfoMetric> metrics = new ArrayList<>();
        for (Map.Entry<MetricKey, MetricSeed> entry : metricSeeds.entrySet()) {
            MetricKey key = entry.getKey();
            MetricSeed seed = entry.getValue();
            int averageMinutesLate = seed.totalTrips > 0 ? seed.averageMinutesLateSum / seed.totalTrips : 0;

            metrics.add(TripInfoMetric.builder()
                    .originId(key.originId)
                    .destinationId(key.destinationId)
                    .scheduledDepartureTime(key.scheduledDepartureTime)
                    .totalTrips(seed.totalTrips)
                    .averageMinutesLate(averageMinutesLate)
                    .canceledTripDates(new ArrayList<>(seed.canceledTripDates))
                    .totalReimbursableTrips(seed.totalReimbursableTrips)
                    .totalReimbursementsRequested(seed.totalReimbursementsRequested)
                    .build());
        }

        tripInfoMetricRepository.saveAll(metrics);
    }

    private static final class MetricKey {
        private final int originId;
        private final int destinationId;
        private final LocalTime scheduledDepartureTime;

        private MetricKey(int originId, int destinationId, LocalTime scheduledDepartureTime) {
            this.originId = originId;
            this.destinationId = destinationId;
            this.scheduledDepartureTime = scheduledDepartureTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MetricKey metricKey = (MetricKey) o;
            return originId == metricKey.originId && destinationId == metricKey.destinationId && scheduledDepartureTime.equals(metricKey.scheduledDepartureTime);
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(originId);
            result = 31 * result + Integer.hashCode(destinationId);
            result = 31 * result + scheduledDepartureTime.hashCode();
            return result;
        }
    }

    private static final class MetricSeed {
        private int totalTrips;
        private int averageMinutesLateSum;
        private int totalReimbursableTrips;
        private int totalReimbursementsRequested;
        private final List<LocalDate> canceledTripDates = new ArrayList<>();
    }
}
