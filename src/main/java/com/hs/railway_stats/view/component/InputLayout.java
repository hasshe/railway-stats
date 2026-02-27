package com.hs.railway_stats.view.component;

import com.hs.railway_stats.config.StationConstants;
import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TripInfoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.VaadinRequest;

import java.time.LocalDate;

public class InputLayout extends FormLayout {

    private final ComboBox<String> originField;
    private final ComboBox<String> destinationField;
    private final DatePicker dateFilter;

    public InputLayout(TripInfoService tripInfoService, TripInfoGrid tripInfoGrid,
                       AdminControls adminControls,
                       RateLimiterService rateLimiterService, ScheduledJobTimer scheduledJobTimer) {

        originField = new ComboBox<>("From:");
        originField.setItems(StationConstants.ALL_STATIONS);
        originField.setValue(StationConstants.UPPSALA);

        destinationField = new ComboBox<>("To:");
        destinationField.setItems(StationConstants.ALL_STATIONS);
        destinationField.setValue(StationConstants.STOCKHOLM);

        dateFilter = new DatePicker("Date:");
        dateFilter.setMax(LocalDate.now());
        //dateFilter.setValue(LocalDate.now());

        Button swapButton = getSwapButton(tripInfoService, tripInfoGrid, rateLimiterService);

        originField.addValueChangeListener(event -> refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService));
        destinationField.addValueChangeListener(event -> refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService));
        dateFilter.addValueChangeListener(event -> refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService));

        HorizontalLayout dateAndFilterRow = new HorizontalLayout(dateFilter, tripInfoGrid.reimbursableFilter);
        dateAndFilterRow.setAlignItems(FlexComponent.Alignment.END);
        dateAndFilterRow.setSpacing(true);

        setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("800px", 4)
        );

        add(originField, swapButton, destinationField);
        add(dateAndFilterRow, scheduledJobTimer, adminControls);
        setColspan(dateAndFilterRow, 4);
        setColspan(adminControls, 4);
    }

    private Button getSwapButton(TripInfoService tripInfoService, TripInfoGrid tripInfoGrid, RateLimiterService rateLimiterService) {
        Button swapButton = new Button("Swap", new Icon(VaadinIcon.ARROWS_LONG_H), clickEvent -> {
            String temp = originField.getValue();
            originField.setValue(destinationField.getValue());
            destinationField.setValue(temp);
            refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService);
        });
        swapButton.addThemeVariants(ButtonVariant.LUMO_SMALL);
        swapButton.addClassName("swap-button");
        swapButton.setWidth("auto");
        swapButton.getStyle()
                .set("align-self", "flex-end")
                .set("max-width", "fit-content")
                .set("white-space", "nowrap")
                .set("background", "rgba(76, 175, 125, 0.12)")
                .set("color", "#4caf7d")
                .set("border", "1px solid rgba(76, 175, 125, 0.35)")
                .set("border-radius", "10px")
                .set("transition", "background 0.2s ease, box-shadow 0.2s ease");
        return swapButton;
    }

    public Runnable buildCollectRunnable(TripInfoService tripInfoService, TripInfoGrid tripInfoGrid,
                                         RateLimiterService rateLimiterService) {
        return () -> {
            String origin = originField.getValue();
            String destination = destinationField.getValue();
            if (origin == null || destination == null) {
                Notification.show("Please select both stations");
                return;
            }
            tripInfoService.collectTripInformation(origin, destination);
            Notification.show("Trip information collection started for " + origin + " to " + destination);
            refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService);
        };
    }

    public Runnable buildClearDateRunnable(TripInfoService tripInfoService, TripInfoGrid tripInfoGrid,
                                           RateLimiterService rateLimiterService) {
        return () -> {
            LocalDate selectedDate = dateFilter.getValue() != null ? dateFilter.getValue() : LocalDate.now();
            tripInfoService.deleteTripsByDate(selectedDate);
            Notification.show("Cleared all trip records for " + selectedDate);
            refreshGrid(tripInfoService, tripInfoGrid, rateLimiterService);
        };
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
