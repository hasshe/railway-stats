package com.hs.railway_stats.view;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.service.TripInfoService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Route("")
public class TripInfoView extends VerticalLayout {

    private final Grid<TripInfoResponse> grid;
    private final String cryptoSecret;
    private final String cryptoSalt;
    private final String adminPassword;
    private final Span adminBanner;

    public TripInfoView(final TripInfoService tripInfoService,
                        @Value("${app.crypto.secret}") String cryptoSecret,
                        @Value("${app.crypto.salt}") String cryptoSalt,
                        @Value("${app.admin.password}") String adminPassword) {
        this.grid = new Grid<>(TripInfoResponse.class);
        this.cryptoSecret = cryptoSecret;
        this.cryptoSalt = cryptoSalt;
        this.adminPassword = adminPassword;

        this.adminBanner = new Span("🔐 Admin Mode Active");
        adminBanner.getStyle()
                .set("background-color", "var(--lumo-error-color)")
                .set("color", "var(--lumo-error-contrast-color)")
                .set("padding", "0.4em 1em")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)");
        adminBanner.setVisible(false);

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
        add(adminBanner);

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
        adminCollectButton.setVisible(false);

        Button adminToggle = new Button("Toggle Admin Mode");
        adminToggle.addClickListener(clickEvent -> buildAdminPasswordDialog(adminCollectButton, adminBanner).open());

        restoreAdminSession(adminCollectButton, adminBanner, cryptoSecret, cryptoSalt);

        dateFilter.addValueChangeListener(event ->
                refreshGrid(tripInfoService, originField, destinationField, dateFilter));

        formatGrid();

        HorizontalLayout inputLayout = getInputLayout(
                originField, destinationField, swapButton, searchButton,
                adminCollectButton, adminToggle, dateFilter);

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

            LocalDate selectedDate = dateFilter.getValue() != null ? dateFilter.getValue() : LocalDate.now();
            List<TripInfoResponse> trips = tripInfoService.getTripInfo(
                    originStation, destinationStation, selectedDate);

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

    private HorizontalLayout getInputLayout(ComboBox<String> originField, ComboBox<String> destinationField,
                                            Button swapButton, Button searchButton,
                                            Button adminCollectButton, Button adminToggle, DatePicker dateFilter) {
        HorizontalLayout inputLayout =
                new HorizontalLayout(originField, swapButton, destinationField, searchButton, dateFilter, adminToggle, adminCollectButton);
        inputLayout.setAlignItems(Alignment.END);
        return inputLayout;
    }

    private Dialog buildAdminPasswordDialog(Button adminCollectButton, Span adminBanner) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Admin Mode");
        dialog.setWidth("360px");

        PasswordField passwordField = new PasswordField("Admin Password");
        passwordField.setPlaceholder("Enter password");
        passwordField.setWidthFull();

        dialog.addDialogCloseActionListener(closeActionEvent -> dialog.close());

        Button confirmButton = new Button("Confirm", clickEvent -> {
            if (adminPassword.equals(passwordField.getValue())) {
                boolean nowVisible = !adminCollectButton.isVisible();
                adminCollectButton.setVisible(nowVisible);
                adminBanner.setVisible(nowVisible);
                if (nowVisible) {
                    saveAdminSession(cryptoSecret, cryptoSalt);
                    Notification.show("Admin mode enabled");
                } else {
                    clearAdminSession();
                    Notification.show("Admin mode disabled");
                }
            } else {
                Notification.show("Incorrect password");
                passwordField.setInvalid(true);
                passwordField.setErrorMessage("Wrong password");
            }
            dialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", clickEvent -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        passwordField.addKeyDownListener(Key.ENTER,
                keyDownEvent -> confirmButton.click());

        dialog.add(passwordField);
        dialog.getFooter().add(cancelButton, confirmButton);
        return dialog;
    }

    private static void saveAdminSession(String secret, String salt) {
        UI.getCurrent().getPage().executeJs(buildCryptoJs(secret, salt) + """
                (async () => {
                    const key = await deriveKey();
                    const iv  = crypto.getRandomValues(new Uint8Array(12));
                    const enc = await crypto.subtle.encrypt(
                        { name: 'AES-GCM', iv },
                        key,
                        new TextEncoder().encode('admin-authenticated')
                    );
                    localStorage.setItem('adminSession', toHex(iv) + ':' + toHex(enc));
                })();
                """);
    }

    private static void clearAdminSession() {
        UI.getCurrent().getPage().executeJs("localStorage.removeItem('adminSession');");
    }

