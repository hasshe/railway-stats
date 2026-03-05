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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * A Vaadin web component wrapper around a Chart.js bar chart that shows
 * trip lateness and cancellation metrics per scheduled departure time.
 *
 * <p>Usage:
 * <pre>
 *   TripStatsChart chart = new TripStatsChart(tripInfoService);
 *   chart.loadMetrics("Uppsala C", "Stockholm C");
 *   add(chart);
 * </pre>
 */
@Tag("trip-stats-chart")
@NpmPackage(value = "chart.js", version = "4.4.3")
@JsModule("./trip-stats-chart.js")
public class TripStatsChart extends Div {

    public enum ChartType {
        AVG_LATE,
        CANCELLATIONS,
        CLAIMS,
        REIMBURSABLE
    }

    private static final Logger log = LoggerFactory.getLogger(TripStatsChart.class);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TripInfoMetricService tripInfoMetricService;
    private final ChartType chartType;

    public TripStatsChart(TripInfoMetricService tripInfoMetricService, ChartType chartType) {
        this.tripInfoMetricService = tripInfoMetricService;
        this.chartType = chartType;
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

            getElement().setAttribute("title", resolveTitle());
            getElement().executeJs(
                    "customElements.whenDefined('trip-stats-chart').then(() => { this.chartData = JSON.parse($0); })",
                    json
            );

        } catch (Exception e) {
            log.error("Failed to load metrics for chart {}: {} → {}", chartType, originStationName, destinationStationName, e);
        }
    }

    private String resolveTitle() {
        return switch (chartType) {
            case AVG_LATE -> "Average Minutes Late";
            case CANCELLATIONS -> "Times Cancelled";
            case CLAIMS -> "Claims Requested";
            case REIMBURSABLE -> "Total Reimbursable Trips";
        };
    }

    private ObjectNode buildChartData(List<TripInfoMetric> metrics) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("type", "bar");

        ArrayNode labels = root.putArray("labels");
        ArrayNode data = MAPPER.createArrayNode();

        for (TripInfoMetric m : metrics) {
            labels.add(m.getScheduledDepartureTime().format(TIME_FMT));
            data.add((int) switch (chartType) {
                case AVG_LATE -> m.getAverageMinutesLate();
                case CANCELLATIONS -> m.getCanceledTripDates() != null ? m.getCanceledTripDates().size() : 0;
                case CLAIMS -> m.getTotalReimbursementsRequested();
                case REIMBURSABLE -> m.getTotalReimbursableTrips();
            });
        }

        ArrayNode datasets = root.putArray("datasets");
        ObjectNode ds = datasets.addObject();

        String color = switch (chartType) {
            case AVG_LATE -> "#e8a56b";
            case CANCELLATIONS -> "#e84b4b";
            case CLAIMS -> "#4caf7d";
            case REIMBURSABLE -> "#2196f3";
        };

        ds.put("label", resolveTitle());
        ds.set("data", data);
        ds.put("color", color);
        ds.put("fill", false);

        return root;
    }
}
