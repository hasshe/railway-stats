package com.hs.railway_stats.view.util;

import com.hs.railway_stats.view.component.AdminBanner;
import com.vaadin.flow.component.button.Button;

public final class AdminSessionUtils {

    private static final String STORAGE_KEY = "adminSession";
    private static final String ADMIN_TOKEN = "admin-authenticated";

    private AdminSessionUtils() {
    }

    public static void saveAdminSession(String secret, String salt) {
        BrowserStorageUtils.encryptedLocalStorageSave(STORAGE_KEY, ADMIN_TOKEN, secret, salt);
    }

    public static void clearAdminSession() {
        BrowserStorageUtils.localStorageRemove(STORAGE_KEY);
    }

    public static void restoreAdminSession(Button adminCollectButton, AdminBanner adminBanner,
                                           String secret, String salt) {
        BrowserStorageUtils.encryptedLocalStorageLoad(STORAGE_KEY, secret, salt, result -> {
            if (ADMIN_TOKEN.equals(result)) {
                adminCollectButton.setVisible(true);
                adminBanner.setVisible(true);
            }
        });
    }
}

