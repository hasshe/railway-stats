package com.hs.railway_stats.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.Year;
import java.util.List;
import java.util.stream.IntStream;

@Route("tbd")
@PageTitle("Under construction – Movingo Tracker")
@CssImport("./themes/railway-stats/styles.css")
public class TbdView extends VerticalLayout {

    public TbdView() {
        addClassName("metrics-view");
        setPadding(false);
        setSpacing(true);
        setAlignItems(Alignment.CENTER);

        Button backButton = getBackButton();
        backButton.addClickListener(e -> UI.getCurrent().navigate(""));

        H1 heading = new H1("TBD");
        heading.getStyle()
                .set("color", "#e2ede6")
                .set("font-size", "1.45rem")
                .set("font-weight", "600")
                .set("letter-spacing", "-0.01em")
                .set("margin", "0");

        Icon titleIcon = new Icon(VaadinIcon.TOOLS);
        titleIcon.setSize("1.6rem");
        titleIcon.getStyle().set("color", "#4caf7d");

        HorizontalLayout titleGroup = new HorizontalLayout(titleIcon, heading);
        titleGroup.setAlignItems(Alignment.CENTER);
        titleGroup.setSpacing(true);
        titleGroup.setWidthFull();
        titleGroup.setJustifyContentMode(JustifyContentMode.CENTER);

        Div rightSpacer = new Div();
        rightSpacer.getStyle()
                .set("width", "2.4rem")
                .set("height", "2.4rem")
                .set("flex-shrink", "0");

        HorizontalLayout headerRow = new HorizontalLayout(backButton, titleGroup, rightSpacer);
        headerRow.setWidthFull();
        headerRow.setAlignItems(Alignment.CENTER);
        headerRow.setJustifyContentMode(JustifyContentMode.BETWEEN);
        headerRow.setSpacing(true);
        headerRow.setFlexGrow(1, titleGroup);
        headerRow.getStyle().set("flex-shrink", "0");

        Div calculatorCard = new Div();
        calculatorCard.addClassNames("metric-card", "metric-card--orange");
        calculatorCard.setWidthFull();
        calculatorCard.setMaxWidth("700px");

        Icon cardIcon = new Icon(VaadinIcon.CALC_BOOK);
        cardIcon.addClassName("metric-icon");

        Span cardTitle = new Span("Expired tickets total");
        cardTitle.addClassName("metric-label");

        Span cardValue = new Span("–");
        cardValue.addClassName("metric-value");

        Span cardSubtitle = new Span("Select a year and click calculate");
        cardSubtitle.addClassName("metric-subtitle");

        ComboBox<Integer> yearSelector = new ComboBox<>("Year");
        int currentYear = Year.now().getValue();
        List<Integer> years = IntStream.rangeClosed(currentYear - 10, currentYear)
                .boxed()
                .sorted((a, b) -> Integer.compare(b, a))
                .toList();
        yearSelector.setItems(years);
        yearSelector.setValue(currentYear);
        yearSelector.setWidth("180px");

        Button calculateButton = new Button("Calculate", new Icon(VaadinIcon.CALC));
        calculateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        calculateButton.addClickListener(e -> Notification.show("Under Construction..."));

        HorizontalLayout controlsRow = new HorizontalLayout(yearSelector, calculateButton);
        controlsRow.setAlignItems(Alignment.END);
        controlsRow.setSpacing(true);

        Div cardContent = new Div(cardTitle, cardValue, cardSubtitle, controlsRow);
        cardContent.addClassName("metric-card-content");
        calculatorCard.add(cardIcon, cardContent);

        Div banner = new Div();
        banner.addClassName("profile-setup-banner");
        banner.setText("Under construction");
        banner.getStyle()
                .set("max-width", "700px")
                .set("margin-top", "2rem")
                .set("text-align", "center");

        add(headerRow, banner, calculatorCard);
        setAlignSelf(Alignment.STRETCH, headerRow);
    }

    private static Button getBackButton() {
        Icon backIcon = new Icon(VaadinIcon.ARROW_LEFT);
        backIcon.getStyle().set("color", "#4caf7d");
        Button backButton = new Button(backIcon);
        backButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        backButton.addClassName("metrics-back-btn");
        backButton.getElement().setAttribute("aria-label", "Back");
        return backButton;
    }
}
