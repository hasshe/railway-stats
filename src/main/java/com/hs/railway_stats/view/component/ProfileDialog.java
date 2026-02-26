package com.hs.railway_stats.view.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.railway_stats.view.util.BrowserStorageUtils;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;

public class ProfileDialog extends Dialog {

    private static final String STORAGE_KEY = "userProfile";

    private static TextField styledField(String label, String placeholder) {
        TextField field = new TextField(label);
        field.setPlaceholder(placeholder);
        field.setWidthFull();
        field.getStyle().set("--vaadin-input-field-label-color", "#ffffff");
        return field;
    }

    public ProfileDialog(String cryptoSecret, String cryptoSalt) {
        setHeaderTitle("Profile");
        getElement().getThemeList().add("profile-dialog");
        setWidth("480px");
        setCloseOnEsc(true);

        TextField firstNameField = styledField("First Name", "John");
        TextField lastNameField = styledField("Last Name", "Doe");
        TextField phoneField = styledField("Phone Number", "+46 70 000 00 00");
        TextField emailField = styledField("Email Address", "you@example.com");
        TextField addressField = styledField("Home Address", "123 Main Street");
        TextField cityField = styledField("City", "Stockholm");
        TextField postalCodeField = styledField("Postal Code", "111 22");
        TextField ticketNumberField = styledField("Ticket Number", "e.g. B123ABCG6");

        firstNameField.getStyle().set("margin-top", "20px");
        ticketNumberField.getStyle().set("margin-bottom", "20px");

        FormLayout form = new FormLayout(firstNameField, lastNameField, phoneField, emailField, addressField, cityField, postalCodeField, ticketNumberField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        BrowserStorageUtils.encryptedLocalStorageLoad(STORAGE_KEY, cryptoSecret, cryptoSalt, json -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);
                firstNameField.setValue(node.path("firstName").asText(""));
                lastNameField.setValue(node.path("lastName").asText(""));
                phoneField.setValue(node.path("phone").asText(""));
                emailField.setValue(node.path("email").asText(""));
                addressField.setValue(node.path("address").asText(""));
                cityField.setValue(node.path("city").asText(""));
                postalCodeField.setValue(node.path("postalCode").asText(""));
                ticketNumberField.setValue(node.path("ticketNumber").asText(""));
            } catch (Exception ignored) {
            }
        });

        Button saveButton = new Button("Save", clickEvent -> {
            String profile = String.format(
                    "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"address\":\"%s\",\"city\":\"%s\",\"postalCode\":\"%s\",\"ticketNumber\":\"%s\"}",
                    firstNameField.getValue(), lastNameField.getValue(),
                    phoneField.getValue(), emailField.getValue(),
                    addressField.getValue(), cityField.getValue(),
                    postalCodeField.getValue(), ticketNumberField.getValue()
            );
            BrowserStorageUtils.encryptedLocalStorageSave(STORAGE_KEY, profile, cryptoSecret, cryptoSalt);
            Notification.show("Profile saved");
            close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setWidthFull();
        saveButton.getStyle().set("margin-top", "16px").set("margin-bottom", "8px");

        add(form);
        getFooter().add(saveButton);
    }
}
