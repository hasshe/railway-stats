package com.hs.railway_stats.view.component;

import com.hs.railway_stats.config.StationConstants;
import com.hs.railway_stats.exception.TripCollectionException;
import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TripInfoService;
import com.hs.railway_stats.view.util.VaadinRequestUtils;
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

import java.time.LocalDate;

public class InputLayout extends VerticalLayout {

    private static final String CSS_ROUTE_SUB        = "route-selector__sub";
    private static final String CSS_ROUTE_STATION    = "route-selector__station";
    private static final String CSS_ROUTE_BLOCK      = "route-selector__block";
    private static final String CSS_ROUTE_ROW        = "route-selector-row";
    private static final String CSS_SWAP_BTN         = "metrics-swap-btn";

    private final String[] stations = {StationConstants.UPPSALA, StationConstants.STOCKHOLM};
    private int idx = 0;

    private final Span originSpan;
    private final Span destSpan;
    private final DatePicker dateFilter;

    private final TripInfoService tripInfoService;
    private final TripInfoCard tripInfoCard;
    private final RateLimiterService rateLimiterService;

    private Runnable onRouteChange = () -> {};

    public InputLayout(TripInfoService tripInfoService, TripInfoCard tripInfoCard,
                       AdminControls adminControls,
                       RateLimiterService rateLimiterService, ScheduledJobTimer scheduledJobTimer) {

        this.tripInfoService  = tripInfoService;
        this.tripInfoCard     = tripInfoCard;
        this.rateLimiterService = rateLimiterService;

        setPadding(false);
        setSpacing(true);
        setWidthFull();

        originSpan = new Span(stations[idx]);
        destSpan   = new Span(getDestination());
        dateFilter = new DatePicker("Date:");
        dateFilter.setMax(LocalDate.now().minusDays(1));

        adminControls.setOnAdminModeEnabled(() -> setAdminMode(true));
        adminControls.setOnAdminModeDisabled(() -> setAdminMode(false));

        add(buildRouteSelector(), buildFormControls(adminControls, scheduledJobTimer));
    }

    /** Register a callback that fires whenever origin or destination changes. */
    public void setOnRouteChange(Runnable callback) {
        this.onRouteChange = callback != null ? callback : () -> {};
    }

    public void setAdminMode(boolean admin) {
        dateFilter.setMax(admin ? null : LocalDate.now().minusDays(1));
    }

    public String getOrigin() {
        return stations[idx];
    }

    public String getDestination() {
        return stations[1 - idx];
    }

    public Runnable buildCollectRunnable() {
        return () -> {
            String origin = getOrigin();
            String destination = getDestination();
            try {
                tripInfoService.collectTripInformation(origin, destination);
                Notification.show("Trip information collected for " + origin + " → " + destination, 3000, Position.TOP_CENTER);
                refreshGrid();
            } catch (TripCollectionException e) {
                Notification notification = Notification.show("Could not collect trip data. Please try again later.", 4000, Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception e) {
                Notification notification = Notification.show("An unexpected error occurred while collecting trip data.", 4000, Position.TOP_CENTER);
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        };
    }

    public Runnable buildClearDateRunnable() {
        return () -> {
            LocalDate selectedDate = getSelectedDate();
            tripInfoService.deleteTripsByDate(selectedDate);
            Notification.show("Cleared all trip records for " + selectedDate);
            refreshGrid();
        };
    }

    private HorizontalLayout buildRouteSelector() {
        Span fromLabel = new Span("From");
        fromLabel.addClassName(CSS_ROUTE_SUB);
        originSpan.addClassName(CSS_ROUTE_STATION);
        Div originBlock = new Div(fromLabel, originSpan);
        originBlock.addClassName(CSS_ROUTE_BLOCK);

        Icon swapIcon = new Icon(VaadinIcon.ARROWS_LONG_H);
        swapIcon.getStyle().set("color", "#4caf7d");
        Button swapButton = new Button(swapIcon);
        swapButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        swapButton.addClassName(CSS_SWAP_BTN);
        swapButton.getElement().setAttribute("aria-label", "Swap stations");
        swapButton.addClickListener(e -> {
            idx = 1 - idx;
            originSpan.setText(getOrigin());
            destSpan.setText(getDestination());
            refreshGrid();
            onRouteChange.run();
        });

        Span toLabel = new Span("To");
        toLabel.addClassName(CSS_ROUTE_SUB);
        destSpan.addClassName(CSS_ROUTE_STATION);
        Div destBlock = new Div(toLabel, destSpan);
        destBlock.addClassName(CSS_ROUTE_BLOCK);

        HorizontalLayout row = new HorizontalLayout(originBlock, swapButton, destBlock);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        row.setSpacing(true);
        row.setWidthFull();
        row.addClassName(CSS_ROUTE_ROW);
        return row;
    }

    private FormLayout buildFormControls(AdminControls adminControls, ScheduledJobTimer scheduledJobTimer) {
        dateFilter.addValueChangeListener(event -> refreshGrid());
        dateFilter.setWidthFull();
        tripInfoCard.reimbursableFilter.setWidthFull();

        FormLayout form = new FormLayout();
        form.setWidthFull();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2),
                new FormLayout.ResponsiveStep("800px", 4)
        );
        form.add(dateFilter, tripInfoCard.reimbursableFilter, scheduledJobTimer, adminControls);
        form.setColspan(scheduledJobTimer, 2);
        form.setColspan(adminControls, 4);
        return form;
    }

    private void refreshGrid() {
        String ip = VaadinRequestUtils.getClientIp();
        if (!rateLimiterService.tryConsume(ip)) {
            long remaining = rateLimiterService.getRemainingBlockSeconds(ip);
            Notification notification = Notification.show(
                    "Too many requests. Please wait " + (remaining / 60) + " min " + (remaining % 60) + " sec before trying again.");
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        try {
            tripInfoCard.setTrips(tripInfoService.getTripInfo(getOrigin(), getDestination(), getSelectedDate()));
        } catch (TripCollectionException e) {
            Notification notification = Notification.show("Could not load trips. Please try again later.", 4000, Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception e) {
            Notification notification = Notification.show("An unexpected error occurred while loading trips.", 4000, Position.TOP_CENTER);
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private LocalDate getSelectedDate() {
        return dateFilter.getValue() != null ? dateFilter.getValue() : LocalDate.now();
    }
}
