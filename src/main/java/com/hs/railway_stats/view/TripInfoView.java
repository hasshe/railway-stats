package com.hs.railway_stats.view;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.service.TripInfoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("")
public class TripInfoView extends VerticalLayout {

    private final TripInfoService tripInfoService;

    private final Grid<TripInfoResponse> grid;

    public TripInfoView(final TripInfoService tripInfoService) {
        this.tripInfoService = tripInfoService;
        this.grid = new Grid<>(TripInfoResponse.class);

        setPadding(true);
        setSpacing(true);

        add(new com.vaadin.flow.component.html.H1("Trip Information"));

        List<String> stationOptions = Arrays.asList("Uppsala C", "Stockholm C");

        ComboBox<String> originField = new ComboBox<>("Origin Station");
        originField.setItems(stationOptions);
        originField.setValue("Uppsala C");

        ComboBox<String> destinationField = new ComboBox<>("Destination Station");
        destinationField.setItems(stationOptions);
        destinationField.setValue("Stockholm C");

        Button swapButton = getSwapButton(originField, destinationField);

        Button searchButton = getSearchButton(tripInfoService, originField, destinationField);
        Button adminCollectButton = getAdminCollectButton(tripInfoService, originField, destinationField);
        formatGrid();

        HorizontalLayout inputLayout = getInputLayout(originField, destinationField, swapButton, searchButton, adminCollectButton);
        add(inputLayout, grid);
        setFlexGrow(1, grid);
    }

    private Button getSwapButton(ComboBox<String> originField, ComboBox<String> destinationField) {
        return new Button("⇄ Swap", event -> {
            String temp = originField.getValue();
            originField.setValue(destinationField.getValue());
            destinationField.setValue(temp);
        });
    }

    private Button getSearchButton(final TripInfoService tripInfoService, ComboBox<String> originField,
            ComboBox<String> destinationField) {
        return new Button("Search", event -> {
            try {
                String originStation = originField.getValue();
                String destinationStation = destinationField.getValue();
                
                if (originStation == null || destinationStation == null) {
                    Notification.show("Please select both stations");
                    return;
                }
                
                List<TripInfoResponse> trips = tripInfoService.getTripInfo(originStation.toLowerCase(),
                    destinationStation.toLowerCase(), LocalDate.now());
                grid.setItems(trips);
            } catch (Exception e) {
                Notification.show("Error fetching trips: " + e.getMessage());
            }
        });
    }

    private Button getAdminCollectButton(final TripInfoService tripInfoService, ComboBox<String> originField,
            ComboBox<String> destinationField) {
        Button collectButton = new Button("🔄 Collect (Admin)");
        collectButton.addClickListener(event -> {
            try {
                String originStation = originField.getValue();
                String destinationStation = destinationField.getValue();
                
                if (originStation == null || destinationStation == null) {
                    Notification.show("Please select both stations");
                    return;
                }
                
                tripInfoService.collectTripInformation(originStation, destinationStation);
                Notification.show("Trip information collection started for " + originStation + " to " + destinationStation);
                
                // Refresh the grid after collection
                List<TripInfoResponse> trips = tripInfoService.getTripInfo(originStation.toLowerCase(),
                    destinationStation.toLowerCase(), LocalDate.now());
                grid.setItems(trips);
            } catch (Exception e) {
                Notification.show("Error collecting trip information: " + e.getMessage());
            }
        });
        return collectButton;
    }

    private HorizontalLayout getInputLayout(ComboBox<String> originField, ComboBox<String> destinationField, Button swapButton,
            Button searchButton, Button adminCollectButton) {
        HorizontalLayout inputLayout =
            new HorizontalLayout(originField, swapButton, destinationField, searchButton, adminCollectButton);
        inputLayout.setAlignItems(Alignment.END);
        return inputLayout;
    }

    private void formatGrid() {
        grid.removeAllColumns();
        grid.addColumn(TripInfoResponse::startDestination)
            .setHeader("Start");
        grid.addColumn(TripInfoResponse::endingDestination)
            .setHeader("End");
        grid.addColumn(trip -> trip.isCancelled() ? "Yes" : "No")
            .setHeader("Cancelled");
        grid.addColumn(TripInfoResponse::totalMinutesLate)
            .setHeader("Minutes Late");
        DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm");
        grid.addColumn(trip -> trip.initialDepartureTime() != null
            ? trip.initialDepartureTime().format(formatter)
            : "N/A")
            .setHeader("Departure");
        grid.addColumn(trip -> trip.actualArrivalTime() != null
            ? trip.actualArrivalTime().format(formatter)
            : "N/A")
            .setHeader("Arrival");
    }
}
