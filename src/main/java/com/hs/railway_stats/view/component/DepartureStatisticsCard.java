package com.hs.railway_stats.view.component;

import com.hs.railway_stats.repository.entity.TripInfoMetric;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.List;

/**
 * Displays aggregated departure statistics as metric cards.
 * Shows reimbursable trips, claims requested, cancelled trips, and cancellation rate.
 */
public class DepartureStatisticsCard extends VerticalLayout {

    public DepartureStatisticsCard() {
        addClassName("departure-stats-container");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setMaxWidth("700px");
        getStyle()
                .set("margin-left", "auto")
                .set("margin-right", "auto");
    }

    public void loadMetrics(List<TripInfoMetric> metrics) {
        removeAll();

        if (metrics.isEmpty()) {
            return;
        }

        // Calculate aggregated metrics
        int totalReimbursableTrips = metrics.stream()
                .mapToInt(TripInfoMetric::getTotalReimbursableTrips)
                .sum();
        int totalReimbursementsRequested = metrics.stream()
                .mapToInt(TripInfoMetric::getTotalReimbursementsRequested)
                .sum();
        int totalTrips = metrics.stream()
                .mapToInt(TripInfoMetric::getTotalTrips)
                .sum();
        long cancelledTrips = metrics.stream()
                .mapToLong(m -> m.getCanceledTripDates() != null ? m.getCanceledTripDates().size() : 0)
                .sum();
        double reimbursableRate = totalTrips > 0 ? (totalReimbursableTrips * 100.0 / totalTrips) : 0;

        // Create metrics grid
        HorizontalLayout metricsGrid = new HorizontalLayout();
        metricsGrid.addClassName("metrics-grid");
        metricsGrid.setWidthFull();
        metricsGrid.setSpacing(false);
        metricsGrid.setPadding(false);

        // Reimbursable trips card
        metricsGrid.add(MetricCardBuilder.createMetricCard(
                "Reimbursable trips",
                String.valueOf(totalReimbursableTrips),
                "Total",
                VaadinIcon.TRAIN,
                "metric-card",
                "metric-card--blue"
        ));

        // Claims requested card
        metricsGrid.add(MetricCardBuilder.createMetricCard(
                "Claims requested",
                String.valueOf(totalReimbursementsRequested),
                "Total",
                VaadinIcon.CLIPBOARD_CHECK,
                "metric-card",
                "metric-card--green"
        ));

        // Cancelled trips card
        metricsGrid.add(MetricCardBuilder.createMetricCard(
                "Cancelled trips",
                String.valueOf(cancelledTrips),
                "Total",
                VaadinIcon.CLOSE_CIRCLE,
                "metric-card",
                "metric-card--red"
        ));

        // Reimbursable rate card
        metricsGrid.add(MetricCardBuilder.createMetricCard(
                "Reimbursable rate",
                String.format("%.0f%%", reimbursableRate),
                totalReimbursableTrips + " of " + totalTrips + " trips",
                VaadinIcon.TRENDING_UP,
                "metric-card",
                "metric-card--orange"
        ));

        add(metricsGrid);
    }
}
