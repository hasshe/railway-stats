package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class AdminBanner extends Span {

    public AdminBanner() {
        Icon lockIcon = new Icon(VaadinIcon.LOCK);
        lockIcon.getStyle().set("margin-right", "0.4em");
        add(lockIcon, new Text("Admin Mode Active"));
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

