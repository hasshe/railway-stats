package com.hs.railway_stats.view.component;

import com.hs.railway_stats.service.TranslationService;
import com.hs.railway_stats.view.util.AdminSessionUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

public class AdminControls extends HorizontalLayout {

    @Getter
    private final Button adminCollectButton;
    @Getter
    private final Button adminAddStationButton;
    @Getter
    private final AdminBanner adminBanner;

    public AdminControls(AdminBanner adminBanner,
                         String cryptoSecret,
                         String cryptoSalt,
                         Runnable onCollect,
                         TranslationService translationService) {

        this.adminBanner = adminBanner;

        adminCollectButton = new Button("Collect (Admin)", new Icon(VaadinIcon.CLOUD_UPLOAD));
        adminCollectButton.setVisible(false);
        adminCollectButton.addClickListener(clickEvent -> {
            try {
                onCollect.run();
            } catch (Exception e) {
                Notification.show("Error collecting trip information: " + e.getMessage());
            }
        });

        adminAddStationButton = new Button("Add Station (Admin)", new Icon(VaadinIcon.PLUS));
        adminAddStationButton.setVisible(false);
        adminAddStationButton.addClickListener(clickEvent -> {
            AddStationDialog dialog = new AddStationDialog(translationService);
            dialog.open();
        });

        AdminSessionUtils.restoreAdminSession(adminCollectButton, adminBanner, cryptoSecret, cryptoSalt,
                () -> adminAddStationButton.setVisible(true));

        add(adminCollectButton, adminAddStationButton);
    }

    public void setAdminVisible(boolean visible) {
        adminCollectButton.setVisible(visible);
        adminAddStationButton.setVisible(visible);
        adminBanner.setVisible(visible);
    }
}
