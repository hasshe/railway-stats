package com.hs.railway_stats.view;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.service.TripInfoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Route("")
public class TripInfoView extends VerticalLayout {

    private final Grid<TripInfoResponse> grid;

    public TripInfoView(final TripInfoService tripInfoService) {
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

        DatePicker dateFilter = new DatePicker("Filter by Date");
        dateFilter.setMax(LocalDate.now());
        dateFilter.setValue(LocalDate.now());

        Button swapButton = getSwapButton(originField, destinationField);

        Button searchButton = getSearchButton(tripInfoService, originField, destinationField, dateFilter);
        Button adminCollectButton = getAdminCollectButton(tripInfoService, originField, destinationField, dateFilter);

        dateFilter.addValueChangeListener(event ->
                refreshGrid(tripInfoService, originField, destinationField, dateFilter));

        formatGrid();

        HorizontalLayout inputLayout = getInputLayout(originField, destinationField, swapButton, searchButton, adminCollectButton, dateFilter);

        HorizontalLayout ticketLayout = buildTicketLayout();

        add(inputLayout, ticketLayout, grid);
        setFlexGrow(1, grid);
    }

    private Button getSwapButton(ComboBox<String> originField, ComboBox<String> destinationField) {
        return new Button("⇄ Swap", event -> {
            String temp = originField.getValue();
            originField.setValue(destinationField.getValue());
            destinationField.setValue(temp);
        });
    }

    private void refreshGrid(TripInfoService tripInfoService, ComboBox<String> originField,
                             ComboBox<String> destinationField, DatePicker dateFilter) {
        try {
            String originStation = originField.getValue();
            String destinationStation = destinationField.getValue();
            if (originStation == null || destinationStation == null) return;

            LocalDate selectedDate = dateFilter.getValue();
            List<TripInfoResponse> trips = tripInfoService.getTripInfo(
                    originStation.toLowerCase(), destinationStation.toLowerCase(), LocalDate.now());

            if (selectedDate != null) {
                trips = trips.stream()
                        .filter(trip -> trip.initialDepartureTime() != null
                                && trip.initialDepartureTime().toLocalDate().equals(selectedDate))
                        .toList();
            }

            grid.setItems(trips);
        } catch (Exception e) {
            Notification.show("Error filtering trips: " + e.getMessage());
        }
    }

    private Button getSearchButton(final TripInfoService tripInfoService, ComboBox<String> originField,
                                   ComboBox<String> destinationField, DatePicker dateFilter) {
        return new Button("Search", event -> refreshGrid(tripInfoService, originField, destinationField, dateFilter));
    }

    private Button getAdminCollectButton(final TripInfoService tripInfoService, ComboBox<String> originField,
                                         ComboBox<String> destinationField, DatePicker dateFilter) {
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

                refreshGrid(tripInfoService, originField, destinationField, dateFilter);
            } catch (Exception e) {
                Notification.show("Error collecting trip information: " + e.getMessage());
            }
        });
        return collectButton;
    }

    private HorizontalLayout getInputLayout(ComboBox<String> originField, ComboBox<String> destinationField, Button swapButton,
                                            Button searchButton, Button adminCollectButton, DatePicker dateFilter) {
        HorizontalLayout inputLayout =
                new HorizontalLayout(originField, swapButton, destinationField, searchButton, adminCollectButton, dateFilter);
        inputLayout.setAlignItems(Alignment.END);
        return inputLayout;
    }

    private HorizontalLayout buildTicketLayout() {
        TextField ticketField = new TextField("Ticket Number");
        ticketField.setPlaceholder("e.g. B123ABCG6");
        ticketField.setReadOnly(true);

        Button editButton = new Button(new Icon(VaadinIcon.PENCIL));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        editButton.getElement().setAttribute("aria-label", "Edit ticket number");

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setVisible(false);

        editButton.addClickListener(e -> {
            ticketField.setReadOnly(false);
            ticketField.focus();
            editButton.setVisible(false);
            saveButton.setVisible(true);
        });

        saveButton.addClickListener(e -> {
            ticketField.setReadOnly(true);
            saveButton.setVisible(false);
            editButton.setVisible(true);
            Notification.show("Ticket number saved");
        });

        HorizontalLayout layout = new HorizontalLayout(ticketField, editButton, saveButton);
        layout.setAlignItems(Alignment.BASELINE);
        return layout;
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