    private static void restoreAdminSession(Button adminCollectButton, Span adminBanner, String secret, String salt) {
        UI.getCurrent().getPage().executeJs(buildCryptoJs(secret, salt) + """
                return (async () => {
                    const raw = localStorage.getItem('adminSession');
                    if (!raw || !raw.includes(':')) return 'false';
                    try {
                        const [ivHex, dataHex] = raw.split(':');
                        const key = await deriveKey();
                        const dec = await crypto.subtle.decrypt(
                            { name: 'AES-GCM', iv: fromHex(ivHex) },
                            key,
                            fromHex(dataHex)
                        );
                        return new TextDecoder().decode(dec) === 'admin-authenticated' ? 'true' : 'false';
                    } catch(e) { return 'false'; }
                })();
                """).then(String.class, result -> {
            if ("true".equals(result)) {
                adminCollectButton.setVisible(true);
                adminBanner.setVisible(true);
            }
        });
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
                emailField, addressField, cityField, postalCodeField, cryptoSecret, cryptoSalt);

        Button saveButton = getSaveUserInfoDialogButton(firstNameField, lastNameField, phoneField, emailField,
                addressField, cityField, postalCodeField, dialog, cryptoSecret, cryptoSalt);
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

    private static Button getSaveUserInfoDialogButton(TextField firstNameField, TextField lastNameField,
                                                      TextField phoneField, TextField emailField,
                                                      TextField addressField, TextField cityField,
                                                      TextField postalCodeField, Dialog dialog,
                                                      String cryptoSecret, String cryptoSalt) {
        return new Button("Save", clickEvent -> {
            saveProfileToStorage(
                    firstNameField.getValue(), lastNameField.getValue(),
                    phoneField.getValue(), emailField.getValue(),
                    addressField.getValue(), cityField.getValue(),
                    postalCodeField.getValue(), cryptoSecret, cryptoSalt
            );
            Notification.show("Profile saved");
            dialog.close();
        });
    }

    private static String buildCryptoJs(String secret, String salt) {
        return ("""
                const APP_SECRET = '%s';
                const SALT       = new TextEncoder().encode('%s');
                
                async function deriveKey() {
                    const keyMaterial = await crypto.subtle.importKey(
                        'raw', new TextEncoder().encode(APP_SECRET),
                        'PBKDF2', false, ['deriveKey']
                    );
                    return crypto.subtle.deriveKey(
                        { name: 'PBKDF2', salt: SALT, iterations: 100000, hash: 'SHA-256' },
                        keyMaterial,
                        { name: 'AES-GCM', length: 256 },
                        false, ['encrypt', 'decrypt']
                    );
                }
                
                function toHex(buf) {
                    return Array.from(new Uint8Array(buf)).map(b => b.toString(16).padStart(2,'0')).join('');
                }
                
                function fromHex(hex) {
                    const bytes = new Uint8Array(hex.length / 2);
                    for (let i = 0; i < bytes.length; i++) bytes[i] = parseInt(hex.substr(i*2,2),16);
                    return bytes;
                }
                """).formatted(secret, salt);
    }

    private static void saveProfileToStorage(String firstName, String lastName,
                                             String phone, String email,
                                             String address, String city,
                                             String postalCode, String secret, String salt) {
        UI.getCurrent().getPage().executeJs(buildCryptoJs(secret, salt) + """
                (async () => {
                    const profile = JSON.stringify({
                        firstName: $0, lastName: $1,
                        phone: $2, email: $3,
                        address: $4, city: $5,
                        postalCode: $6
                    });
                    const key = await deriveKey();
                    const iv  = crypto.getRandomValues(new Uint8Array(12));
                    const enc = await crypto.subtle.encrypt(
                        { name: 'AES-GCM', iv },
                        key,
                        new TextEncoder().encode(profile)
                    );
                    localStorage.setItem('userProfile', toHex(iv) + ':' + toHex(enc));
                })();
                """, firstName, lastName, phone, email, address, city, postalCode);
    }

    private static void loadProfileFromStorage(TextField firstNameField, TextField lastNameField,
                                               TextField phoneField, TextField emailField,
                                               TextField addressField, TextField cityField,
                                               TextField postalCodeField, String secret, String salt) {
        UI.getCurrent().getPage().executeJs(buildCryptoJs(secret, salt) + """
                return (async () => {
                    const raw = localStorage.getItem('userProfile');
                    if (!raw || !raw.includes(':')) return '';
                    try {
                        const [ivHex, dataHex] = raw.split(':');
                        const key = await deriveKey();
                        const dec = await crypto.subtle.decrypt(
                            { name: 'AES-GCM', iv: fromHex(ivHex) },
                            key,
                            fromHex(dataHex)
                        );
                        return new TextDecoder().decode(dec);
                    } catch(e) { return ''; }
                })();
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
