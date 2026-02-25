package com.hs.railway_stats.view;

import com.hs.railway_stats.service.RateLimiterService;
import com.hs.railway_stats.service.TripInfoService;
import com.hs.railway_stats.view.component.AdminBanner;
import com.hs.railway_stats.view.component.InputLayout;
import com.hs.railway_stats.view.component.ProfileDialog;
import com.hs.railway_stats.view.component.TicketLayout;
import com.hs.railway_stats.view.component.TripInfoGrid;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Value;

@Route("")
public class TripInfoView extends VerticalLayout {

    public TripInfoView(final TripInfoService tripInfoService,
                        @Value("${app.crypto.secret}") String cryptoSecret,
                        @Value("${app.crypto.salt}") String cryptoSalt,
                        @Value("${app.admin.password}") String adminPassword,
                        RateLimiterService rateLimiterService) {

        setPadding(true);
        setSpacing(true);

        AdminBanner adminBanner = new AdminBanner();

        Icon profileIcon = new Icon(VaadinIcon.USER);
        profileIcon.setSize("2rem");
        Button profileButton = new Button(profileIcon);
        profileButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE);
        profileButton.getElement().setAttribute("aria-label", "Profile");
        profileButton.addClickListener(clickEvent -> new ProfileDialog(cryptoSecret, cryptoSalt).open());

        HorizontalLayout headerRow = new HorizontalLayout(new H1("Trip Information"), profileButton);
        headerRow.setWidthFull();
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setAlignItems(Alignment.CENTER);

        TripInfoGrid tripInfoGrid = new TripInfoGrid();
        InputLayout inputLayout = new InputLayout(tripInfoService, tripInfoGrid, adminBanner, adminPassword, cryptoSecret, cryptoSalt, rateLimiterService);
        TicketLayout ticketLayout = new TicketLayout();

        add(headerRow, adminBanner, inputLayout, ticketLayout, tripInfoGrid);
        setFlexGrow(1, tripInfoGrid);
    }
}
