package com.hs.railway_stats;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;
import org.springframework.stereotype.Component;

/**
 * App Shell Configuration for Vaadin PWA
 * 
 * This class configures the application shell, including PWA settings,
 * meta tags, and viewport configuration.
 */
@Component
@PWA(
    name = "Movingo Tracker",
    shortName = "Movingo Tracker",
    description = "Railway statistics and trip tracking application",
    startPath = "/",
    backgroundColor = "#ffffff",
    themeColor = "#1677be",
    iconPath = "icons/icon-192x192.png",
    manifestPath = "manifest.webmanifest"
)
@Viewport("width=device-width, initial-scale=1, viewport-fit=contain")
@Meta(name = "apple-mobile-web-app-capable", content = "yes")
@Meta(name = "apple-mobile-web-app-status-bar-style", content = "black-translucent")
@Meta(name = "apple-mobile-web-app-title", content = "Movingo Tracker")
public class AppShell implements AppShellConfigurator {
    // Configuration is done via annotations
}
