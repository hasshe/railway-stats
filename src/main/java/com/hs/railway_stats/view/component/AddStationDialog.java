package com.hs.railway_stats.view.component;

import com.hs.railway_stats.service.TranslationService;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;

public class AddStationDialog extends Dialog {

    public AddStationDialog(TranslationService translationService) {
        setHeaderTitle("Add Station");
        setWidth("380px");

        IntegerField stationIdField = getStationIdField();

        TextField stationNameField = getStationNameField();

        TextField claimsStationIdField = getClaimsStationIdField();

        FormLayout formLayout = new FormLayout(stationIdField, stationNameField, claimsStationIdField);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        Button saveButton = getSaveButton(translationService, stationIdField, stationNameField, claimsStationIdField);
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Cancel", clickEvent -> close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        stationNameField.addKeyDownListener(Key.ENTER, e -> saveButton.click());
        claimsStationIdField.addKeyDownListener(Key.ENTER, e -> saveButton.click());

        addDialogCloseActionListener(e -> close());

        add(formLayout);
        getFooter().add(cancelButton, saveButton);
    }

    private static IntegerField getStationIdField() {
        IntegerField stationIdField = new IntegerField("Station ID");
        stationIdField.setPlaceholder("e.g. 74100");
        stationIdField.setWidthFull();
        stationIdField.setMin(1);
        return stationIdField;
    }

    private static TextField getStationNameField() {
        TextField stationNameField = new TextField("Station Name");
        stationNameField.setPlaceholder("e.g. Arlanda C");
        stationNameField.setWidthFull();
        return stationNameField;
    }

    private static TextField getClaimsStationIdField() {
        TextField claimsStationIdField = new TextField("Claims Station ID (Optional)");
        claimsStationIdField.setPlaceholder("e.g. f4d25596-a9f9-41a1-b200-713439d92fc4");
        claimsStationIdField.setWidthFull();
        claimsStationIdField.setHelperText("UUID format for claims system integration");
        return claimsStationIdField;
    }

    private Button getSaveButton(TranslationService translationService, IntegerField stationIdField, TextField stationNameField, TextField claimsStationIdField) {
        return new Button("Save", clickEvent -> {
            Integer stationId = stationIdField.getValue();
            String stationName = stationNameField.getValue();
            String claimsStationId = claimsStationIdField.getValue();

            if (stationId == null || stationId <= 0) {
                stationIdField.setInvalid(true);
                stationIdField.setErrorMessage("Please enter a valid station ID");
                return;
            }
            if (stationName == null || stationName.isBlank()) {
                stationNameField.setInvalid(true);
                stationNameField.setErrorMessage("Station name cannot be empty");
                return;
            }

            // Validate UUID format if provided
            if (claimsStationId != null && !claimsStationId.isBlank()) {
                String trimmedClaimsId = claimsStationId.trim();
                if (!isValidUUID(trimmedClaimsId)) {
                    claimsStationIdField.setInvalid(true);
                    claimsStationIdField.setErrorMessage("Invalid UUID format");
                    return;
                }
                claimsStationId = trimmedClaimsId;
            } else {
                claimsStationId = null;
            }

            try {
                translationService.addStation(stationId, stationName.trim(), claimsStationId);
                Notification success = Notification.show("Station '" + stationName.trim() + "' (ID: " + stationId + ") added successfully.");
                success.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                close();
            } catch (IllegalArgumentException e) {
                Notification error = Notification.show(e.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (Exception e) {
                Notification error = Notification.show("Failed to add station: " + e.getMessage());
                error.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
    }

    private static boolean isValidUUID(String uuid) {
        if (uuid == null) return false;
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        return uuid.matches(uuidPattern);
    }
}

