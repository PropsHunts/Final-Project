package com.urielt.my_final_proj.ui;

import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.UserService;
import com.urielt.my_final_proj.utils.PasswordHelper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;

@Route(value = "register", layout = AppNavbarLayout.class)
public class RegisterView extends VerticalLayout {

    private final UserService userService;

    public RegisterView(UserService userService) {
        this.userService = userService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Create Account");

        TextField usernameField = new TextField("Username");
        usernameField.setRequiredIndicatorVisible(true);
        usernameField.setMinLength(3);
        usernameField.setErrorMessage("Minimum 3 characters");

        EmailField emailField = new EmailField("Email");
        emailField.setRequiredIndicatorVisible(true);
        emailField.setErrorMessage("Enter a valid email address");

        PasswordField passwordField = new PasswordField("Password");
        passwordField.setRequiredIndicatorVisible(true);
        passwordField.setMinLength(6);
        passwordField.setErrorMessage("Minimum 6 characters");

        Button registerBtn = new Button("Register");
        registerBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        registerBtn.setEnabled(false); // כבוי כברירת מחדל עד שהכל תקין

        // הפעלת בדיקה בכל הקלדה (EAGER)
        usernameField.setValueChangeMode(ValueChangeMode.EAGER);
        emailField.setValueChangeMode(ValueChangeMode.EAGER);
        passwordField.setValueChangeMode(ValueChangeMode.EAGER);

        // פונקציית הבדיקה שמדליקה את הכפתור
        Runnable checkValid = () -> {
            boolean isFormValid = !usernameField.isInvalid() && !usernameField.isEmpty() &&
                    !emailField.isInvalid() && !emailField.isEmpty() &&
                    !passwordField.isInvalid() && !passwordField.isEmpty();
            registerBtn.setEnabled(isFormValid);
        };

        // הצמדת הפונקציה לכל הקלדה בשדות
        usernameField.addValueChangeListener(e -> checkValid.run());
        emailField.addValueChangeListener(e -> checkValid.run());
        passwordField.addValueChangeListener(e -> checkValid.run());

        registerBtn.addClickListener(e -> {
            String email = emailField.getValue();
            String username = usernameField.getValue();
            String password = passwordField.getValue();

            // 1. בדיקה האם האימייל כבר קיים במערכת
            if (userService.isEmailTaken(email)) { // שים את שם הפונקציה המדויק שיש לך ב-Service
                Notification.show("Email already exists. Please choose a different one.", 4000,
                        Notification.Position.MIDDLE);
                return; // עוצרים כאן, לא ממשיכים להרשמה!
            }

            // 2. בדיקה האם שם המשתמש כבר קיים במערכת
            if (userService.isUsernameTaken(username)) { // הפונקציה החדשה שיצרנו
                Notification.show("Username already taken. Please choose a different one.", 4000,
                        Notification.Position.MIDDLE);
                return; // עוצרים כאן!
            }

            // 3. אם הגענו לפה, גם האימייל וגם שם המשתמש פנויים! אפשר לשמור
            User newUser = new User(email, username, PasswordHelper.getInstance().encode(password));
            boolean success = userService.addUserToDB(newUser);

            if (success) {
                Notification.show("Registration successful! Please log in.");
                UI.getCurrent().navigate(LoginView.class);
            } else {
                Notification.show("Registration failed due to a server error. Please try again.");
            }
        });

        Button loginBtn = new Button("Already have an account? Log in", e -> UI.getCurrent().navigate(LoginView.class));
        loginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        VerticalLayout formLayout = new VerticalLayout(title, usernameField, emailField, passwordField, registerBtn,
                loginBtn);
        formLayout.setAlignItems(Alignment.CENTER);
        formLayout.getStyle()
                .set("border", "1px solid var(--lumo-contrast-20pct)")
                .set("padding", "2rem")
                .set("border-radius", "8px")
                .set("box-shadow", "var(--lumo-box-shadow-m)");

        add(formLayout);
    }
}