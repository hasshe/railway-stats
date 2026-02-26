package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * A small live countdown to the next scheduled data-collection run (23:50:59 Europe/Stockholm).
 * Uses Vaadin UI polling (no server push / Atmosphere required).
 */
public class ScheduledJobTimer extends HorizontalLayout {

    private static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");
    private static final int JOB_HOUR = 23;
    private static final int JOB_MINUTE = 50;
    private static final int JOB_SECOND = 59;
    private static final int POLL_MS = 1000;

    private final Span countdownLabel;

    public ScheduledJobTimer() {
        setAlignItems(Alignment.CENTER);
        setSpacing(false);
        getStyle()
                .set("gap", "5px")
                .set("background", "rgba(76,175,125,0.08)")
                .set("border", "1px solid rgba(76,175,125,0.20)")
                .set("border-radius", "8px")
                .set("padding", "4px 10px")
                .set("white-space", "nowrap");

        Icon clockIcon = new Icon(VaadinIcon.CLOCK);
        clockIcon.setSize("0.85rem");
        clockIcon.getStyle().set("color", "#4caf7d").set("flex-shrink", "0");

        Span prefixLabel = new Span("Next sync: ");
        prefixLabel.getStyle()
                .set("font-size", "0.72rem")
                .set("color", "#8aaa92")
                .set("font-weight", "500");

        countdownLabel = new Span(formatCountdown());
        countdownLabel.getStyle()
                .set("font-size", "0.72rem")
                .set("color", "#4caf7d")
                .set("font-weight", "600")
                .set("font-variant-numeric", "tabular-nums")
                .set("letter-spacing", "0.03em");

        add(clockIcon, prefixLabel, countdownLabel);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        UI ui = attachEvent.getUI();
        // Poll every second; each poll triggers a round-trip where we refresh the label
        ui.setPollInterval(POLL_MS);
        ui.addPollListener(event -> countdownLabel.setText(formatCountdown()));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // Disable polling when the view is closed to avoid unnecessary requests
        detachEvent.getUI().setPollInterval(-1);
    }

    private String formatCountdown() {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime next = now.toLocalDate()
                .atStartOfDay(ZONE)
                .withHour(JOB_HOUR)
                .withMinute(JOB_MINUTE)
                .withSecond(JOB_SECOND)
                .withNano(0);

        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }

        long total = Duration.between(now, next).getSeconds();
        long hours = total / 3600;
        long minutes = (total % 3600) / 60;
        long seconds = total % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
