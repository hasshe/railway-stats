package com.hs.railway_stats.view.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.railway_stats.view.util.BrowserStorageUtils;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;

@Getter
public class ProfileDrawer extends Div {

    private static final String STORAGE_KEY = "userProfile";
    private boolean open = false;

    private static TextField styledField(String label, String placeholder) {
        TextField field = new TextField(label);
        field.setPlaceholder(placeholder);
        field.setWidthFull();
        field.getStyle().set("--vaadin-input-field-label-color", "#ffffff");
        return field;
    }

    public ProfileDrawer(String cryptoSecret, String cryptoSalt) {
        addClassName("profile-drawer");
        getStyle()
                .set("--lumo-primary-color", "#6aa3ff")
                .set("--lumo-primary-text-color", "#6aa3ff")
                .set("--lumo-body-text-color", "#e8edf5")
                .set("--lumo-contrast-60pct", "rgba(232, 237, 245, 0.85)")
                .set("--lumo-contrast-70pct", "rgba(232, 237, 245, 0.90)")
                .set("--lumo-contrast-90pct", "#e8edf5");

        Div backdrop = new Div();
        backdrop.addClassName("profile-drawer-backdrop");
        backdrop.addClickListener(e -> close());

        Div panel = new Div();
        panel.addClassName("profile-drawer-panel");

        H2 title = new H2("Profile");
        title.getStyle()
                .set("color", "#e8edf5")
                .set("font-size", "1.2rem")
                .set("font-weight", "700")
                .set("letter-spacing", "0.03em")
                .set("margin", "0");

        Button closeBtn = new Button(new Icon(VaadinIcon.CLOSE));
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeBtn.getStyle().set("color", "#b0bdd0");
        closeBtn.addClickListener(e -> close());

        Div header = new Div(title, closeBtn);
        header.addClassName("profile-drawer-header");

        TextField firstNameField = styledField("First Name", "John");
        TextField lastNameField = styledField("Last Name", "Doe");
        TextField phoneField = styledField("Phone Number", "+46 70 000 00 00");
        TextField emailField = styledField("Email Address", "you@example.com");
        TextField addressField = styledField("Home Address", "123 Main Street");
        TextField cityField = styledField("City", "Stockholm");
        TextField postalCodeField = styledField("Postal Code", "111 22");
        TextField ticketNumberField = styledField("Ticket Number", "e.g. B123ABCG6");

        firstNameField.getStyle().set("margin-top", "8px");

        FormLayout form = new FormLayout(firstNameField, lastNameField, phoneField, emailField,
                addressField, cityField, postalCodeField, ticketNumberField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.addClassName("profile-drawer-form");
        form.getStyle().set("--lumo-body-text-color", "#e8edf5");

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
        saveButton.getStyle().set("margin", "0");

        Div content = new Div(form);
        content.addClassName("profile-drawer-content");

        Div footer = new Div(saveButton);
        footer.addClassName("profile-drawer-footer");

        panel.add(header, content, footer);
        add(backdrop, panel);
    }

    public void open() {
        open = true;
        removeClassName("profile-drawer--closed");
        addClassName("profile-drawer--open");
        UI.getCurrent().getPage().executeJs("document.body.style.overflow='hidden';");
    }

    public void close() {
        open = false;
        removeClassName("profile-drawer--open");
        addClassName("profile-drawer--closed");
        UI.getCurrent().getPage().executeJs("document.body.style.overflow='';");
    }

}

