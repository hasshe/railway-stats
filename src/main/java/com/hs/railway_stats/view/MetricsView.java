package com.hs.railway_stats.view;

import com.hs.railway_stats.config.StationConstants;
import com.hs.railway_stats.service.TripInfoMetricService;
import com.hs.railway_stats.view.component.TripStatsChart;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Route("metrics")
@PageTitle("Metrics – Movingo Tracker")
@CssImport("./themes/railway-stats/styles.css")
public class MetricsView extends VerticalLayout {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public MetricsView(TripInfoMetricService tripInfoMetricService) {

        addClassName("metrics-view");
        setPadding(false);
        setSpacing(true);

        HorizontalLayout headerRow = getHeaderRow();

        final String[] stations = {StationConstants.UPPSALA, StationConstants.STOCKHOLM};
        final int[] idx = {0};

        Span fromLabel = new Span("From");
        fromLabel.addClassName("route-selector__sub");
        Span originSpan = new Span(stations[idx[0]]);
        originSpan.addClassName("route-selector__station");
        Div originBlock = new Div(fromLabel, originSpan);
        originBlock.addClassName("route-selector__block");

        Button swapBtn = getSwapBtn(VaadinIcon.ARROWS_LONG_H, "metrics-swap-btn", "Swap stations");

        Span toLabel = new Span("To");
        toLabel.addClassName("route-selector__sub");
        Span destSpan = new Span(stations[1 - idx[0]]);
        destSpan.addClassName("route-selector__station");
        Div destBlock = new Div(toLabel, destSpan);
        destBlock.addClassName("route-selector__block");

        HorizontalLayout selectorRow = getSelectorRow(originBlock, swapBtn, destBlock);

        MultiSelectComboBox<String> timeFilter = getMultiSelectComboBox();

        Div filterRow = getFilterRow(timeFilter);

        TripStatsChart avgLateChart = new TripStatsChart(tripInfoMetricService, TripStatsChart.ChartType.AVG_LATE);
        TripStatsChart cancelChart = new TripStatsChart(tripInfoMetricService, TripStatsChart.ChartType.CANCELLATIONS);
        TripStatsChart claimsChart = new TripStatsChart(tripInfoMetricService, TripStatsChart.ChartType.CLAIMS);
        TripStatsChart reimbursableChart = new TripStatsChart(tripInfoMetricService, TripStatsChart.ChartType.REIMBURSABLE);

        for (TripStatsChart statsChart : new TripStatsChart[]{avgLateChart, cancelChart, claimsChart, reimbursableChart}) {
            statsChart.setWidthFull();
            statsChart.setMaxWidth("700px");
            statsChart.getStyle().set("margin-left", "auto").set("margin-right", "auto");
        }

        Runnable reloadCharts = getReloadCharts(stations, idx, timeFilter, avgLateChart, cancelChart, claimsChart, reimbursableChart);

        Runnable reloadAll = getReloadAll(tripInfoMetricService, stations, idx, timeFilter, reloadCharts);

        timeFilter.addValueChangeListener(e -> reloadCharts.run());

        swapBtn.addClickListener(clickEvent -> {
            idx[0] = 1 - idx[0];
            originSpan.setText(stations[idx[0]]);
            destSpan.setText(stations[1 - idx[0]]);
            reloadAll.run();
        });

        addAttachListener(event -> reloadAll.run());

        add(headerRow, selectorRow, filterRow, avgLateChart, cancelChart, claimsChart, reimbursableChart);
        setAlignItems(Alignment.CENTER);
        setAlignSelf(Alignment.STRETCH, headerRow, selectorRow, filterRow, avgLateChart, cancelChart, claimsChart, reimbursableChart);
    }

    private static Div getFilterRow(MultiSelectComboBox<String> timeFilter) {
        Div filterRow = new Div(timeFilter);
        filterRow.addClassName("filter-row");
        filterRow.setWidthFull();
        filterRow.getStyle()
                .set("max-width", "700px")
                .set("margin-left", "auto")
                .set("margin-right", "auto");
        return filterRow;
    }

