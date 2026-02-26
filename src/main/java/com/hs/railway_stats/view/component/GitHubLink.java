package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.SvgIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class GitHubLink extends Anchor {

    public GitHubLink(String url, String version) {
        super(url);

        SvgIcon githubIcon = new SvgIcon("icons/github.svg");
        githubIcon.setSize("1.8rem");
        githubIcon.getStyle().set("color", "#b0bdd0");

        Button githubButton = new Button(githubIcon);
        githubButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_LARGE);
        githubButton.getElement().setAttribute("aria-label", "GitHub repository");

        Span versionLabel = new Span("v" + version);
        versionLabel.getStyle()
                .set("font-size", "0.65rem")
                .set("color", "#7a8a9a")
                .set("letter-spacing", "0.04em");

        VerticalLayout wrapper = new VerticalLayout(githubButton, versionLabel);
        wrapper.setAlignItems(FlexComponent.Alignment.CENTER);
        wrapper.setSpacing(false);
        wrapper.setPadding(false);
        wrapper.getStyle().set("gap", "0");

        add(wrapper);
        setTarget("_blank");
        getElement().setAttribute("rel", "noopener noreferrer");
    }
}
