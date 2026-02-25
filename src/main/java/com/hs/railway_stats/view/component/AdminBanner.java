package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.html.Span;

public class AdminBanner extends Span {

    public AdminBanner() {
        setText("🔐 Admin Mode Active");
        getStyle()
                .set("background-color", "var(--lumo-error-color)")
                .set("color", "var(--lumo-error-contrast-color)")
                .set("padding", "0.4em 1em")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("font-weight", "bold")
                .set("font-size", "var(--lumo-font-size-s)");
        setVisible(false);
    }
}

