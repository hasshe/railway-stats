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
    public final Checkbox reimbursableFilter = new Checkbox("Reimbursable only", true);

    public TripInfoGrid() {
        setPadding(false);
        setSpacing(false);

        formatGrid();
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
            grid.setItems(allTrips.stream()
                    .filter(t -> t.isCancelled() || t.totalMinutesLate() >= REIMBURSABLE_MINUTES_THRESHOLD)
                    .toList());
        } else {
            grid.setItems(allTrips);
        }
    }

    private void formatGrid() {
        grid.removeAllColumns();
        grid.addColumn(TripInfoResponse::startDestination).setHeader("Start");
        grid.addColumn(TripInfoResponse::endingDestination).setHeader("End");
        grid.addColumn(trip -> trip.isCancelled() ? "Yes" : "No").setHeader("Cancelled");
        grid.addColumn(TripInfoResponse::totalMinutesLate).setHeader("Minutes Late");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        grid.addColumn(trip -> trip.initialDepartureTime() != null
                        ? trip.initialDepartureTime().format(formatter) : "N/A")
                .setHeader("Departure");
        grid.addColumn(trip -> trip.actualArrivalTime() != null
                        ? trip.actualArrivalTime().format(formatter) : "N/A")
                .setHeader("Arrival");
    }
}

