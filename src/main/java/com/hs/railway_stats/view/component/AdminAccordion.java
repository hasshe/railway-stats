package com.hs.railway_stats.view.component;

import com.hs.railway_stats.view.util.AdminSessionUtils;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

public class AdminAccordion extends Accordion {

    public AdminAccordion(String adminPassword, String adminUsername,
                          AdminControls adminControls,
                          String cryptoSecret, String cryptoSalt) {
        TextField usernameField = getUsernameField();

        PasswordField passwordField = getPasswordField();

        Button loginButton = new Button("Login as Admin", clickEvent -> {
            buildLoginButtonClickEvent(adminPassword, adminUsername, adminControls, cryptoSecret, cryptoSalt, usernameField, passwordField);
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        loginButton.setWidthFull();

        FormLayout adminForm = new FormLayout(usernameField, passwordField, loginButton);
        adminForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        add("Admin Mode", adminForm);
        close();
        setWidthFull();
        getStyle()
                .set("margin-top", "16px")
                .set("--lumo-body-text-color", "#e2ede6")
                .set("--lumo-contrast-90pct", "#e2ede6")
                .set("color", "#e2ede6");
    }

    private static void buildLoginButtonClickEvent(String adminPassword, String adminUsername, AdminControls adminControls, String cryptoSecret, String cryptoSalt, TextField usernameField, PasswordField passwordField) {
        boolean usernameMatches = usernameField.getValue().trim().equalsIgnoreCase(adminUsername);
        boolean passwordMatches = adminPassword.equals(passwordField.getValue());

        if (usernameMatches && passwordMatches) {
            boolean nowVisible = !adminControls.getAdminCollectButton().isVisible();
            adminControls.setAdminVisible(nowVisible);
            if (nowVisible) {
                AdminSessionUtils.saveAdminSession(cryptoSecret, cryptoSalt);
                Notification.show("Admin mode enabled");
            } else {
                AdminSessionUtils.clearAdminSession();
                Notification.show("Admin mode disabled");
            }
            usernameField.clear();
            passwordField.clear();
        } else {
            Notification.show("Incorrect username or password");
        }
    }

    private static TextField getUsernameField() {
        TextField usernameField = new TextField("Username");
        usernameField.setPlaceholder("Admin username");
        usernameField.setWidthFull();
        usernameField.getStyle().set("--vaadin-input-field-label-color", "#8aaa92");
        return usernameField;
    }

    private static PasswordField getPasswordField() {
        PasswordField passwordField = new PasswordField("Password");
        passwordField.setPlaceholder("Admin password");
        passwordField.setWidthFull();
        passwordField.getStyle().set("--vaadin-input-field-label-color", "#8aaa92");
        return passwordField;
    }
}

