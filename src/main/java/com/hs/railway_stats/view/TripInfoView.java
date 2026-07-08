package com.hs.railway_stats.view;

import com.hs.railway_stats.dto.UserProfile;
import com.hs.railway_stats.service.ClaimsService;
import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TranslationService;
import com.hs.railway_stats.service.TripInfoMetricService;
import com.hs.railway_stats.service.TripInfoService;
import com.hs.railway_stats.view.component.AdminBanner;
import com.hs.railway_stats.view.component.AdminControls;
import com.hs.railway_stats.view.component.GitHubLink;
import com.hs.railway_stats.view.component.InputLayout;
import com.hs.railway_stats.view.component.ProfileDrawer;
import com.hs.railway_stats.view.component.ProfileSetupBanner;
import com.hs.railway_stats.view.component.ScheduledJobTimer;
import com.hs.railway_stats.view.component.TripInfoCard;
import com.hs.railway_stats.view.util.BrowserStorageUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

@Route("")
@CssImport("./themes/railway-stats/styles.css")
public class TripInfoView extends VerticalLayout {

    private static final String REMOVE_RIPPLE_JS =
            "this.querySelectorAll('.profile-btn-ripple-ring').forEach(el => el.remove());";

    private static final String METRICS_FAB_SVG =
            "this.innerHTML = '<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"26\" height=\"26\" " +
            "viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"#ffffff\" stroke-width=\"2.2\" " +
            "stroke-linecap=\"round\" stroke-linejoin=\"round\">" +
            "<line x1=\"4\" y1=\"7\" x2=\"20\" y2=\"7\"/>" +
            "<line x1=\"4\" y1=\"12\" x2=\"20\" y2=\"12\"/>" +
            "<line x1=\"4\" y1=\"17\" x2=\"20\" y2=\"17\"/>" +
            "</svg>';";

    public TripInfoView(final TripInfoService tripInfoService,
                        @Value("${app.crypto.secret}") String cryptoSecret,
                        @Value("${app.crypto.salt}") String cryptoSalt,
                        @Value("${app.admin.password}") String adminPassword,
                        @Value("${app.admin.username}") String adminUsername,
                        @Value("${app.version}") String appVersion,
                        @Value("${app.dev-mode:false}") boolean devMode,
                        RateLimiterService rateLimiterService,
                        TranslationService translationService,
                        ClaimsService claimsService,
                        TripInfoMetricService tripInfoMetricService) {

        addClassName("trip-info-view");
        setPadding(false);
        setSpacing(true);

        Button profileButton = buildProfileButton();
        HorizontalLayout headerRow = buildHeader(profileButton, appVersion);

        Button quickActionsFabButton = new Button();
        quickActionsFabButton.getElement().setAttribute("aria-label", "Open quick actions");
        quickActionsFabButton.addClassName("metrics-fab-btn");
        quickActionsFabButton.getElement().executeJs(METRICS_FAB_SVG);

        Button quickActionsMetricsButton = new Button("Metrics", new Icon(VaadinIcon.BAR_CHART));
        quickActionsMetricsButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        quickActionsMetricsButton.addClassName("metrics-fab-option");
        quickActionsMetricsButton.addClickListener(e -> UI.getCurrent().navigate("metrics"));

        Button quickActionsTbdButton = new Button("TBD", new Icon(VaadinIcon.TOOLS));
        quickActionsTbdButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        quickActionsTbdButton.addClassName("metrics-fab-option");
        quickActionsTbdButton.addClickListener(e -> UI.getCurrent().navigate("tbd"));

        Div quickActionsMenu = new Div(quickActionsMetricsButton, quickActionsTbdButton);
        quickActionsMenu.addClassName("metrics-fab-menu");

        Div quickActionsFab = new Div(quickActionsMenu, quickActionsFabButton);
        quickActionsFab.addClassName("metrics-fab");
        addAttachListener(e -> UI.getCurrent().getElement().appendChild(quickActionsFab.getElement()));
        addDetachListener(e -> quickActionsFab.getElement().removeFromParent());

        ProfileSetupBanner profileSetupBanner = new ProfileSetupBanner();
        Runnable profileHighlightCallback = buildProfileHighlightCallback(profileButton);
        AdminBanner adminBanner = new AdminBanner();

        TripInfoCard tripInfoCard = new TripInfoCard(cryptoSecret, cryptoSalt, claimsService,
                devMode, profileSetupBanner, profileHighlightCallback);

        ScheduledJobTimer scheduledJobTimer = new ScheduledJobTimer();

        AdminControls adminControls = new AdminControls(adminBanner, cryptoSecret, cryptoSalt,
                clearCacheRunnable(tripInfoService::clearCache,      "Trip info cache cleared"),
                clearCacheRunnable(tripInfoMetricService::clearCache, "Metrics cache cleared"),
                clearCacheRunnable(translationService::clearCache,    "Translation cache cleared"),
                translationService);

        InputLayout inputLayout = new InputLayout(tripInfoService, tripInfoCard, adminControls, rateLimiterService, scheduledJobTimer);
        inputLayout.setWidthFull();
        inputLayout.setMaxWidth("700px");
        inputLayout.getStyle().set("margin-left", "auto").set("margin-right", "auto");

        adminControls.getAdminCollectButton().addClickListener(e -> inputLayout.buildCollectRunnable().run());
        adminControls.getAdminClearDateButton().addClickListener(e -> inputLayout.buildClearDateRunnable().run());

        ProfileDrawer profileDrawer = new ProfileDrawer(cryptoSecret, cryptoSalt, adminControls, adminPassword, adminUsername);

        profileButton.addClickListener(e -> {
            profileButton.removeClassName("profile-btn--highlight");
            profileButton.getElement().executeJs(REMOVE_RIPPLE_JS);
            profileDrawer.open();
        });

        profileDrawer.setOnCloseCallback(() ->
                BrowserStorageUtils.encryptedLocalStorageLoad("userProfile", cryptoSecret, cryptoSalt, profileJson -> {
                    UserProfile profile = profileJson != null ? UserProfile.fromJson(profileJson) : null;
                    boolean complete = profile != null && profile.isComplete();
                    UI ui = UI.getCurrent();
                    if (ui != null) {
                        ui.access(() -> {
                            profileSetupBanner.setVisible(!complete);
                            if (!complete) profileHighlightCallback.run();
                        });
                    }
                }));

        profileDrawer.setOnSaveCallback(saved -> {
            boolean complete = saved != null && saved.isComplete();
            tripInfoCard.updateProfileState(complete);
            if (complete) {
                profileButton.removeClassName("profile-btn--highlight");
                profileButton.getElement().executeJs(REMOVE_RIPPLE_JS);
            } else {
                profileHighlightCallback.run();
            }
        });

        add(profileDrawer, headerRow, adminBanner, profileSetupBanner, inputLayout, tripInfoCard);
        setFlexGrow(1, tripInfoCard);
        setAlignItems(Alignment.CENTER);
        setAlignSelf(Alignment.STRETCH, headerRow, inputLayout, tripInfoCard);
    }

