package com.ftp_proj.project_ftp_v1.ui;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.services.UserService;

@Route("/login")
public class LoginView extends VerticalLayout {
    private UserService userService;
    private LoginForm loginForm;

    public LoginView(UserService userService) {
        this.userService = userService;
        loginForm = new LoginForm();
        loginForm.setError(false);
        loginForm.addLoginListener(event -> {
            String username = event.getUsername();
            String password = event.getPassword();
            if (!Login(username, password)) {
                loginForm.setEnabled(true);
            }
        });
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        add(loginForm);
    }

    private Boolean Login(String un, String pw) {
        Notification notify = new Notification("", 5000, Position.TOP_CENTER);
        Boolean signin = true;
        User user = userService.getUser(un, pw);
        if (user != null) {
            notify.setText("Successfully Logged in!");
            notify.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            // Store username in Vaadin session
            // SessionHelper.setAttribute("USER", user);
            // // Navigate to HomeView route
            // RouteHelper.navigateTo(UserView.class);
        } else {
            notify.setText("User not found!");
            notify.addThemeVariants(NotificationVariant.LUMO_ERROR);
            loginForm.setError(true);
            signin = false;
        }

        notify.open();
        return signin;
    }
}
