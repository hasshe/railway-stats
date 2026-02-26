package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;

public class AdminPasswordDialog extends Dialog {

    public AdminPasswordDialog(String adminPassword, AdminControls adminControls,
                               Runnable onEnable, Runnable onDisable) {
        setHeaderTitle("Admin Mode");
        setWidth("360px");

        PasswordField passwordField = getPasswordField();

        FormLayout formLayout = new FormLayout(passwordField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        addDialogCloseActionListener(closeActionEvent -> close());

        Button confirmButton = getConfirmButton(adminPassword, adminControls, onEnable, onDisable, passwordField);
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", clickEvent -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        passwordField.addKeyDownListener(Key.ENTER, keyDownEvent -> confirmButton.click());

        add(formLayout);
        getFooter().add(cancelButton, confirmButton);
    }

    private Button getConfirmButton(String adminPassword, AdminControls adminControls,
                                    Runnable onEnable, Runnable onDisable, PasswordField passwordField) {
        Button confirmButton = new Button("Confirm", clickEvent -> {
            if (adminPassword.equals(passwordField.getValue())) {
                boolean nowVisible = !adminControls.getAdminCollectButton().isVisible();
                adminControls.setAdminVisible(nowVisible);
                if (nowVisible) {
                    onEnable.run();
                    Notification.show("Admin mode enabled");
                } else {
                    onDisable.run();
                    Notification.show("Admin mode disabled");
                }
            } else {
                Notification.show("Incorrect password");
                passwordField.setInvalid(true);
                passwordField.setErrorMessage("Wrong password");
            }
            close();
        });
        return confirmButton;
    }

    private static PasswordField getPasswordField() {
        PasswordField passwordField = new PasswordField("Admin Password");
        passwordField.setPlaceholder("Enter password");
        passwordField.setWidthFull();
        return passwordField;
    }
}
