package com.hs.railway_stats.view;

import com.hs.railway_stats.service.ClaimsService;
import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TranslationService;
import com.hs.railway_stats.service.TripInfoService;
import com.hs.railway_stats.view.component.AdminBanner;
import com.hs.railway_stats.view.component.AdminControls;
import com.hs.railway_stats.view.component.GitHubLink;
import com.hs.railway_stats.view.component.InputLayout;
import com.hs.railway_stats.view.component.ProfileDrawer;
import com.hs.railway_stats.view.component.ScheduledJobTimer;
import com.hs.railway_stats.view.component.TripInfoCard;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

import java.util.List;

@Route("")
@CssImport("./themes/railway-stats/styles.css")
public class TripInfoView extends VerticalLayout {

    public TripInfoView(final TripInfoService tripInfoService,
                        @Value("${app.crypto.secret}") String cryptoSecret,
                        @Value("${app.crypto.salt}") String cryptoSalt,
                        @Value("${app.admin.password}") String adminPassword,
                        @Value("${app.admin.username}") String adminUsername,
                        @Value("${app.version}") String appVersion,
                        RateLimiterService rateLimiterService,
                        TranslationService translationService,
                        ClaimsService claimsService,
                        Environment environment) {

        addClassName("trip-info-view");
        setPadding(false);
        setSpacing(true);

        Button profileButton = getProfileButton();

        Icon trainIcon = getTrainIcon();

        H1 heading = getHeading();

        HorizontalLayout titleGroup = getTitleGroup(trainIcon, heading);

        GitHubLink githubLink = new GitHubLink("https://github.com/hasshe/railway-stats.git", appVersion);

        ScheduledJobTimer scheduledJobTimer = new ScheduledJobTimer();

        // Title wrapper — CSS grid centres this column; title wraps naturally inside
        Div titleWrapper = new Div(titleGroup);
        titleWrapper.addClassName("header-title-wrapper");

        // CSS grid header: [profileButton] [titleWrapper] [githubLink]
        HorizontalLayout headerRow = getHeaderRow(profileButton, titleWrapper, githubLink);

        // ── Metrics FAB (sticky bottom-right) ────────────────────
        Button metricsButton = getMetricsButton();

        Div metricsFab = new Div(metricsButton);
        metricsFab.addClassName("metrics-fab");

        TripInfoCard tripInfoCard = new TripInfoCard(cryptoSecret, cryptoSalt, claimsService,
                List.of(environment.getActiveProfiles()).contains("dev"));
        AdminBanner adminBanner = new AdminBanner();

        Runnable[] collectHolder = {() -> {
        }};
        Runnable[] clearDateHolder = {() -> {
        }};
        AdminControls adminControls = new AdminControls(adminBanner, cryptoSecret, cryptoSalt,
                () -> collectHolder[0].run(),
                () -> clearDateHolder[0].run(),
                translationService);

        ProfileDrawer profileDrawer = new ProfileDrawer(cryptoSecret, cryptoSalt, adminControls, adminPassword, adminUsername);
        profileButton.addClickListener(clickEvent -> profileDrawer.open());

        InputLayout inputLayout = getInputLayout(tripInfoService, rateLimiterService, tripInfoCard, adminControls, scheduledJobTimer, collectHolder, clearDateHolder);

        add(profileDrawer, headerRow, adminBanner, inputLayout, tripInfoCard, metricsFab);
        setFlexGrow(1, tripInfoCard);
        setAlignItems(Alignment.CENTER);
        setAlignSelf(Alignment.STRETCH, headerRow, tripInfoCard);
    }

    private static InputLayout getInputLayout(TripInfoService tripInfoService, RateLimiterService rateLimiterService, TripInfoCard tripInfoCard, AdminControls adminControls, ScheduledJobTimer scheduledJobTimer, Runnable[] collectHolder, Runnable[] clearDateHolder) {
        InputLayout inputLayout = new InputLayout(tripInfoService, tripInfoCard, adminControls, rateLimiterService, scheduledJobTimer);
        collectHolder[0] = inputLayout.buildCollectRunnable(tripInfoService, tripInfoCard, rateLimiterService);
        clearDateHolder[0] = inputLayout.buildClearDateRunnable(tripInfoService, tripInfoCard, rateLimiterService);

        inputLayout.setWidthFull();
        inputLayout.setMaxWidth("700px");
        inputLayout.getStyle().set("margin-left", "auto").set("margin-right", "auto");
        return inputLayout;
    }

    private static Button getMetricsButton() {
        Icon metricsIcon = new Icon(VaadinIcon.BAR_CHART);
        metricsIcon.getStyle().set("color", "#fff");
        Button metricsButton = new Button(metricsIcon);
        metricsButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        metricsButton.getElement().setAttribute("aria-label", "Metrics");
        metricsButton.addClassName("metrics-fab-btn");
        metricsButton.addClickListener(e -> UI.getCurrent().navigate("metrics"));
        return metricsButton;
    }

    private static HorizontalLayout getHeaderRow(Button profileButton, Div titleWrapper, GitHubLink githubLink) {
        HorizontalLayout headerRow = new HorizontalLayout(profileButton, titleWrapper, githubLink);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.addClassName("header-row");
        return headerRow;
    }

    private static HorizontalLayout getTitleGroup(Icon trainIcon, H1 heading) {
        HorizontalLayout titleGroup = new HorizontalLayout(trainIcon, heading);
        titleGroup.setAlignItems(Alignment.CENTER);
        titleGroup.setSpacing(true);
        return titleGroup;
    }

    private static H1 getHeading() {
        H1 heading = new H1("Movingo Tracker");
        heading.getStyle()
                .set("color", "#e2ede6")
                .set("font-size", "1.45rem")
                .set("font-weight", "600")
                .set("letter-spacing", "-0.01em")
                .set("margin", "0");
        return heading;
    }

    private static Icon getTrainIcon() {
        Icon trainIcon = new Icon(VaadinIcon.TRAIN);
        trainIcon.setSize("1.9rem");
        trainIcon.getStyle().set("color", "#4caf7d");
        return trainIcon;
    }

    private static Button getProfileButton() {
        Icon profileIcon = new Icon(VaadinIcon.MENU);
        profileIcon.setSize("2rem");
        Button profileButton = new Button(profileIcon);
        profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE);
        profileButton.getElement().setAttribute("aria-label", "Profile");
        return profileButton;
    }
}