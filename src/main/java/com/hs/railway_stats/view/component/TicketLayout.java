/*
package com.hs.railway_stats.view.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.railway_stats.view.util.BrowserStorageUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;

public class TicketLayout extends FormLayout {

    private static final String STORAGE_KEY = "userProfile";

    public TicketLayout(String cryptoSecret, String cryptoSalt) {
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
            // Load the existing profile, update ticketNumber, and re-save encrypted
            BrowserStorageUtils.encryptedLocalStorageLoad(STORAGE_KEY, cryptoSecret, cryptoSalt, json -> {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode node = mapper.readTree(json);
                    String updated = String.format(
                            "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"address\":\"%s\",\"city\":\"%s\",\"postalCode\":\"%s\",\"ticketNumber\":\"%s\"}",
                            node.path("firstName").asText(""),
                            node.path("lastName").asText(""),
                            node.path("phone").asText(""),
                            node.path("email").asText(""),
                            node.path("address").asText(""),
                            node.path("city").asText(""),
                            node.path("postalCode").asText(""),
                            ticketField.getValue()
                    );
                    BrowserStorageUtils.encryptedLocalStorageSave(STORAGE_KEY, updated, cryptoSecret, cryptoSalt);
                } catch (Exception ignored) {
                    // If no profile exists yet, save just the ticket number
                    String minimal = String.format(
                            "{\"firstName\":\"\",\"lastName\":\"\",\"phone\":\"\",\"email\":\"\",\"address\":\"\",\"city\":\"\",\"postalCode\":\"\",\"ticketNumber\":\"%s\"}",
                            ticketField.getValue()
                    );
                    BrowserStorageUtils.encryptedLocalStorageSave(STORAGE_KEY, minimal, cryptoSecret, cryptoSalt);
                }
            });
            ticketField.setReadOnly(true);
            saveButton.setVisible(false);
            editButton.setVisible(true);
            ticketField.setSuffixComponent(editButton);
            Notification.show("Ticket number saved");
        });

        // Load ticket number from encrypted profile storage
        BrowserStorageUtils.encryptedLocalStorageLoad(STORAGE_KEY, cryptoSecret, cryptoSalt, json -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);
                String ticket = node.path("ticketNumber").asText("");
                if (!ticket.isBlank()) {
                    ticketField.setValue(ticket);
                }
            } catch (Exception ignored) {
            }
        });

        setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 3)
        );
        add(ticketField);
    }
}

*/
