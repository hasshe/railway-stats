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

    public ProfileDialog(String cryptoSecret, String cryptoSalt) {
        setHeaderTitle("Profile");
        setWidth("480px");

        TextField firstNameField = new TextField("First Name");
        firstNameField.setPlaceholder("John");
        firstNameField.setWidthFull();

        TextField lastNameField = new TextField("Last Name");
        lastNameField.setPlaceholder("Doe");
        lastNameField.setWidthFull();

        TextField phoneField = new TextField("Phone Number");
        phoneField.setPlaceholder("+46 70 000 00 00");
        phoneField.setWidthFull();

        TextField emailField = new TextField("Email Address");
        emailField.setPlaceholder("you@example.com");
        emailField.setWidthFull();

        TextField addressField = new TextField("Home Address");
        addressField.setPlaceholder("123 Main Street");
        addressField.setWidthFull();

        TextField cityField = new TextField("City");
        cityField.setPlaceholder("Stockholm");
        cityField.setWidthFull();

        TextField postalCodeField = new TextField("Postal Code");
        postalCodeField.setPlaceholder("111 22");
        postalCodeField.setWidthFull();

        FormLayout form = new FormLayout(firstNameField, lastNameField, phoneField, emailField, addressField, cityField, postalCodeField);
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
            } catch (Exception ignored) {
            }
        });

        Button saveButton = new Button("Save", clickEvent -> {
            String profile = String.format(
                    "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"address\":\"%s\",\"city\":\"%s\",\"postalCode\":\"%s\"}",
                    firstNameField.getValue(), lastNameField.getValue(),
                    phoneField.getValue(), emailField.getValue(),
                    addressField.getValue(), cityField.getValue(),
                    postalCodeField.getValue()
            );
            BrowserStorageUtils.encryptedLocalStorageSave(STORAGE_KEY, profile, cryptoSecret, cryptoSalt);
            Notification.show("Profile saved");
            close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", clickEvent -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        add(form);
        getFooter().add(cancelButton, saveButton);
    }
}
