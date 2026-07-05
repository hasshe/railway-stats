package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class MetricCardBuilder {

    public static Div createMetricCard(String label, String value, String subtitle, VaadinIcon icon, String... classNames) {
        Div card = new Div();
        for (String className : classNames) {
            card.addClassName(className);
        }
        card.setWidthFull();

        Icon iconElement = new Icon(icon);
        iconElement.addClassName("metric-icon");

        VerticalLayout contentLayout = createVerticalLayout(label, value, subtitle);

        HorizontalLayout cardContent = new HorizontalLayout(iconElement, contentLayout);
        cardContent.addClassName("metric-card-content");
        cardContent.setWidthFull();
        cardContent.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContent.setSpacing(true);
        cardContent.setPadding(false);

        card.add(cardContent);
        return card;
    }

    private static VerticalLayout createVerticalLayout(String label, String value, String subtitle) {
        Span labelSpan = new Span(label);
        labelSpan.addClassName("metric-label");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("metric-value");

        Span subtitleSpan = new Span(subtitle);
        subtitleSpan.addClassName("metric-subtitle");

        VerticalLayout contentLayout = new VerticalLayout();
        contentLayout.addClassName("metric-content");
        contentLayout.setPadding(false);
        contentLayout.setSpacing(false);
        contentLayout.add(labelSpan, valueSpan, subtitleSpan);
        return contentLayout;
    }
}
