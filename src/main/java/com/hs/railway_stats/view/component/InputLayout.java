package com.hs.railway_stats.view.component;

import com.hs.railway_stats.config.StationConstants;
import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TripInfoService;
import com.hs.railway_stats.view.util.AdminSessionUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.server.VaadinRequest;

import java.time.LocalDate;

public class InputLayout extends FormLayout {

    private final ComboBox<String> originField;
    private final ComboBox<String> destinationField;
    private final DatePicker dateFilter;

    public InputLayout(TripInfoService tripInfoService, TripInfoGrid tripInfoGrid,
                       AdminBanner adminBanner, String adminPassword,
                       String cryptoSecret, String cryptoSalt,
                       RateLimiterService rateLimiterService) {

        originField = new ComboBox<>("Origin Station");
        originField.setItems(StationConstants.ALL_STATIONS);
        originField.setValue(StationConstants.UPPSALA);

        destinationField = new ComboBox<>("Destination Station");
        destinationField.setItems(StationConstants.ALL_STATIONS);
        destinationField.setValue(StationConstants.STOCKHOLM);

        dateFilter = new DatePicker("Filter by Date");
        dateFilter.setMax(LocalDate.now());
        dateFilter.setValue(LocalDate.now());

        Button swapButton = new Button("⇄ Swap", clickEvent -> {
            String temp = originField.getValue();
            originField.setValue(destinationField.getValue());
            destinationField.setValue(temp);
            refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService);
        });

        Button searchButton = new Button("Search",
                clickEvent -> refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService));

        Button adminCollectButton = new Button("🔄 Collect (Admin)");
        adminCollectButton.setVisible(false);
        adminCollectButton.addClickListener(clickEvent -> {
            try {
                String origin = originField.getValue();
                String destination = destinationField.getValue();
                if (origin == null || destination == null) {
                    Notification.show("Please select both stations");
                    return;
                }
                tripInfoService.collectTripInformation(origin, destination);
                Notification.show("Trip information collection started for " + origin + " to " + destination);
                refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService);
            } catch (Exception e) {
                Notification.show("Error collecting trip information: " + e.getMessage());
            }
        });

        Button adminToggle = new Button("Toggle Admin Mode");
        adminToggle.addClickListener(clickEvent -> new AdminPasswordDialog(
                adminPassword, adminCollectButton, adminBanner,
                () -> AdminSessionUtils.saveAdminSession(cryptoSecret, cryptoSalt),
                AdminSessionUtils::clearAdminSession
        ).open());

        AdminSessionUtils.restoreAdminSession(adminCollectButton, adminBanner, cryptoSecret, cryptoSalt);

        dateFilter.addValueChangeListener(event -> refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService));

        setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("800px", 4)
        );

        add(originField, swapButton, destinationField, searchButton);
        add(dateFilter, tripInfoGrid.reimbursableFilter, adminToggle, adminCollectButton);
    }

    private void refreshGrid(TripInfoService tripInfoService, TripInfoGrid tripInfoGrid,
                              RateLimiterService rateLimiterService) {
        String ip = getClientIp();
        if (!rateLimiterService.tryConsume(ip)) {
            long remaining = rateLimiterService.getRemainingBlockSeconds(ip);
            Notification notification = Notification.show(
                    "Too many requests. Please wait " + (remaining / 60) + " min " + (remaining % 60) + " sec before trying again.");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            String origin = originField.getValue();
            String destination = destinationField.getValue();
            if (origin == null || destination == null) return;

            LocalDate selectedDate = dateFilter.getValue() != null ? dateFilter.getValue() : LocalDate.now();
            tripInfoGrid.setTrips(tripInfoService.getTripInfo(origin, destination, selectedDate));
        } catch (Exception e) {
            Notification.show("Error filtering trips: " + e.getMessage());
        }
    }

    private String getClientIp() {
        VaadinRequest request = VaadinRequest.getCurrent();
        if (request == null) return "unknown";
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
