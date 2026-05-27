package com.urielt.my_final_proj.ui;

import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = AppNavbarLayout.class)
public class MainView extends VerticalLayout {

    public MainView() {
        // הגדרות רקע ופריסה כללית של הדף
        setSizeFull(); 
        setAlignItems(Alignment.CENTER); 
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(135deg, #f0f4ff 0%, #e5e7eb 100%)");

        // יצירת הכרטיסייה המרכזית (הכרטיס הראשי)
        Div card = new Div();
        card.getStyle()
            .set("width", "100%")
            .set("max-width", "650px") 
            .set("padding", "40px")
            .set("border-radius", "20px")
            .set("box-shadow", "0 20px 40px rgba(0,0,0,0.1)")
            .set("background-color", "white")
            .set("text-align", "center");

        // 1. שם הפרויקט
        H1 title = new H1("שרת FTP להעלאת והורדת קבצים");
        title.getStyle()
            .set("margin-top", "0")
            .set("margin-bottom", "20px")
            .set("color", "#1f2937")
            .set("font-size", "1.8rem")
            .set("direction", "rtl");

        // 2. לוגו
        Image logo = new Image("images/logo.png", "FTP Cloud Logo");
        logo.setWidth("160px");
        logo.getStyle().set("margin-bottom", "20px");

        // 3. הסבר ב-2 שורות על הפרויקט
        Paragraph projectDesc = new Paragraph(
            "פלטפורמת ענן מתקדמת המאפשרת ניהול ואחסון קבצים מאובטח באמצעות שרת FTP עצמאי המבוסס על טכנולוגיית הזרמת מידע חכמה. " +
            "המערכת משלבת את אלגוריתם הדחיסה LZ78 המבצע כיווץ ופריסה של הנתונים בזמן אמת (On-the-fly) לשמירה על טביעת זיכרון מינימלית."
        );
        projectDesc.getStyle()
            .set("color", "#4b5563")
            .set("font-size", "1.05rem")
            .set("line-height", "1.6")
            .set("margin-bottom", "25px")
            .set("padding", "0 10px")
            .set("direction", "rtl");

        // פס הפרדה מעוצב לפני פרטי המגיש
        Hr divider = new Hr();
        divider.getStyle().set("border", "none").set("border-top", "1px solid #e2e8f0").set("margin-bottom", "20px");

        // 4 + 5. פרטי סטודנט ומוסד לימודים
        VerticalLayout detailsLayout = new VerticalLayout();
        detailsLayout.setPadding(false);
        detailsLayout.setSpacing(true);
        detailsLayout.setAlignItems(Alignment.CENTER);
        detailsLayout.getStyle().set("direction", "rtl");

        H4 studentNameAndId = new H4("אוריאל טוטיאשוילי ת.ז 215750779");
        studentNameAndId.getStyle().set("color", "#1e293b").set("margin", "0").set("font-size", "1rem");

        Span collegeName = new Span("כנפי רוח קרית נוער ירושלים");
        collegeName.getStyle().set("color", "#64748b").set("font-weight", "500").set("font-size", "0.95rem");

        detailsLayout.add(studentNameAndId, collegeName);

        // הרכבת כל הרכיבים לתוך הכרטיס לפי הסדר המדויק
        card.add(title, logo, projectDesc, divider, detailsLayout);
        add(card);
    }
}