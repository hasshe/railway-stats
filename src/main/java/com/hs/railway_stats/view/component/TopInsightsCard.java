package com.hs.railway_stats.view.component;

import com.hs.railway_stats.repository.entity.TripInfoMetric;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopInsightsCard extends VerticalLayout {

    public TopInsightsCard() {
        addClassName("top-insights-card");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setMaxWidth("360px");
    }

    public void loadInsights(List<TripInfoMetric> metrics) {
        removeAll();

        if (metrics.isEmpty()) {
            return;
        }

        // Worst train: scheduled time with highest total reimbursable trips
        var worstMetric = metrics.stream()
                .max((a, b) -> Integer.compare(a.getTotalReimbursableTrips(), b.getTotalReimbursableTrips()))
                .orElse(null);

        String worstTime = worstMetric != null ? worstMetric.getScheduledDepartureTime().toString() : "-";
        int worstCount = worstMetric != null ? worstMetric.getTotalReimbursableTrips() : 0;
        int worstTotal = worstMetric != null ? worstMetric.getTotalTrips() : 0;

        // Time of day spans
        Map<String, int[]> spans = Map.of(
                "Early morning (05:00-09:59)", new int[]{5,9},
                "Daytime (10:00-15:59)", new int[]{10,15},
                "Evening (16:00-21:59)", new int[]{16,21},
                "Night (22:00-04:59)", new int[]{22,4}
        );

        Map<String, Integer> spanCounts = new HashMap<>();
        int totalReimbursableCount = metrics.stream().mapToInt(TripInfoMetric::getTotalReimbursableTrips).sum();
        for (TripInfoMetric m : metrics) {
            int count = m.getTotalReimbursableTrips();
            int hour = m.getScheduledDepartureTime().getHour();
            for (var e : spans.entrySet()) {
                int[] range = e.getValue();
                boolean inRange = range[0] <= range[1] ? (hour >= range[0] && hour <= range[1]) : (hour >= range[0] || hour <= range[1]);
                if (inRange) {
                    spanCounts.put(e.getKey(), spanCounts.getOrDefault(e.getKey(), 0) + count);
                    break;
                }
            }
        }

        String bestSpan = "-";
        int bestPercent = 0;
        if (!spanCounts.isEmpty()) {
            var best = spanCounts.entrySet().stream().max(Map.Entry.comparingByValue()).get();
            bestSpan = best.getKey();
            bestPercent = totalReimbursableCount > 0 ? (int) Math.round(best.getValue() * 100.0 / totalReimbursableCount) : 0;
        }

        long cancelled = metrics.stream().mapToLong(m -> m.getCanceledTripDates() != null ? m.getCanceledTripDates().size() : 0).sum();
        long totalTrips = metrics.stream().mapToLong(TripInfoMetric::getTotalTrips).sum();
        int cancelPercent = totalTrips > 0 ? (int) Math.round(cancelled * 100.0 / totalTrips) : 0;

        // Build two metric-style insight cards (Worst train, Worst time span)
        Div grid = new Div();
        grid.addClassName("metrics-grid");
        grid.setWidthFull();

        grid.add(MetricCardBuilder.createMetricCard(
                "Worst train",
                worstTime,
                String.format("%d reimbursable — of %d trips", worstCount, worstTotal),
                VaadinIcon.TRENDING_DOWN,
                "metric-card",
                "metric-card--red"
        ));

        grid.add(MetricCardBuilder.createMetricCard(
                "Worst time span",
                bestSpan,
                String.format("%d%% of claims", bestPercent),
                VaadinIcon.CLOCK,
                "metric-card",
                "metric-card--orange"
        ));

        add(grid);
    }

}
