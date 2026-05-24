package com.ftp_proj.project_ftp_v1.ui;

import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.services.FtpUiService;
import com.ftp_proj.project_ftp_v1.services.UserService;
import com.ftp_proj.project_ftp_v1.utils.SessionHelper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

public class ProfileDialog extends Dialog {

    private final UserService userService;
    private final FtpUiService ftpService;
    private final Runnable onUpdateSuccess;

    // רכיבי ה-UI שנצטרך לגשת אליהם בין המתודות
    private TextField usernameField;
    private EmailField emailField;
    private PasswordField newPasswordField;
    private VerticalLayout passwordSection;
    private Button saveBtn;
    private Button changePasswordBtn;

    // משתנים לשמירת המצב המקורי כדי לדעת אם משהו השתנה
    private String originalUsername;
    private String originalEmail;
    private boolean isPasswordSectionActive = false;

    public ProfileDialog(UserService userService, FtpUiService ftpService, Runnable onUpdateSuccess) {
        this.userService = userService;
        this.ftpService = ftpService;
        this.onUpdateSuccess = onUpdateSuccess;

        setHeaderTitle("User Profile Settings");
        
        // מניעת סגירת החלון בלחיצה מחוץ לדיאלוג (Esc או קליק ברקע) כדי שלא יאבדו שינויים בטעות
        setCloseOnOutsideClick(false);
        setCloseOnEsc(false);

        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");
        if (currentUser != null) {
            // שמירת המצב המקורי של המשתמש
            this.originalUsername = currentUser.getUsername();
            this.originalEmail = currentUser.getEmail();
            
            buildContent(currentUser);
        } else {
            add("No active user session found.");
        }
    }

    private void buildContent(User user) {
        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);

        usernameField = new TextField("Username");
        usernameField.setValue(originalUsername);
        usernameField.setWidthFull();

        emailField = new EmailField("Email Address");
        emailField.setValue(originalEmail);
        emailField.setWidthFull();

        // אזור סיסמה מוסתר
        changePasswordBtn = new Button("Change Password");
        passwordSection = new VerticalLayout();
        passwordSection.setVisible(false);
        passwordSection.setPadding(false);

        newPasswordField = new PasswordField("New Password");
        newPasswordField.setWidthFull();
        passwordSection.add(newPasswordField);

        // מאזינים לשינוי ערך בכל אחד מהשדות כדי לבדוק בזמן אמת אם כפתור השמירה צריך להידלק
        usernameField.addValueChangeListener(e -> checkChanges());
        emailField.addValueChangeListener(e -> checkChanges());
        newPasswordField.addValueChangeListener(e -> checkChanges());

        changePasswordBtn.addClickListener(e -> {
            isPasswordSectionActive = !isPasswordSectionActive;
            passwordSection.setVisible(isPasswordSectionActive);
            changePasswordBtn.setText(isPasswordSectionActive ? "Cancel Password Change" : "Change Password");
            if (!isPasswordSectionActive) {
                newPasswordField.clear(); // מנקה את השדה אם התחרט וסגר
            }
            checkChanges(); // בדיקה מחדש אם כפתור השמירה צריך להשתנות
        });

        // יצירת כפתור שמירה (כחול) - מתחיל כ-Disabled (אפור מנוטרל)
        saveBtn = new Button("Save Changes");
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveBtn.setEnabled(false); 

        saveBtn.addClickListener(e -> handleSave(user));

        // כפתור ביטול
        Button cancelBtn = new Button("Cancel", e -> handleCancel());

        HorizontalLayout actions = new HorizontalLayout(saveBtn, cancelBtn);
        
        layout.add(usernameField, emailField, changePasswordBtn, passwordSection, actions);
        add(layout);
    }

    /**
     * פונקציה הבודקת בזמן אמת האם חל שינוי כלשהו בטופס לעומת המצב המקורי.
     * אם יש שינוי - כפתור השמירה מופעל (הופך לכחול). אם אין שינוי - הוא מנוטרל (אפור).
     */
    private void checkChanges() {
        String currentUsername = usernameField.getValue().trim();
        String currentEmail = emailField.getValue().trim();
        String currentPassword = newPasswordField.getValue();

        boolean usernameChanged = !currentUsername.equals(originalUsername);
        boolean emailChanged = !currentEmail.equals(originalEmail);
        boolean passwordChanged = isPasswordSectionActive && !currentPassword.isEmpty();

        // כפתור השמירה יהיה פעיל (Enabled) רק אם לפחות שדה אחד השתנה בפועל
        boolean hasAnyChange = usernameChanged || emailChanged || passwordChanged;
        saveBtn.setEnabled(hasAnyChange);
    }

    /**
     * פונקציה המנהלת את הלוגיקה של כפתור הביטול או סגירת החלון
     */
    private void handleCancel() {
        // אם כפתור השמירה דולק/פעיל, זה אומר שיש שינויים שלא נשמרו!
        if (saveBtn.isEnabled()) {
            // פתיחת דיאלוג משני קטן השואל לאישור
            Dialog confirmDialog = new Dialog();
            confirmDialog.setHeaderTitle("Unsaved Changes");
            confirmDialog.add(new VerticalLayout(new com.vaadin.flow.component.html.Span(
                    "You have unsaved changes. Are you sure you want to leave without saving?")));

            Button stayBtn = new Button("Stay", e -> confirmDialog.close());
            Button leaveBtn = new Button("Discard & Leave", e -> {
                confirmDialog.close();
                this.close(); // סגירת חלון הפרופיל הראשי
            });
            leaveBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

            confirmDialog.getFooter().add(stayBtn, leaveBtn);
            confirmDialog.open();
        } else {
            // אם לא נעשה שום שינוי, אפשר לסגור ישירות בלי להציק למשתמש
            this.close();
        }
    }

    private void handleSave(User user) {
        String oldEmail = user.getEmail();
        String newUsername = usernameField.getValue().trim();
        String newEmail = emailField.getValue().trim();

        if (newUsername.isEmpty() || newEmail.isEmpty()) {
            Notification.show("Fields cannot be empty.");
            return;
        }

        // 1. עדכון הפרופיל ב-DB
        boolean success = userService.updateProfile(user, newUsername, newEmail);
        
        if (success) {
            // 2. עדכון סיסמה אם השדה פתוח ומלא
            if (isPasswordSectionActive && !newPasswordField.getValue().isEmpty()) {
                user.setPassword(newPasswordField.getValue());
                userService.updateProfile(user, user.getUsername(), user.getEmail());
            }

            // 3. עדכון קבצים במידה והאימייל השתנה
            if (!oldEmail.equalsIgnoreCase(newEmail)) {
                ftpService.updateFilesOwnerEmail(oldEmail, newEmail);
            }

            // 4. עדכון הסשן
            SessionHelper.setAttribute("loggedInUser", user);
            
            Notification.show("Profile updated successfully!");
            onUpdateSuccess.run(); 
            this.close();
        } else {
            Notification.show("Username/Email already taken.");
        }
    }
}