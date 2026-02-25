package com.hs.railway_stats.view.component;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.PasswordField;

public class AdminPasswordDialog extends Dialog {

    public AdminPasswordDialog(String adminPassword, Button adminCollectButton, Span adminBanner,
                               Runnable onEnable, Runnable onDisable) {
        setHeaderTitle("Admin Mode");
        setWidth("360px");

        PasswordField passwordField = new PasswordField("Admin Password");
        passwordField.setPlaceholder("Enter password");
        passwordField.setWidthFull();

        addDialogCloseActionListener(closeActionEvent -> close());

        Button confirmButton = new Button("Confirm", clickEvent -> {
            if (adminPassword.equals(passwordField.getValue())) {
                boolean nowVisible = !adminCollectButton.isVisible();
                adminCollectButton.setVisible(nowVisible);
                adminBanner.setVisible(nowVisible);
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
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", clickEvent -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        passwordField.addKeyDownListener(Key.ENTER, keyDownEvent -> confirmButton.click());

        add(passwordField);
        getFooter().add(cancelButton, confirmButton);
    }
}

