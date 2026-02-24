package com.hs.railway_stats.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.service.TripInfoService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
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

        Icon profileIcon = new Icon(VaadinIcon.USER);
        profileIcon.setSize("2rem");
        Button profileButton = new Button(profileIcon);
        profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE);
        profileButton.getElement().setAttribute("aria-label", "Profile");
        profileButton.addClickListener(clickEvent -> buildProfileDialog().open());

        HorizontalLayout headerRow = new HorizontalLayout(new H1("Trip Information"), profileButton);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(Alignment.CENTER);

        add(headerRow);

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
        return new Button("⇄ Swap", clickEvent -> {
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
        return new Button("Search", clickEvent -> refreshGrid(tripInfoService, originField, destinationField, dateFilter));
    }

    private Button getAdminCollectButton(final TripInfoService tripInfoService, ComboBox<String> originField,
                                         ComboBox<String> destinationField, DatePicker dateFilter) {
        Button collectButton = new Button("🔄 Collect (Admin)");
        collectButton.addClickListener(clickEvent -> {
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

    private Dialog buildProfileDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Profile");
        dialog.setWidth("480px");

        TextField firstNameField = new TextField("First Name");
        firstNameField.setPlaceholder("John");
        firstNameField.setWidthFull();

        TextField lastNameField = new TextField("Last Name");
        lastNameField.setPlaceholder("Doe");
        lastNameField.setWidthFull();

        TextField phoneField = new TextField("Phone Number");
        phoneField.setPlaceholder("+46 70 000 00 00");
        phoneField.setWidthFull();

        TextField emailField = new TextField("Email Address");
        emailField.setPlaceholder("you@example.com");
        emailField.setWidthFull();

        TextField addressField = new TextField("Home Address");
        addressField.setPlaceholder("123 Main Street");
        addressField.setWidthFull();

        TextField cityField = new TextField("City");
        cityField.setPlaceholder("Stockholm");
        cityField.setWidthFull();

        TextField postalCodeField = new TextField("Postal Code");
        postalCodeField.setPlaceholder("111 22");
        postalCodeField.setWidthFull();

        FormLayout form = new FormLayout(firstNameField, lastNameField, phoneField, emailField, addressField, cityField, postalCodeField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        loadProfileFromStorage(firstNameField, lastNameField, phoneField,
                emailField, addressField, cityField, postalCodeField);

        Button saveButton = getSaveUserInfoDialogButton(firstNameField, lastNameField, phoneField, emailField, addressField, cityField, postalCodeField, dialog);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = getCancelUserInfoDialogButton(dialog);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.add(form);
        dialog.getFooter().add(cancelButton, saveButton);
        return dialog;
    }

    private static Button getCancelUserInfoDialogButton(Dialog dialog) {
        return new Button("Cancel", clickEvent -> dialog.close());
    }

    private static Button getSaveUserInfoDialogButton(TextField firstNameField, TextField lastNameField, TextField phoneField, TextField emailField, TextField addressField, TextField cityField, TextField postalCodeField, Dialog dialog) {
        return new Button("Save", clickEvent -> {
            saveProfileToStorage(
                    firstNameField.getValue(), lastNameField.getValue(),
                    phoneField.getValue(), emailField.getValue(),
                    addressField.getValue(), cityField.getValue(),
                    postalCodeField.getValue()
            );
            Notification.show("Profile saved");
            dialog.close();
        });
    }

    private static void saveProfileToStorage(String firstName, String lastName,
                                             String phone, String email,
                                             String address, String city,
                                             String postalCode) {
        UI.getCurrent().getPage().executeJs("""
                const profile = JSON.stringify({
                    firstName: $0, lastName: $1,
                    phone: $2, email: $3,
                    address: $4, city: $5,
                    postalCode: $6
                });
                localStorage.setItem('userProfile', btoa(unescape(encodeURIComponent(profile))));
                """, firstName, lastName, phone, email, address, city, postalCode);
    }

    private static void loadProfileFromStorage(TextField firstNameField, TextField lastNameField,
                                               TextField phoneField, TextField emailField,
                                               TextField addressField, TextField cityField,
                                               TextField postalCodeField) {
        UI.getCurrent().getPage().executeJs("""
                const raw = localStorage.getItem('userProfile');
                if (!raw) return '';
                try {
                    return decodeURIComponent(escape(atob(raw)));
                } catch(e) { return ''; }
                """).then(String.class, json -> {
            if (json == null || json.isBlank()) return;
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);
                firstNameField.setValue(node.path("firstName").asText(""));
                lastNameField.setValue(node.path("lastName").asText(""));
                phoneField.setValue(node.path("phone").asText(""));
                emailField.setValue(node.path("email").asText(""));
                addressField.setValue(node.path("address").asText(""));
                cityField.setValue(node.path("city").asText(""));
                postalCodeField.setValue(node.path("postalCode").asText(""));
            } catch (Exception ignored) {
            }
        });
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

        editButton.addClickListener(clickEvent -> {
            ticketField.setReadOnly(false);
            ticketField.focus();
            editButton.setVisible(false);
            saveButton.setVisible(true);
        });

        saveButton.addClickListener(clickEvent -> {
            String value = ticketField.getValue();
            setTicketNumberCookie(value);
            ticketField.setReadOnly(true);
            saveButton.setVisible(false);
            editButton.setVisible(true);
            Notification.show("Ticket number saved");
        });

        fetchTicketNumberCookie(ticketField);

        HorizontalLayout layout = new HorizontalLayout(ticketField, editButton, saveButton);
        layout.setAlignItems(Alignment.BASELINE);
        return layout;
    }

    private static void setTicketNumberCookie(String value) {
        UI.getCurrent().getPage().executeJs(
                "document.cookie = 'ticketNumber=' + encodeURIComponent($0) + '; path=/; max-age=' + (365*24*60*60);",
                value);
    }

    private static void fetchTicketNumberCookie(TextField ticketField) {
        UI.getCurrent().getPage().executeJs(
                        "const match = document.cookie.split('; ').find(r => r.startsWith('ticketNumber='));" +
                                "return match ? decodeURIComponent(match.split('=')[1]) : '';")
                .then(String.class, cookieValue -> {
                    if (cookieValue != null && !cookieValue.isBlank()) {
                        ticketField.setValue(cookieValue);
                    }
                });
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
