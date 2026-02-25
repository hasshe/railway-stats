package com.hs.railway_stats.view.component;

import com.hs.railway_stats.view.util.BrowserStorageUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

public class TicketLayout extends HorizontalLayout {

    private static final String COOKIE_KEY = "ticketNumber";

    public TicketLayout() {
        TextField ticketField = new TextField("Ticket Number");
        ticketField.setPlaceholder("e.g. B123ABCG6");
        ticketField.setReadOnly(true);

        Button editButton = new Button(new Icon(VaadinIcon.PENCIL));
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        editButton.getElement().setAttribute("aria-label", "Edit ticket number");

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setVisible(false);

        editButton.addClickListener(clickEvent -> {
            ticketField.setReadOnly(false);
            ticketField.focus();
            editButton.setVisible(false);
            saveButton.setVisible(true);
        });

        saveButton.addClickListener(clickEvent -> {
            BrowserStorageUtils.cookieSave(COOKIE_KEY, ticketField.getValue());
            ticketField.setReadOnly(true);
            saveButton.setVisible(false);
            editButton.setVisible(true);
            Notification.show("Ticket number saved");
        });

        BrowserStorageUtils.cookieLoadIntoField(COOKIE_KEY, ticketField);

        setAlignItems(Alignment.BASELINE);
        add(ticketField, editButton, saveButton);
    }
}
