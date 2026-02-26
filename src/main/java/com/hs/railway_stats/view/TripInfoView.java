package com.hs.railway_stats.view;

import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TranslationService;
import com.hs.railway_stats.service.TripInfoService;
import com.hs.railway_stats.view.component.AdminBanner;
import com.hs.railway_stats.view.component.AdminControls;
import com.hs.railway_stats.view.component.GitHubLink;
import com.hs.railway_stats.view.component.InputLayout;
import com.hs.railway_stats.view.component.ProfileDrawer;
import com.hs.railway_stats.view.component.TripInfoGrid;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

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
                        TranslationService translationService) {

        addClassName("trip-info-view");
        setPadding(false);   // handled in CSS per breakpoint
        setSpacing(true);

        Icon profileIcon = new Icon(VaadinIcon.MENU);
        profileIcon.setSize("2rem");
        Button profileButton = new Button(profileIcon);
        profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE);
        profileButton.getElement().setAttribute("aria-label", "Profile");

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

        GitHubLink githubLink = new GitHubLink("https://github.com/hasshe/railway-stats.git", appVersion);

        HorizontalLayout headerRow = new HorizontalLayout(profileButton, titleGroup, githubLink);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerRow.getStyle()
                .set("max-width", "100%")
                .set("overflow", "hidden")
                .set("flex-shrink", "0");

        TripInfoGrid tripInfoGrid = new TripInfoGrid();
        AdminBanner adminBanner = new AdminBanner();

        Runnable[] collectHolder = {() -> {}};
        AdminControls adminControls = new AdminControls(adminBanner, cryptoSecret, cryptoSalt,
                () -> collectHolder[0].run(), translationService);

        ProfileDrawer profileDrawer = new ProfileDrawer(cryptoSecret, cryptoSalt, adminControls, adminPassword, adminUsername);
        profileButton.addClickListener(clickEvent -> profileDrawer.open());

        InputLayout inputLayout = new InputLayout(tripInfoService, tripInfoGrid, adminControls, rateLimiterService);
        collectHolder[0] = inputLayout.buildCollectRunnable(tripInfoService, tripInfoGrid, rateLimiterService);

        inputLayout.setWidthFull();
        inputLayout.setMaxWidth("700px");
        inputLayout.getStyle().set("margin-left", "auto").set("margin-right", "auto");

        add(profileDrawer, headerRow, adminBanner, inputLayout, tripInfoGrid);
        setFlexGrow(1, tripInfoGrid);
        setAlignItems(Alignment.CENTER);
        setAlignSelf(Alignment.STRETCH, headerRow, tripInfoGrid);
    }
}
