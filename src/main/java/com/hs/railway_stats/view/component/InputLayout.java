package com.hs.railway_stats.view.component;

import com.hs.railway_stats.config.StationConstants;
import com.hs.railway_stats.exception.TripCollectionException;
import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TripInfoService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinRequest;

import java.time.LocalDate;

public class InputLayout extends VerticalLayout {

    private final String[] stations = {StationConstants.UPPSALA, StationConstants.STOCKHOLM};
    private final int[] idx = {0};
    private final Span originSpan;
    private final Span destSpan;
    private final DatePicker dateFilter;
    private Runnable onRouteChange = () -> {};

    public InputLayout(TripInfoService tripInfoService, TripInfoCard tripInfoCard,
                       AdminControls adminControls,
                       RateLimiterService rateLimiterService, ScheduledJobTimer scheduledJobTimer) {

        setPadding(false);
        setSpacing(true);
        setWidthFull();

        // ── Route selector row (matches MetricsView style) ──
        Span fromLabel = new Span("From");
        fromLabel.addClassName("route-selector__sub");
        originSpan = new Span(stations[idx[0]]);
        originSpan.addClassName("route-selector__station");
        Div originBlock = new Div(fromLabel, originSpan);
        originBlock.addClassName("route-selector__block");

        Icon swapIcon = new Icon(VaadinIcon.ARROWS_LONG_H);
        swapIcon.getStyle().set("color", "#4caf7d");
        Button swapButton = new Button(swapIcon);
        swapButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        swapButton.addClassName("metrics-swap-btn");
        swapButton.getElement().setAttribute("aria-label", "Swap stations");

        Span toLabel = new Span("To");
        toLabel.addClassName("route-selector__sub");
        destSpan = new Span(stations[1 - idx[0]]);
        destSpan.addClassName("route-selector__station");
        Div destBlock = new Div(toLabel, destSpan);
        destBlock.addClassName("route-selector__block");

        HorizontalLayout selectorRow = new HorizontalLayout(originBlock, swapButton, destBlock);
        selectorRow.setAlignItems(FlexComponent.Alignment.CENTER);
        selectorRow.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        selectorRow.setSpacing(true);
        selectorRow.setWidthFull();
        selectorRow.addClassName("route-selector-row");

        swapButton.addClickListener(e -> {
            idx[0] = 1 - idx[0];
            originSpan.setText(stations[idx[0]]);
            destSpan.setText(stations[1 - idx[0]]);
            refreshGrid(tripInfoService, tripInfoCard, rateLimiterService);
            onRouteChange.run();
        });

        // ── Date / filter / admin controls ──
        dateFilter = new DatePicker("Date:");
        dateFilter.setMax(LocalDate.now());
        dateFilter.addValueChangeListener(event -> refreshGrid(tripInfoService, tripInfoCard, rateLimiterService));

        HorizontalLayout dateAndFilterRow = new HorizontalLayout(dateFilter, tripInfoCard.reimbursableFilter);
        dateAndFilterRow.setAlignItems(FlexComponent.Alignment.END);
        dateAndFilterRow.setSpacing(true);

        FormLayout formControls = new FormLayout();
        formControls.setWidthFull();
        formControls.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("800px", 4)
        );
        formControls.add(dateAndFilterRow, scheduledJobTimer, adminControls);
        formControls.setColspan(dateAndFilterRow, 4);
        formControls.setColspan(adminControls, 4);

        add(selectorRow, formControls);
    }

    /** Register a callback that fires whenever origin or destination changes. */
    public void setOnRouteChange(Runnable callback) {
        this.onRouteChange = callback != null ? callback : () -> {};
    }

    public String getOrigin() {
        return stations[idx[0]];
    }

    public String getDestination() {
        return stations[1 - idx[0]];
    }

    public Runnable buildCollectRunnable(TripInfoService tripInfoService, TripInfoCard tripInfoCard,
                                         RateLimiterService rateLimiterService) {
        return () -> {
            String origin = getOrigin();
            String destination = getDestination();
            try {
                tripInfoService.collectTripInformation(origin, destination);
                Notification.show("Trip information collected for " + origin + " → " + destination, 3000, Position.TOP_CENTER);
                refreshGrid(tripInfoService, tripInfoCard, rateLimiterService);
            } catch (TripCollectionException e) {
                Notification notification = Notification.show("Could not collect trip data. Please try again later.", 4000, Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception e) {
                Notification notification = Notification.show("An unexpected error occurred while collecting trip data.", 4000, Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        };
    }

    public Runnable buildClearDateRunnable(TripInfoService tripInfoService, TripInfoCard tripInfoCard,
                                           RateLimiterService rateLimiterService) {
        return () -> {
            LocalDate selectedDate = dateFilter.getValue() != null ? dateFilter.getValue() : LocalDate.now();
            tripInfoService.deleteTripsByDate(selectedDate);
            Notification.show("Cleared all trip records for " + selectedDate);
            refreshGrid(tripInfoService, tripInfoCard, rateLimiterService);
        };
    }

    private void refreshGrid(TripInfoService tripInfoService, TripInfoCard tripInfoCard,
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
            String origin = getOrigin();
            String destination = getDestination();

            LocalDate selectedDate = dateFilter.getValue() != null ? dateFilter.getValue() : LocalDate.now();
            tripInfoCard.setTrips(tripInfoService.getTripInfo(origin, destination, selectedDate));
        } catch (TripCollectionException e) {
            Notification notification = Notification.show("Could not load trips. Please try again later.", 4000, Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification notification = Notification.show("An unexpected error occurred while loading trips.", 4000, Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
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
