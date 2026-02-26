package com.hs.railway_stats.view.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hs.railway_stats.view.util.AdminSessionUtils;
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
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import lombok.Getter;

@Getter
public class ProfileDrawer extends Div {

    private static final String STORAGE_KEY = "userProfile";
    private boolean open = false;

    public ProfileDrawer(String cryptoSecret, String cryptoSalt,
                         AdminControls adminControls, String adminPassword,
                         String adminUsername) {
        addClassName("profile-drawer");
        applyDrawerStyles();

        Div backdrop = buildBackdrop();
        Div panel = new Div();
        panel.addClassName("profile-drawer-panel");

        ProfileFields fields = new ProfileFields();

        applyValidations(fields);

        Button adminToggleButton = buildAdminToggleButton(adminPassword, adminControls, cryptoSecret, cryptoSalt);
        wireAdminToggleVisibility(adminToggleButton, fields.firstName, adminUsername);

        loadFromStorage(cryptoSecret, cryptoSalt, fields, () -> {
            boolean matches = fields.firstName.getValue().trim().equalsIgnoreCase(adminUsername);
            adminToggleButton.setVisible(matches);
        });

        Div header = buildHeader();
        FormLayout form = buildForm(fields);
        Button saveButton = buildSaveButton(cryptoSecret, cryptoSalt, fields);
        Div footer = buildFooter(saveButton, adminToggleButton);
        Div content = new Div(form);
        content.addClassName("profile-drawer-content");

        panel.add(header, content, footer);
        add(backdrop, panel);
    }

    private static class ProfileFields {
        final TextField firstName = styledField("First Name", "John");
        final TextField lastName = styledField("Last Name", "Doe");
        final TextField phone = styledField("Phone Number", "+46 70 000 00 00");
        final TextField email = styledField("Email Address", "you@example.com");
        final TextField address = styledField("Home Address", "123 Main Street");
        final TextField city = styledField("City", "Stockholm");
        final TextField postalCode = styledField("Postal Code", "111 22");
        final TextField ticketNumber = styledField("Ticket Number", "e.g. B123ABCG6");
    }

    private Div buildBackdrop() {
        Div backdrop = new Div();
        backdrop.addClassName("profile-drawer-backdrop");
        backdrop.addClickListener(e -> close());
        return backdrop;
    }

    private Div buildHeader() {
        H2 title = new H2("Profile");
        title.getStyle()
                .set("color", "#e2ede6")
                .set("font-size", "1.05rem")
                .set("font-weight", "600")
                .set("letter-spacing", "-0.01em")
                .set("margin", "0");

        Button closeBtn = new Button(new Icon(VaadinIcon.CLOSE));
        closeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        closeBtn.getStyle().set("color", "#4d6654");
        closeBtn.addClickListener(e -> close());

        Div header = new Div(title, closeBtn);
        header.addClassName("profile-drawer-header");
        return header;
    }

    private static FormLayout buildForm(ProfileFields fields) {
        FormLayout form = new FormLayout(
                fields.firstName, fields.lastName, fields.phone, fields.email,
                fields.address, fields.city, fields.postalCode, fields.ticketNumber);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.addClassName("profile-drawer-form");
        form.getStyle().set("--lumo-body-text-color", "#e8edf5");
        return form;
    }

    private static Button buildAdminToggleButton(String adminPassword, AdminControls adminControls,
                                                 String cryptoSecret, String cryptoSalt) {
        Button button = new Button("Toggle Admin Mode");
        button.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        button.setWidthFull();
        button.setVisible(false);
        button.getStyle().set("margin-top", "20px");
        button.addClickListener(e -> new AdminPasswordDialog(
                adminPassword,
                adminControls,
                () -> AdminSessionUtils.saveAdminSession(cryptoSecret, cryptoSalt),
                AdminSessionUtils::clearAdminSession
        ).open());
        return button;
    }

    private static void wireAdminToggleVisibility(Button adminToggleButton,
                                                  TextField firstNameField,
                                                  String adminUsername) {
        firstNameField.addValueChangeListener(e -> {
            boolean matches = e.getValue().trim().equalsIgnoreCase(adminUsername);
            adminToggleButton.setVisible(matches);
        });
    }

    private static Div buildFooter(Button saveButton, Button adminToggleButton) {
        Div footer = new Div(saveButton, adminToggleButton);
        footer.addClassName("profile-drawer-footer");
        return footer;
    }

