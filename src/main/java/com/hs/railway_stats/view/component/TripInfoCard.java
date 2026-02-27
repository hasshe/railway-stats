package com.hs.railway_stats.view.component;

import com.hs.railway_stats.dto.TripInfoResponse;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TripInfoCard extends VerticalLayout {

    private static final int REIMBURSABLE_MINUTES_THRESHOLD = 20;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final List<TripInfoResponse> allTrips = new ArrayList<>();
    public final Checkbox reimbursableFilter = new Checkbox("Claimable", true);

    private String emptyStateMessage = "No trips found for the selected route and date.";

    public TripInfoCard() {
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        getStyle().set("min-width", "0").set("overflow", "hidden");

        cardsContainer.setPadding(false);
        cardsContainer.setSpacing(false);
        cardsContainer.setWidthFull();
        cardsContainer.addClassName("trip-cards-container");

        reimbursableFilter.addValueChangeListener(event -> applyFilter());

        add(cardsContainer);
        setFlexGrow(1, cardsContainer);
    }

    public void setTrips(List<TripInfoResponse> trips) {
        allTrips.clear();
        allTrips.addAll(trips);
        applyFilter();
    }

    public void applyFilter() {
        List<TripInfoResponse> displayed;
        if (reimbursableFilter.getValue()) {
            displayed = allTrips.stream()
                    .filter(t -> t.isCancelled() || t.totalMinutesLate() >= REIMBURSABLE_MINUTES_THRESHOLD)
                    .toList();
            emptyStateMessage = allTrips.isEmpty()
                    ? "No trips found for the selected route and date."
                    : "No claimable trips found. All trains were on time!";
        } else {
            displayed = List.copyOf(allTrips);
            emptyStateMessage = "No trips found for the selected route and date.";
        }
        renderCards(displayed);
    }

    private void renderCards(List<TripInfoResponse> trips) {
        cardsContainer.removeAll();
        if (trips.isEmpty()) {
            Span emptyLabel = new Span(emptyStateMessage);
            emptyLabel.addClassName("trip-card-empty");
            cardsContainer.add(emptyLabel);
            return;
        }
        for (TripInfoResponse trip : trips) {
            cardsContainer.add(buildCard(trip));
        }
    }

    private HorizontalLayout buildCard(TripInfoResponse trip) {
        String departure = trip.initialDepartureTime() != null
                ? trip.initialDepartureTime().format(TIME_FMT) : "N/A";
        String arrival = trip.actualArrivalTime() != null
                ? trip.actualArrivalTime().format(TIME_FMT) : "N/A";
        boolean cancelled = Boolean.TRUE.equals(trip.isCancelled());
        int minsLate = trip.totalMinutesLate();

        // ── left info section ──────────────────────────────────────
        Div infoSection = new Div();
        infoSection.addClassName("trip-card-info");

        Div timeRow = new Div();
        timeRow.addClassName("trip-card-time-row");

        Span depSpan = new Span(departure);
        depSpan.addClassName("trip-card-time");

        Span arrow = new Span("→");
        arrow.addClassName("trip-card-arrow");

        Span arrSpan = new Span(arrival);
        arrSpan.addClassName("trip-card-time");

        timeRow.add(depSpan, arrow, arrSpan);

        Div badgeRow = new Div();
        badgeRow.addClassName("trip-card-badge-row");

        if (cancelled) {
            Span cancelBadge = new Span("Cancelled");
            cancelBadge.addClassNames("trip-card-badge", "trip-card-badge--cancelled");
            badgeRow.add(cancelBadge);
        } else if (minsLate > 0) {
            Span lateBadge = new Span(minsLate + " min late");
            lateBadge.addClassNames("trip-card-badge", "trip-card-badge--late");
            badgeRow.add(lateBadge);
        } else {
            Span onTimeBadge = new Span("On time");
            onTimeBadge.addClassNames("trip-card-badge", "trip-card-badge--ontime");
            badgeRow.add(onTimeBadge);
        }

        infoSection.add(timeRow, badgeRow);

        // ── action button ──────────────────────────────────────────
        Button actionBtn = new Button(new Icon(VaadinIcon.ELLIPSIS_DOTS_V));
        actionBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        actionBtn.addClassName("trip-card-action-btn");
        actionBtn.addClickListener(e -> {
            Notification notification = Notification.show(
                    "Trip " + departure + " → " + arrival,
                    3000,
                    Notification.Position.BOTTOM_END
            );
            notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        });

        // ── card wrapper ───────────────────────────────────────────
        HorizontalLayout card = new HorizontalLayout(infoSection, actionBtn);
        card.addClassName("trip-card");
        if (cancelled) card.addClassName("trip-card--cancelled");
        else if (minsLate >= REIMBURSABLE_MINUTES_THRESHOLD) card.addClassName("trip-card--late");
        card.setWidthFull();
        card.setAlignItems(Alignment.CENTER);
        card.setJustifyContentMode(JustifyContentMode.BETWEEN);
        card.setFlexGrow(1, infoSection);

        return card;
    }
}