    private static Runnable getReloadAll(TripInfoMetricService tripInfoMetricService, String[] stations, int[] idx, MultiSelectComboBox<String> timeFilter, Runnable reloadCharts) {
        return () -> {
            String origin = stations[idx[0]];
            String dest = stations[1 - idx[0]];
            List<String> times = tripInfoMetricService.getDepartureTimes(origin, dest)
                    .stream()
                    .map(t -> t.format(TIME_FMT))
                    .toList();
            timeFilter.setItems(times);
            timeFilter.clear();
            reloadCharts.run();
        };
    }

    private static Runnable getReloadCharts(String[] stations, int[] idx, MultiSelectComboBox<String> timeFilter, TripStatsChart avgLateChart, TripStatsChart cancelChart, TripStatsChart claimsChart, TripStatsChart reimbursableChart) {
        return () -> {
            String origin = stations[idx[0]];
            String dest = stations[1 - idx[0]];
            Set<LocalTime> selected = timeFilter.getSelectedItems().stream()
                    .map(s -> LocalTime.parse(s, TIME_FMT))
                    .collect(Collectors.toSet());
            avgLateChart.loadMetrics(origin, dest, selected);
            cancelChart.loadMetrics(origin, dest, selected);
            claimsChart.loadMetrics(origin, dest, selected);
            reimbursableChart.loadMetrics(origin, dest, selected);
        };
    }

    private static MultiSelectComboBox<String> getMultiSelectComboBox() {
        MultiSelectComboBox<String> timeFilter = new MultiSelectComboBox<>("Filter by departure time");
        timeFilter.setPlaceholder("All departures");
        timeFilter.setClearButtonVisible(true);
        timeFilter.setWidthFull();
        timeFilter.addClassName("time-filter-combo");
        return timeFilter;
    }

    private static HorizontalLayout getSelectorRow(Div originBlock, Button swapBtn, Div destBlock) {
        HorizontalLayout selectorRow = new HorizontalLayout(originBlock, swapBtn, destBlock);
        selectorRow.setAlignItems(Alignment.CENTER);
        selectorRow.setJustifyContentMode(JustifyContentMode.CENTER);
        selectorRow.setSpacing(true);
        selectorRow.setWidthFull();
        selectorRow.setMaxWidth("700px");
        selectorRow.addClassName("route-selector-row");
        selectorRow.getStyle().set("margin-left", "auto").set("margin-right", "auto");
        return selectorRow;
    }

    private static Button getSwapBtn(VaadinIcon arrowsLongH, String className, String Swap_stations) {
        Icon swapIcon = new Icon(arrowsLongH);
        swapIcon.getStyle().set("color", "#4caf7d");
        Button swapBtn = new Button(swapIcon);
        swapBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        swapBtn.addClassName(className);
        swapBtn.getElement().setAttribute("aria-label", Swap_stations);
        return swapBtn;
    }

    private static HorizontalLayout getHeaderRow() {
        Button backButton = getSwapBtn(VaadinIcon.ARROW_LEFT, "metrics-back-btn", "Back");
        backButton.addClickListener(e -> UI.getCurrent().navigate(""));

        Icon chartIcon = new Icon(VaadinIcon.BAR_CHART);
        chartIcon.setSize("1.6rem");
        chartIcon.getStyle().set("color", "#4caf7d");

        H1 heading = new H1("Departure Statistics");
        heading.getStyle()
                .set("color", "#e2ede6")
                .set("font-size", "1.45rem")
                .set("font-weight", "600")
                .set("letter-spacing", "-0.01em")
                .set("margin", "0");

        HorizontalLayout titleGroup = new HorizontalLayout(chartIcon, heading);
        titleGroup.setAlignItems(Alignment.CENTER);
        titleGroup.setSpacing(true);

        HorizontalLayout headerRow = new HorizontalLayout(backButton, titleGroup);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.setSpacing(true);
        headerRow.getStyle().set("flex-shrink", "0");
        return headerRow;
    }
}
