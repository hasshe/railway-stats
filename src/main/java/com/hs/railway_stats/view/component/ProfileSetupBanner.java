package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

public class ProfileSetupBanner extends Span {

    public ProfileSetupBanner() {
        Icon icon = new Icon(VaadinIcon.USER);
        icon.getStyle()
                .set("margin-right", "0.5em")
                .set("flex-shrink", "0")
                .set("vertical-align", "middle");
        add(icon, new Text("Set up your profile to enable trip claims."));
        addClassName("profile-setup-banner");
        getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("width", "100%")
                .set("box-sizing", "border-box");
        setVisible(false);
    }
}

