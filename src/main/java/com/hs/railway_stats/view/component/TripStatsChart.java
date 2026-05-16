package com.hs.railway_stats.view.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hs.railway_stats.repository.entity.TripInfoMetric;
import com.hs.railway_stats.service.TripInfoMetricService;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.Div;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * A Vaadin web component wrapper around a Chart.js bar chart that shows
 * trip lateness and cancellation metrics per scheduled departure time.
 * Supports displaying multiple metrics in a single chart with a legend.
 *
 * <p>Usage:
 * <pre>
 *   TripStatsChart chart = new TripStatsChart(tripInfoService);
 *   chart.loadMetrics("Uppsala C", "Stockholm C", EnumSet.of(ChartType.CANCELLATIONS, ChartType.CLAIMS, ChartType.REIMBURSABLE));
 *   add(chart);
 * </pre>
 */
@Tag("trip-stats-chart")
@NpmPackage(value = "chart.js", version = "4.4.3")
@JsModule("./trip-stats-chart.js")
public class TripStatsChart extends Div {

    @Getter
    public enum ChartType {
        CANCELLATIONS("Times Cancelled", "#e84b4b"),
        CLAIMS("Claims Requested", "#4caf7d"),
        REIMBURSABLE("Total Reimbursable Trips", "#2196f3");

        private final String label;
        private final String color;

        ChartType(String label, String color) {
            this.label = label;
            this.color = color;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(TripStatsChart.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TripInfoMetricService tripInfoMetricService;
    private final Set<ChartType> chartTypes;

    // Constructor for combined chart (all types)
    public TripStatsChart(TripInfoMetricService tripInfoMetricService) {
        this(tripInfoMetricService, EnumSet.of(ChartType.CANCELLATIONS, ChartType.CLAIMS, ChartType.REIMBURSABLE));
    }

    // Constructor for single type (backward compatible)
    public TripStatsChart(TripInfoMetricService tripInfoMetricService, ChartType chartType) {
        this(tripInfoMetricService, EnumSet.of(chartType));
    }

    // Constructor for custom set of types
    public TripStatsChart(TripInfoMetricService tripInfoMetricService, Set<ChartType> chartTypes) {
        this.tripInfoMetricService = tripInfoMetricService;
        this.chartTypes = chartTypes;
        getStyle()
                .set("display", "block")
                .set("width", "100%");
    }

    public void loadMetrics(String originStationName, String destinationStationName) {
        loadMetrics(originStationName, destinationStationName, Set.of());
    }

    public void loadMetrics(String originStationName, String destinationStationName, Set<LocalTime> filter) {
        try {
            List<TripInfoMetric> metrics = tripInfoMetricService
                    .getMetrics(originStationName, destinationStationName)
                    .stream()
                    .filter(m -> filter.isEmpty() || filter.contains(m.getScheduledDepartureTime()))
                    .sorted(Comparator.comparing(TripInfoMetric::getScheduledDepartureTime))
                    .toList();

            String json = MAPPER.writeValueAsString(buildChartData(metrics));

            getElement().executeJs(
                    "customElements.whenDefined('trip-stats-chart').then(() => { this.chartData = JSON.parse($0); })",
                    json
            );

        } catch (Exception e) {
            log.error("Failed to load metrics for {} → {}: {}", originStationName, destinationStationName, e.getMessage(), e);
        }
    }

    private ObjectNode buildChartData(List<TripInfoMetric> metrics) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "bar");

        ArrayNode labels = root.putArray("labels");
        for (TripInfoMetric m : metrics) {
            labels.add(m.getScheduledDepartureTime().format(TIME_FMT));
        }

        ArrayNode datasets = root.putArray("datasets");

        // Create a dataset for each chart type
        for (ChartType type : chartTypes) {
            ObjectNode ds = datasets.addObject();
            ArrayNode data = MAPPER.createArrayNode();

            for (TripInfoMetric m : metrics) {
                data.add((int) getDataForType(m, type));
            }

            ds.put("label", type.getLabel());
            ds.set("data", data);
            ds.put("color", type.getColor());
            ds.put("fill", false);
        }

        return root;
    }

    private long getDataForType(TripInfoMetric m, ChartType type) {
        return switch (type) {
            case CANCELLATIONS -> m.getCanceledTripDates() != null ? m.getCanceledTripDates().size() : 0;
            case CLAIMS -> m.getTotalReimbursementsRequested();
            case REIMBURSABLE -> m.getTotalReimbursableTrips();
        };
    }
}
