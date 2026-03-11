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
    private final Button adminClearDateButton;
    @Getter
    private final Button adminClearTripInfoCacheButton;
    @Getter
    private final Button adminClearMetricsCacheButton;
    @Getter
    private final Button adminClearTranslationCacheButton;
    @Getter
    private final AdminBanner adminBanner;

    public AdminControls(AdminBanner adminBanner,
                         String cryptoSecret,
                         String cryptoSalt,
                         Runnable onCollect,
                         Runnable onClearDate,
                         Runnable onClearTripInfoCache,
                         Runnable onClearMetricsCache,
                         Runnable onClearTranslationCache,
                         TranslationService translationService) {

        this.adminBanner = adminBanner;

        adminCollectButton = new Button("Collect (Admin)", new Icon(VaadinIcon.CLOUD_UPLOAD));
        adminCollectButton.setVisible(false);
        adminCollectButton.addClassName("admin-controls-btn");
        adminCollectButton.addClickListener(clickEvent -> {
            try {
                onCollect.run();
            } catch (Exception e) {
                Notification.show("Error collecting trip information: " + e.getMessage());
            }
        });

        adminAddStationButton = new Button("Add Station (Admin)", new Icon(VaadinIcon.PLUS));
        adminAddStationButton.setVisible(false);
        adminAddStationButton.addClassName("admin-controls-btn");
        adminAddStationButton.addClickListener(clickEvent -> {
            AddStationDialog dialog = new AddStationDialog(translationService);
            dialog.open();
        });

        adminClearDateButton = new Button("Clear Date (Admin)", new Icon(VaadinIcon.TRASH));
        adminClearDateButton.setVisible(false);
        adminClearDateButton.addClassName("admin-controls-btn");
        adminClearDateButton.addClickListener(clickEvent -> {
            try {
                onClearDate.run();
            } catch (Exception e) {
                Notification.show("Error clearing trips: " + e.getMessage());
            }
        });

        adminClearTripInfoCacheButton = new Button("Clear Trip Info Cache", new Icon(VaadinIcon.REFRESH));
        adminClearTripInfoCacheButton.setVisible(false);
        adminClearTripInfoCacheButton.addClassName("admin-controls-btn");
        adminClearTripInfoCacheButton.addClickListener(clickEvent -> {
            try {
                onClearTripInfoCache.run();
            } catch (Exception e) {
                Notification.show("Error clearing trip info cache: " + e.getMessage());
            }
        });

        adminClearMetricsCacheButton = new Button("Clear Metrics Cache", new Icon(VaadinIcon.REFRESH));
        adminClearMetricsCacheButton.setVisible(false);
        adminClearMetricsCacheButton.addClassName("admin-controls-btn");
        adminClearMetricsCacheButton.addClickListener(clickEvent -> {
            try {
                onClearMetricsCache.run();
            } catch (Exception e) {
                Notification.show("Error clearing metrics cache: " + e.getMessage());
            }
        });

        adminClearTranslationCacheButton = new Button("Clear Translation Cache", new Icon(VaadinIcon.REFRESH));
        adminClearTranslationCacheButton.setVisible(false);
        adminClearTranslationCacheButton.addClassName("admin-controls-btn");
        adminClearTranslationCacheButton.addClickListener(clickEvent -> {
            try {
                onClearTranslationCache.run();
            } catch (Exception e) {
                Notification.show("Error clearing translation cache: " + e.getMessage());
            }
        });

        AdminSessionUtils.restoreAdminSession(adminCollectButton, adminBanner, cryptoSecret, cryptoSalt,
                () -> {
                    adminAddStationButton.setVisible(true);
                    adminClearDateButton.setVisible(true);
                    adminClearTripInfoCacheButton.setVisible(true);
                    adminClearMetricsCacheButton.setVisible(true);
                    adminClearTranslationCacheButton.setVisible(true);
                });

        setWidthFull();
        getStyle().set("flex-wrap", "wrap").set("row-gap", "0.5rem");

        add(adminCollectButton, adminAddStationButton, adminClearDateButton, adminClearTripInfoCacheButton, adminClearMetricsCacheButton, adminClearTranslationCacheButton);
    }

    public void setAdminVisible(boolean visible) {
        adminCollectButton.setVisible(visible);
        adminAddStationButton.setVisible(visible);
        adminClearDateButton.setVisible(visible);
        adminClearTripInfoCacheButton.setVisible(visible);
        adminClearMetricsCacheButton.setVisible(visible);
        adminClearTranslationCacheButton.setVisible(visible);
        adminBanner.setVisible(visible);
    }
}
