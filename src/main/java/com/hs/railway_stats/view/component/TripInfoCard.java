package com.hs.railway_stats.view.component;

import com.hs.railway_stats.dto.ClaimRequest;
import com.hs.railway_stats.dto.TripInfoResponse;
import com.hs.railway_stats.dto.UserProfile;
import com.hs.railway_stats.service.ClaimsService;
import com.hs.railway_stats.view.util.BrowserStorageUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class TripInfoCard extends VerticalLayout {

    private static final Logger log = LoggerFactory.getLogger(TripInfoCard.class);

    private static final int REIMBURSABLE_MINUTES_THRESHOLD = 20;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    public static final String UUID = "00000000-0000-0000-0000-000000000000";

    private final VerticalLayout cardsContainer = new VerticalLayout();
    private final List<TripInfoResponse> allTrips = new ArrayList<>();
    public final Checkbox reimbursableFilter = new Checkbox("Claimable", true);

    private String emptyStateMessage = "No trips found for the selected route and date.";

    private final String cryptoSecret;
    private final String cryptoSalt;
    private final ClaimsService claimsService;

    public TripInfoCard(String cryptoSecret, String cryptoSalt, ClaimsService claimsService) {
        this.cryptoSecret = cryptoSecret;
        this.cryptoSalt = cryptoSalt;
        this.claimsService = claimsService;
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

    private Card buildCard(TripInfoResponse trip) {
        String departure = trip.initialDepartureTime() != null
                ? trip.initialDepartureTime().format(TIME_FMT) : "N/A";
        String arrival = trip.actualArrivalTime() != null
                ? trip.actualArrivalTime().format(TIME_FMT) : "N/A";
        boolean cancelled = Boolean.TRUE.equals(trip.isCancelled());
        int minsLate = trip.totalMinutesLate();

        // ── left info section ──────────────────────────────────────
        Div infoSection = getInfoSection(departure, arrival, cancelled, minsLate);

        // ── card root (Vaadin Card component) ─────────────────────
        Card card = new Card();
        card.addClassName("trip-card"); // Restore previous custom card background and style
        card.getStyle().set("width", "100%");
        card.getStyle().set("boxSizing", "border-box");
        if (cancelled) card.addClassName("trip-card--cancelled");
        else if (minsLate >= REIMBURSABLE_MINUTES_THRESHOLD) card.addClassName("trip-card--late");

        // ── action button (only for reimbursable trips) ────────────
        boolean reimbursable = cancelled || minsLate >= REIMBURSABLE_MINUTES_THRESHOLD;
        if (reimbursable) {
            buildReimbursableCard(trip, infoSection, card);
        } else {
            buildRegularCard(new HorizontalLayout(infoSection), infoSection, card);
        }

        return card;
    }

    private static void buildRegularCard(HorizontalLayout cardRow, Div infoSection, Card card) {
        cardRow.setWidthFull();
        cardRow.setAlignItems(Alignment.CENTER);
        cardRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        cardRow.setFlexGrow(1, infoSection);
        card.add(cardRow);
    }

    private void buildReimbursableCard(TripInfoResponse trip, Div infoSection, Card card) {
        Button actionBtn = new Button("Claim", new Icon(VaadinIcon.CHECK));
        actionBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        actionBtn.addClassName("trip-card-action-btn");
        claimsButtonClickAction(trip, actionBtn);
        buildRegularCard(new HorizontalLayout(infoSection, actionBtn), infoSection, card);
    }

    private static Div getInfoSection(String departure, String arrival, boolean cancelled, int minsLate) {
        Div infoSection = new Div();
        infoSection.addClassName("trip-card-info");

        Div timeRow = new Div();
        timeRow.addClassName("trip-card-time-row");

        Span depSpan = new Span(departure);
        depSpan.addClassName("trip-card-time");

        Span arrow = new Span(new Icon(VaadinIcon.ARROW_RIGHT));
        arrow.addClassName("trip-card-arrow");

        Span arrSpan = new Span(arrival);
        arrSpan.addClassName("trip-card-time");

        timeRow.add(depSpan, arrow, arrSpan);

        Div badgeRow = getBadgeRow(cancelled, minsLate);

        infoSection.add(timeRow, badgeRow);
        return infoSection;
    }

    private void claimsButtonClickAction(TripInfoResponse trip, Button actionBtn) {
        actionBtn.addClickListener(clickEvent -> {
            String storageKey = "userProfile";
            BrowserStorageUtils.encryptedLocalStorageLoad(storageKey, cryptoSecret, cryptoSalt, profileJson -> {
                UserProfile profile = validateAndGetUserProfile(profileJson);
                if (profile == null) return;
                // Build ClaimRequest
                ClaimRequest.Customer customer = getCustomer(profile);
                ClaimRequest.RefundType refundType = new ClaimRequest.RefundType(
                        UUID,
                        "Payment via Swedbank SUS"
                );
                ClaimRequest claimRequest = getClaimRequest(trip, customer, profile, refundType);
                log.info("Submitting claim request: ticketNumber={}, departureStationId={}, arrivalStationId={}, departureDate={}, customer=[{} {}], payoutOption={}",
                        claimRequest.ticketNumber(),
                        claimRequest.departureStationId(),
                        claimRequest.arrivalStationId(),
                        claimRequest.departureDate(),
                        claimRequest.customer().firstName(),
                        claimRequest.customer().surName(),
                        claimRequest.payoutOption());
                try {
                    claimsService.submitClaim(claimRequest);
                    log.info("Claim submitted successfully for ticketNumber={}", claimRequest.ticketNumber());
                    UI.getCurrent().access(() -> {
                        Notification.show("Claim submitted successfully!");
                    });
                } catch (Exception ex) {
                    log.error("Claim submission failed for ticketNumber={}: {}", claimRequest.ticketNumber(), ex.getMessage(), ex);
                    UI.getCurrent().access(() -> {
                        Notification.show("Claim submission failed: " + ex.getMessage());
                    });
                }
            });
        });
    }

    private static UserProfile validateAndGetUserProfile(String profileJson) {
        if (profileJson == null) {
            UI.getCurrent().access(() -> {
                Notification.show("Please set up your profile before claiming a trip.");
            });
            return null;
        }
        UserProfile profile = UserProfile.fromJson(profileJson);
        if (profile == null || !profile.isComplete()) {
            UI.getCurrent().access(() -> {
                Notification.show("Please complete your profile before claiming a trip.");
            });
            return null;
        }
        return profile;
    }

    private static Div getBadgeRow(boolean cancelled, int minsLate) {
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
        return badgeRow;
    }

    private static ClaimRequest getClaimRequest(TripInfoResponse trip, ClaimRequest.Customer customer, UserProfile profile, ClaimRequest.RefundType refundType) {
        String departureDate = trip.initialDepartureTime() != null
                ? trip.initialDepartureTime()
                        .withOffsetSameLocal(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"))
                : null;
        return new ClaimRequest(
                UUID,
                null,
                "SWISH",
                customer,
                profile.ticketNumber(),
                1,
                trip.departureClaimsStationId(),
                trip.arrivalClaimsStationId(),
                departureDate,
                "",
                0,
                0,
                refundType,
                List.of()
        );
    }

    private static ClaimRequest.Customer getCustomer(UserProfile profile) {
        return new ClaimRequest.Customer(
                UUID,
                profile.firstName(),
                profile.lastName(),
                profile.city(),
                profile.postalCode(),
                profile.address(),
                profile.identityNumber(),
                profile.phone(),
                profile.email(),
                true
        );
    }
}