    private Button buildSaveButton(String cryptoSecret, String cryptoSalt, ProfileFields fields) {
        Button saveButton = new Button("Save", clickEvent -> {
            if (fields.firstName.isInvalid() || fields.lastName.isInvalid() || fields.email.isInvalid()
                    || fields.phone.isInvalid() || fields.postalCode.isInvalid() || fields.address.isInvalid()) {
                Notification error = Notification.show("Please fix the validation errors before saving.");
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }
            String profile = String.format(
                    "{\"firstName\":\"%s\",\"lastName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\",\"address\":\"%s\",\"city\":\"%s\",\"postalCode\":\"%s\",\"ticketNumber\":\"%s\"}",
                    fields.firstName.getValue(), fields.lastName.getValue(),
                    fields.phone.getValue(), fields.email.getValue(),
                    fields.address.getValue(), fields.city.getValue(),
                    fields.postalCode.getValue(), fields.ticketNumber.getValue()
            );
            BrowserStorageUtils.encryptedLocalStorageSave(STORAGE_KEY, profile, cryptoSecret, cryptoSalt);
            Notification.show("Profile saved");
            close();
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.setWidthFull();
        saveButton.getStyle().set("margin", "0");
        return saveButton;
    }

    private static void loadFromStorage(String cryptoSecret, String cryptoSalt,
                                        ProfileFields fields, Runnable onLoaded) {
        BrowserStorageUtils.encryptedLocalStorageLoad(STORAGE_KEY, cryptoSecret, cryptoSalt, json -> {
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(json);
                fields.firstName.setValue(node.path("firstName").asText(""));
                fields.lastName.setValue(node.path("lastName").asText(""));
                fields.phone.setValue(node.path("phone").asText(""));
                fields.email.setValue(node.path("email").asText(""));
                fields.address.setValue(node.path("address").asText(""));
                fields.city.setValue(node.path("city").asText(""));
                fields.postalCode.setValue(node.path("postalCode").asText(""));
                fields.ticketNumber.setValue(node.path("ticketNumber").asText(""));
            } catch (Exception ignored) {
            }
            onLoaded.run();
        });
    }

    private static void applyValidations(ProfileFields fields) {
        addValidation(fields.firstName, "^[\\p{L}\\s\\-']+$", "First name must contain letters only");
        addValidation(fields.lastName, "^[\\p{L}\\s\\-']+$", "Last name must contain letters only");
        addValidation(fields.email, "^[\\w.+\\-]+@[\\w\\-]+(\\.[\\w\\-]+)*\\.[a-zA-Z]{2,}$", "Enter a valid email address (e.g. you@example.com)");
        addValidation(fields.phone, "^[+]?[\\d\\s\\-().]{6,20}$", "Enter a valid phone number (e.g. +46 70 000 00 00)");
        addValidation(fields.postalCode, "^[\\w\\s\\-]{3,10}$", "Enter a valid postal code (e.g. 111 22)");
        addValidation(fields.address, "^[\\p{L}\\d\\s\\-,.']+$", "Enter a valid home address (e.g. 123 Main Street)");
    }

    private static void addValidation(TextField field, String regex, String errorMessage) {
        field.addValueChangeListener(e -> {
            String v = e.getValue();
            if (!v.isBlank() && !v.matches(regex)) {
                field.setErrorMessage(errorMessage);
                field.setInvalid(true);
            } else {
                field.setInvalid(false);
            }
        });
    }

    private void applyDrawerStyles() {
        getStyle()
                .set("--lumo-primary-color", "#4caf7d")
                .set("--lumo-primary-text-color", "#4caf7d")
                .set("--lumo-body-text-color", "#e2ede6")
                .set("--lumo-contrast-60pct", "rgba(226, 237, 230, 0.80)")
                .set("--lumo-contrast-70pct", "rgba(226, 237, 230, 0.90)")
                .set("--lumo-contrast-90pct", "#e2ede6")
                .set("--lumo-error-text-color", "#ff5f6d")
                .set("--lumo-error-color", "#ff5f6d");
    }

    private static TextField styledField(String label, String placeholder) {
        TextField field = new TextField(label);
        field.setPlaceholder(placeholder);
        field.setWidthFull();
        field.getStyle().set("--vaadin-input-field-label-color", "#8aaa92");
        return field;
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
