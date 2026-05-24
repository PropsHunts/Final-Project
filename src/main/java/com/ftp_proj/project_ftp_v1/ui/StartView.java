package com.ftp_proj.project_ftp_v1.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = AppNavbarLayout.class)
public class StartView extends VerticalLayout {

    public StartView() {
        // הגדרות רקע ופריסה כללית של הדף
        setSizeFull(); 
        setAlignItems(Alignment.CENTER); 
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #f0f4ff 0%, #e5e7eb 100%)");

        // יצירת הכרטיסייה המרכזית
        Div card = new Div();
        card.getStyle()
            .set("width", "100%")
            .set("max-width", "650px") 
            .set("padding", "40px")
            .set("border-radius", "20px")
            .set("box-shadow", "0 20px 40px rgba(0,0,0,0.1)")
            .set("background-color", "white")
            .set("text-align", "center");

        // לוגו
        Image logo = new Image("images/logo.png", "Logo");
        logo.setWidth("180px");
        logo.getStyle().set("margin-bottom", "15px");

        // כותרת ותת-כותרת
        H1 title = new H1("Cloud FTP Server");
        title.getStyle().set("margin-top", "0").set("color", "#1f2937");

        Paragraph subtitle = new Paragraph("FTP Server + LZ78");
        subtitle.getStyle().set("color", "#4b5563").set("font-size", "1.1rem").set("margin-bottom", "30px");

        // --- תקציר הפרויקט ---
        VerticalLayout featuresList = new VerticalLayout();
        featuresList.setPadding(false);
        featuresList.setSpacing(false);
        featuresList.getStyle()
            .set("text-align", "right") // יישור לימין
            .set("direction", "rtl")    // הגדרת כיוון טקסט לעברית (RTL)
            .set("background", "#f8fafc")
            .set("padding", "20px")
            .set("border-radius", "12px")
            .set("margin-bottom", "30px")
            .set("border", "1px solid #e2e8f0");

        featuresList.add(
            createFeatureItem(VaadinIcon.FILE_ZIP, "שימוש באלגוריתם דחיסה LZ78"),
            createFeatureItem(VaadinIcon.SERVER, "שרת FTP לניהול הקבצים")
            // createFeatureItem(VaadinIcon.ROCKET, "אחסון ענן עד ל7GB בכל העלאה")
        );

        // --- כפתורים ---
        Button loginBtn = new Button("התחברות", VaadinIcon.SIGN_IN.create(), e -> UI.getCurrent().navigate(LoginView.class));
        loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        Button regBtn = new Button("הרשמה", VaadinIcon.USER.create(), e -> UI.getCurrent().navigate(RegisterView.class));
        regBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE); 
        
        HorizontalLayout buttons = new HorizontalLayout(loginBtn, regBtn);
        buttons.setJustifyContentMode(JustifyContentMode.CENTER);
        buttons.getStyle().set("direction", "rtl"); // כדי שכפתור ההתחברות יהיה מימין
        buttons.setWidthFull();

        // הרכבת הכל
        card.add(logo, title, subtitle, featuresList, buttons);
        add(card);
    }

    /**
     * פונקציית עזר ליצירת שורה מעוצבת של אייקון + טקסט
     */
    private HorizontalLayout createFeatureItem(VaadinIcon vaadinIcon, String text) {
        Icon icon = vaadinIcon.create();
        icon.getStyle().set("color", "#2563eb").set("margin-left", "12px"); // margin-left בגלל שזה RTL
        icon.setSize("20px");
        
        Span span = new Span(text);
        span.getStyle().set("color", "#334155").set("font-weight", "500").set("font-size", "0.95rem");
        
        HorizontalLayout layout = new HorizontalLayout(icon, span);
        layout.setAlignItems(Alignment.CENTER);
        layout.getStyle().set("margin-bottom", "12px");
        return layout;
    }
}