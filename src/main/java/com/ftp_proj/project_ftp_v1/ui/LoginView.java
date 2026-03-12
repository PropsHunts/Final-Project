package com.ftp_proj.project_ftp_v1.ui;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.services.UserService;

@Route("login")
public class LoginView extends VerticalLayout {
    public LoginView(UserService userService) {
        setSizeFull(); setAlignItems(Alignment.CENTER); setJustifyContentMode(JustifyContentMode.CENTER);
        
        LoginForm login = new LoginForm();
        login.addLoginListener(e -> {
            User user = userService.loginUser(e.getUsername(), e.getPassword());
            if (user != null) {
                VaadinSession.getCurrent().setAttribute("loggedInUser", user);
                Notification.show("התחברת בהצלחה!");
                UI.getCurrent().navigate(HomeView.class);
            } else { login.setError(true); login.setEnabled(true); }
        });

        add(new H1("התחברות"), login, new Button("הרשמה", e -> UI.getCurrent().navigate(RegisterView.class)));
    }
}   