    private static HorizontalLayout buildHeader(Button profileButton, String appVersion) {
        Icon trainIcon = new Icon(VaadinIcon.TRAIN);
        trainIcon.setSize("1.9rem");
        trainIcon.getStyle().set("color", "#4caf7d");

        H1 heading = new H1("Movingo Tracker");
        heading.getStyle()
                .set("color", "#e2ede6")
                .set("font-size", "1.45rem")
                .set("font-weight", "600")
                .set("letter-spacing", "-0.01em")
                .set("margin", "0");

        HorizontalLayout titleGroup = new HorizontalLayout(trainIcon, heading);
        titleGroup.setAlignItems(Alignment.CENTER);
        titleGroup.setSpacing(true);

        Div titleWrapper = new Div(titleGroup);
        titleWrapper.addClassName("header-title-wrapper");

        GitHubLink githubLink = new GitHubLink("https://github.com/hasshe/railway-stats.git", appVersion);
        Div githubWrapper = new Div(githubLink);
        githubWrapper.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "flex-end");

        HorizontalLayout headerRow = new HorizontalLayout(profileButton, titleWrapper, githubWrapper);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.addClassName("header-row");
        return headerRow;
    }

    private static Button buildProfileButton() {
        Icon profileIcon = new Icon(VaadinIcon.MENU);
        profileIcon.setSize("2rem");
        profileIcon.getStyle().set("color", "#7abf9a");
        Button profileButton = new Button(profileIcon);
        profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE);
        profileButton.getElement().setAttribute("aria-label", "Profile");
        return profileButton;
    }

    private static Runnable buildProfileHighlightCallback(Button profileButton) {
        return () -> {
            profileButton.addClassName("profile-btn--highlight");
            profileButton.getElement().executeJs(
                    "if (!this.querySelector('.profile-btn-ripple-ring')) {" +
                    "  var r1 = document.createElement('div');" +
                    "  r1.className = 'profile-btn-ripple-ring';" +
                    "  var r2 = document.createElement('div');" +
                    "  r2.className = 'profile-btn-ripple-ring';" +
                    "  this.appendChild(r1);" +
                    "  this.appendChild(r2);" +
                    "}"
            );
        };
    }

    private static Runnable clearCacheRunnable(Runnable clearAction, String message) {
        return () -> {
            clearAction.run();
            Notification.show(message);
        };
    }
}
