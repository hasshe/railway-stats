package com.hs.railway_stats.view.component;

import com.hs.railway_stats.view.util.AdminSessionUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;

public class AdminControls extends HorizontalLayout {

    @Getter
    private final Button adminCollectButton;
    @Getter
    private final AdminBanner adminBanner;

    public AdminControls(AdminBanner adminBanner,
                         String cryptoSecret,
                         String cryptoSalt,
                         Runnable onCollect) {

        this.adminBanner = adminBanner;

        adminCollectButton = new Button("🔄 Collect (Admin)");
        adminCollectButton.setVisible(false);
        adminCollectButton.addClickListener(clickEvent -> {
            try {
                onCollect.run();
            } catch (Exception e) {
                Notification.show("Error collecting trip information: " + e.getMessage());
            }
        });

        AdminSessionUtils.restoreAdminSession(adminCollectButton, adminBanner, cryptoSecret, cryptoSalt);

        add(adminCollectButton);
    }
}
