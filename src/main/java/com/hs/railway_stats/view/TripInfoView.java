package com.hs.railway_stats.view;

import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TripInfoService;
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
                        @Value("${app.version}") String appVersion,
                        RateLimiterService rateLimiterService) {

        addClassName("trip-info-view");
        setPadding(true);
        setSpacing(true);

        ProfileDrawer profileDrawer = new ProfileDrawer(cryptoSecret, cryptoSalt);

        Icon profileIcon = new Icon(VaadinIcon.MENU);
        profileIcon.setSize("2rem");
        Button profileButton = new Button(profileIcon);
        profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE);
        profileButton.getElement().setAttribute("aria-label", "Profile");
        profileButton.addClickListener(clickEvent -> profileDrawer.open());

        Icon trainIcon = new Icon(VaadinIcon.TRAIN);
        trainIcon.setSize("2rem");
        trainIcon.getStyle().set("color", "#e8edf5");

        H1 heading = new H1("Movingo Tracker");
        heading.getStyle()
                .set("color", "#e8edf5")
                .set("font-weight", "700")
                .set("letter-spacing", "0.02em")
                .set("margin", "0")
                .set("text-shadow", "0 2px 12px rgba(106, 163, 255, 0.20)");

        HorizontalLayout titleGroup = new HorizontalLayout(trainIcon, heading);
        titleGroup.setAlignItems(Alignment.CENTER);
        titleGroup.setSpacing(true);

        GitHubLink githubLink = new GitHubLink("https://github.com/hasshe/railway-stats.git", appVersion);

        HorizontalLayout headerRow = new HorizontalLayout(profileButton, titleGroup, githubLink);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        TripInfoGrid tripInfoGrid = new TripInfoGrid();
        InputLayout inputLayout = new InputLayout(tripInfoService, tripInfoGrid, null, adminPassword, cryptoSecret, cryptoSalt, rateLimiterService);
        inputLayout.setMaxWidth("700px");
        inputLayout.getStyle().set("margin-left", "auto").set("margin-right", "auto");

        add(profileDrawer, headerRow, inputLayout, tripInfoGrid);
        setFlexGrow(1, tripInfoGrid);
        setAlignItems(Alignment.CENTER);
        setAlignSelf(Alignment.STRETCH, headerRow, tripInfoGrid);
    }
}
