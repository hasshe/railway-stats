package com.hs.railway_stats.view.component;

import com.hs.railway_stats.view.util.BrowserStorageUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;

public class TicketLayout extends FormLayout {

    private static final String COOKIE_KEY = "ticketNumber";

    public TicketLayout() {
        TextField ticketField = new TextField("Ticket Number");
        ticketField.setPlaceholder("e.g. B123ABCG6");
        ticketField.setReadOnly(true);

        Button editButton = new Button("Edit");
        editButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editButton.addClassName("edit-ticket-btn");

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setVisible(false);

        ticketField.setSuffixComponent(editButton);

        editButton.addClickListener(clickEvent -> {
            ticketField.setReadOnly(false);
            ticketField.focus();
            editButton.setVisible(false);
            saveButton.setVisible(true);
            ticketField.setSuffixComponent(saveButton);
        });

        saveButton.addClickListener(clickEvent -> {
            BrowserStorageUtils.cookieSave(COOKIE_KEY, ticketField.getValue());
            ticketField.setReadOnly(true);
            saveButton.setVisible(false);
            editButton.setVisible(true);
            ticketField.setSuffixComponent(editButton);
            Notification.show("Ticket number saved");
        });

        BrowserStorageUtils.cookieLoadIntoField(COOKIE_KEY, ticketField);

        setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 3)
        );
        add(ticketField);
    }
}


