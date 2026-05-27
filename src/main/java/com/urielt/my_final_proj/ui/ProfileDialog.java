package com.urielt.my_final_proj.ui;

import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.FtpUiService;
import com.urielt.my_final_proj.services.UserService;
import com.urielt.my_final_proj.utils.SessionHelper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;

public class ProfileDialog extends Dialog {

    private final UserService userService;
    private final FtpUiService ftpService;
    private final Runnable onSuccessCallback;

    private TextField usernameField;
    private EmailField emailField;
    private PasswordField passwordField;
    private Button saveBtn;

    private String originalUsername;
    private String originalEmail;

    public ProfileDialog(UserService userService, FtpUiService ftpService, Runnable onSuccessCallback) {
        this.userService = userService;
        this.ftpService = ftpService;
        this.onSuccessCallback = onSuccessCallback;

        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");
        if (currentUser != null) {
            this.originalUsername = currentUser.getUsername();
            this.originalEmail = currentUser.getEmail();
            buildUI(currentUser);
        }
    }

    private void buildUI(User user) {
        H2 title = new H2("Edit Profile");

        // הגדרת השדות והבדיקות עליהם (Validation)
        usernameField = new TextField("Username");
        usernameField.setValue(user.getUsername());
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setMinLength(3);
        usernameField.setErrorMessage("Minimum 3 characters");

        emailField = new EmailField("Email");
        emailField.setValue(user.getEmail());
        emailField.setRequiredIndicatorVisible(true);
        emailField.setErrorMessage("Enter a valid email address");

        passwordField = new PasswordField("New Password (Leave empty to keep current)");
        passwordField.setMinLength(6);
        passwordField.setErrorMessage("Password must be at least 6 characters");

        // הפעלת בדיקה בכל הקלדה (EAGER)
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);
        emailField.setValueChangeMode(ValueChangeMode.EAGER);
        passwordField.setValueChangeMode(ValueChangeMode.EAGER);

        saveBtn = new Button("Save Changes", e -> handleSave(user));
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.setEnabled(false); // כבוי כברירת מחדל

        Button cancelBtn = new Button("Cancel", e -> close());

        // מאזינים לשינויים בכל השדות
        // בכל פעם שיש שינוי בשדה שם המשתמש -> תפעיל את פונקציית הבדיקה
        usernameField.addValueChangeListener(event -> validateAndEnableSaveButton());

        // בכל פעם שיש שינוי בשדה האימייל -> תפעיל את פונקציית הבדיקה
        emailField.addValueChangeListener(event -> validateAndEnableSaveButton());

        // בכל פעם שיש שינוי בשדה הסיסמה -> תפעיל את פונקציית הבדיקה
        passwordField.addValueChangeListener(event -> validateAndEnableSaveButton());

        VerticalLayout layout = new VerticalLayout(title, usernameField, emailField, passwordField);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);

        HorizontalLayout buttonsLayout = new HorizontalLayout(cancelBtn, saveBtn);
        buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        add(layout, buttonsLayout);
    }

    private void validateAndEnableSaveButton() {
        // בודקים אם משהו השתנה לעומת המקור
        boolean isUsernameChanged = !usernameField.getValue().equals(originalUsername);
        boolean isEmailChanged = !emailField.getValue().equals(originalEmail);
        boolean isPasswordChanged = !passwordField.getValue().isEmpty();
        boolean isSomethingChanged = isUsernameChanged || isEmailChanged || isPasswordChanged;

        // בודקים שהשדות תקינים חוקית (לא ריקים ולא שוברים את חוקי ה-Email/אורך מינימלי)
        boolean isFormValid = !usernameField.isInvalid() && !usernameField.isEmpty() &&
                !emailField.isInvalid() && !emailField.isEmpty() &&
                !passwordField.isInvalid();

        // מדליקים את הכפתור רק אם יש שינוי והכל תקין
        saveBtn.setEnabled(isSomethingChanged && isFormValid);
    }

    private void handleSave(User user) {
        String newUsername = usernameField.getValue();
        String newEmail = emailField.getValue();
        String newPassword = passwordField.getValue();

        // אם הוזנה סיסמה חדשה, נעדכן אותה בזיכרון (נשמר במסד אח"כ)
        if (!newPassword.isEmpty()) {
            user.setPassword(newPassword);
        }

        // ניסיון שמירה מול ה-DB
        boolean success = userService.updateProfile(user, newUsername, newEmail);

        if (success) {
            // אם האימייל שונה, צריך לעדכן את כל הקבצים שלו בשרת
            if (!originalEmail.equalsIgnoreCase(newEmail)) {
                ftpService.updateFilesOwnerEmail(originalEmail, newEmail);
            }
            SessionHelper.setAttribute("loggedInUser", user);
            Notification.show("Profile updated successfully!", 3000, Notification.Position.BOTTOM_CENTER);
            close();
            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
        } else {
            Notification.show("Email is already in use by another account.", 4000, Notification.Position.MIDDLE);
        }
    }
}