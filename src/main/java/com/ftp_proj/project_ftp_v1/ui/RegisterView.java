package com.ftp_proj.project_ftp_v1.ui;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.*;
import com.vaadin.flow.router.Route;
import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.services.UserService;

@Route("register")
public class RegisterView extends VerticalLayout {
    public RegisterView(UserService userService) {
        setSizeFull(); setAlignItems(Alignment.CENTER); setJustifyContentMode(JustifyContentMode.CENTER);
        
        EmailField email = new EmailField("אימייל");
        TextField user = new TextField("שם תצוגה");
        PasswordField pass = new PasswordField("סיסמה");

        Button regBtn = new Button("הרשם", e -> {
            if (userService.addUserToDB(new User(email.getValue(), user.getValue(), pass.getValue()))) {
                Notification.show("נרשמת בהצלחה!"); UI.getCurrent().navigate(LoginView.class);
            } else { Notification.show("אימייל קיים במערכת!"); }
        });

        add(new H1("הרשמה"), email, user, pass, regBtn, new Button("חזור ללוגין", e -> UI.getCurrent().navigate(LoginView.class)));
    }
}