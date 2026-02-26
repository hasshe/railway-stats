package com.hs.railway_stats.view.component;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.Getter;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TripInfoGrid extends VerticalLayout {

    private static final int REIMBURSABLE_MINUTES_THRESHOLD = 20;

    @Getter
    private final Grid<TripInfoResponse> grid = new Grid<>(TripInfoResponse.class);
    private final List<TripInfoResponse> allTrips = new ArrayList<>();
    public final Checkbox reimbursableFilter = new Checkbox("Claimable", true);

    public TripInfoGrid() {
        setPadding(false);
        setSpacing(false);

        formatGrid();
        styleGrid();
        reimbursableFilter.addValueChangeListener(event -> applyFilter());

        add(grid);
        setFlexGrow(1, grid);
    }

    public void setTrips(List<TripInfoResponse> trips) {
        allTrips.clear();
        allTrips.addAll(trips);
        applyFilter();
    }

    public void applyFilter() {
        if (reimbursableFilter.getValue()) {
            List<TripInfoResponse> claimable = allTrips.stream()
                    .filter(t -> t.isCancelled() || t.totalMinutesLate() >= REIMBURSABLE_MINUTES_THRESHOLD)
                    .toList();
            grid.setEmptyStateText(allTrips.isEmpty()
                    ? "No trips found for the selected route and date."
                    : "No claimable trips found. All trains were on time!");
            grid.setItems(claimable);
        } else {
            grid.setEmptyStateText("No trips found for the selected route and date.");
            grid.setItems(allTrips);
        }
    }

    private void styleGrid() {
        grid.getStyle()
                .set("--lumo-base-color", "#ffffff")
                .set("--lumo-body-text-color", "#000000")
                .set("--lumo-secondary-text-color", "#333333")
                .set("--lumo-contrast-90pct", "#000000")
                .set("--lumo-contrast-70pct", "#333333")
                .set("--lumo-contrast-60pct", "#444444")
                .set("background", "#ffffff")
                .set("color", "#000000")
                .set("border-radius", "8px")
                .set("border", "1px solid rgba(106, 163, 255, 0.30)");
    }

    private void formatGrid() {
        grid.removeAllColumns();
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

        grid.addColumn(trip -> trip.initialDepartureTime() != null
                        ? trip.initialDepartureTime().format(timeFmt) : "N/A")
                .setHeader("Departure");

        grid.addColumn(trip -> trip.actualArrivalTime() != null
                        ? trip.actualArrivalTime().format(timeFmt) : "N/A")
                .setHeader("Arrival");

        grid.addColumn(TripInfoResponse::totalMinutesLate).setHeader("Min. Late");
        grid.addColumn(trip -> trip.isCancelled() ? "Yes" : "No").setHeader("Cancelled");
    }
}

