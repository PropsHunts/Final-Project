package com.ftp_proj.project_ftp_v1.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;

@Route("register")
public class RegisterView extends VerticalLayout {

    public RegisterView() {

        // הגדרות כלליות למסך
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // כרטיס הרשמה
        VerticalLayout card = new VerticalLayout();
        card.setWidth("400px");
        card.setPadding(true);
        card.setSpacing(true);
        card.setAlignItems(Alignment.STRETCH);

        card.getStyle()
                .set("border", "1px solid #e0e0e0")
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 10px rgba(0,0,0,0.1)");

        // כותרת
        H1 title = new H1("הרשמה למערכת");
        Paragraph subtitle = new Paragraph("צור חשבון חדש כדי להתחיל להשתמש בשרת הענן");

        // שדות טופס
        EmailField emailField = new EmailField("אימייל");
        emailField.setPlaceholder("example@email.com");
        emailField.setRequiredIndicatorVisible(true);

        TextField usernameField = new TextField("שם משתמש");
        usernameField.setPlaceholder("הכנס שם משתמש");
        usernameField.setRequiredIndicatorVisible(true);

        PasswordField passwordField = new PasswordField("סיסמה");
        passwordField.setPlaceholder("הכנס סיסמה");
        passwordField.setRequiredIndicatorVisible(true);

        // טופס
        FormLayout formLayout = new FormLayout();
        formLayout.add(emailField, usernameField, passwordField);

        // כפתור הרשמה
        Button registerButton = new Button("הרשמה");
        registerButton.setWidthFull();

        // בעתיד: לוגיקת הרשמה
        /*
        registerButton.addClickListener(e -> {
            String email = emailField.getValue();
            String username = usernameField.getValue();
            String password = passwordField.getValue();

            // בדיקות, שמירה ל-DB, וכו'
        });
        */

        // הוספה לכרטיס
        card.add(title, subtitle, formLayout, registerButton);

        // הוספה למסך
        add(card);
    }
}
