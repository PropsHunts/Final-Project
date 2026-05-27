package com.urielt.my_final_proj.ui;
import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.UserService;
import com.urielt.my_final_proj.utils.RouteHelper;
import com.urielt.my_final_proj.utils.SessionHelper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "login", layout = AppNavbarLayout.class)
public class LoginView extends VerticalLayout {
    public LoginView(UserService userService) {
        setSizeFull(); setAlignItems(Alignment.CENTER); setJustifyContentMode(JustifyContentMode.CENTER);
        
        LoginForm login = new LoginForm();
        
        login.addLoginListener(e -> {
            User user = userService.loginUser(e.getUsername(), e.getPassword());
            if (user != null) {
                SessionHelper.setAttribute("loggedInUser", user);
                Notification.show("התחברת בהצלחה!");
                RouteHelper.navigateTo(HomeView.class);
            } else { login.setError(true); login.setEnabled(true); }
        });

        add(new H1("התחברות"), login, new Button("הרשמה", e -> UI.getCurrent().navigate(RegisterView.class)));
    }
}   