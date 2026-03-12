package com.ftp_proj.project_ftp_v1.ui;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.Route;

@Route("")
public class MainView extends VerticalLayout {
    public MainView() {
        setSizeFull(); setAlignItems(Alignment.CENTER); setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #e0e7ff, #f8fafc)");

        Div card = new Div();
        card.getStyle().set("width", "600px").set("padding", "40px").set("border-radius", "16px")
            .set("box-shadow", "0 10px 30px rgba(0,0,0,0.1)").set("background-color", "white").set("text-align", "center");

        card.add(new H1("Cloud FTP Server"), new Paragraph("אחסון חכם, מהיר ומכווץ עם LZ78."));
        Button loginBtn = new Button("התחברות", e -> UI.getCurrent().navigate(LoginView.class));
        Button regBtn = new Button("הרשמה", e -> UI.getCurrent().navigate(RegisterView.class));
        loginBtn.getStyle().set("background-color", "#2563eb").set("color", "white");
        regBtn.getStyle().set("background-color", "#16a34a").set("color", "white");
        
        HorizontalLayout buttons = new HorizontalLayout(loginBtn, regBtn);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        card.add(buttons); add(card);
    }
}