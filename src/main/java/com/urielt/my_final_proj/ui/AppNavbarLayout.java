package com.urielt.my_final_proj.ui;

import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.FtpUiService;
import com.urielt.my_final_proj.services.UserService;
import com.urielt.my_final_proj.utils.RouteHelper;
import com.urielt.my_final_proj.utils.SessionHelper;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

public class AppNavbarLayout extends AppLayout {

    private final HorizontalLayout navigationContainer = new HorizontalLayout();
    private final HorizontalLayout rightSectionContainer = new HorizontalLayout();

    public AppNavbarLayout() {
        createNavbar();
        refreshNavbarContent();
    }

    private void createNavbar() {
        Image logo = new Image("images/logo.png", "FTP Logo");
        logo.setHeight("50px");

        H1 title = new H1("FTP server");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        navigationContainer.setMargin(true);
        navigationContainer.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout();
        header.add(logo, title, navigationContainer);

        header.addAndExpand(new HorizontalLayout()); // דוחף את החלק הימני לקצה
        header.add(rightSectionContainer);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding", "0 1rem")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        addToNavbar(header);
    }

    @Override
    protected void onAttach(com.vaadin.flow.component.AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshNavbarContent();
    }

    /**
     * מנקה ומשרטט מחדש את תוכן ה-Navbar לפי מצב הסשן הנוכחי
     */
    public void refreshNavbarContent() {
        navigationContainer.removeAll();
        rightSectionContainer.removeAll();
        
        rightSectionContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSectionContainer.setSpacing(true);

        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");

        // יצירת אלמנטים משותפים (עיגול וטקסט)
        Avatar userAvatar = new Avatar();
        Span greeting = new Span();
        greeting.getStyle().set("font-weight", "bold").set("margin-right", "15px");

        if (currentUser != null) {
            // --- משתמש מחובר ---
            userAvatar.setName(currentUser.getUsername()); // יציג ראשי תיבות של שם המשתמש
            greeting.setText("Hello, " + currentUser.getUsername());
            
            navigationContainer.add(new RouterLink("Main", MainView.class));
            navigationContainer.add(new RouterLink("My Cloud", HomeView.class));

            Button profileBtn = new Button("Profile", e -> profile());
            
            Button logoutBtn = new Button("Logout", e -> logOut());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

            // מוסיפים משמאל לימין: עיגול, טקסט, פרופיל, התנתקות
            rightSectionContainer.add(userAvatar, greeting, profileBtn, logoutBtn);
            
        } else {
            // --- משתמש אורח (Guest) ---
            userAvatar.setName("Guest"); // יציג 'G' בעיגול
            greeting.setText("Hello, Guest");
            
            navigationContainer.add(new RouterLink("Home", MainView.class));

            Button loginBtn = new Button("Login", e -> UI.getCurrent().navigate(LoginView.class));
            loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button registerBtn = new Button("Register", e -> UI.getCurrent().navigate(RegisterView.class));

            // מוסיפים משמאל לימין: עיגול, טקסט, התחברות, הרשמה
            rightSectionContainer.add(userAvatar, greeting, loginBtn, registerBtn);
        }
    }

    /**
     * פונקציית עזר סטטית שמאפשרת לכל דף באפליקציה לרענן את ה-Navbar הנוכחי שעל המסך
     */
    public static void refreshCurrentNavbar() {
        UI.getCurrent().getChildren()
                .filter(component -> component instanceof AppNavbarLayout)
                .map(component -> (AppNavbarLayout) component)
                .findFirst()
                .ifPresent(AppNavbarLayout::refreshNavbarContent);
    }

    public void profile() {
        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");
        if (currentUser == null)
            return;

        String originalEmail = currentUser.getEmail();

        UserService userService = VaadinService.getCurrent().getInstantiator()
                .getOrCreate(UserService.class);
        
        FtpUiService ftpService = VaadinService.getCurrent().getInstantiator()
                .getOrCreate(FtpUiService.class);

        ProfileDialog profileDialog = new ProfileDialog(userService, ftpService, () -> {
            User updatedUser = (User) SessionHelper.getAttribute("loggedInUser");

            // במקרה של שינוי אימייל (או אם המשתמש נעלם) - ניתוק כפוי ונקי
            if (updatedUser == null || !originalEmail.equalsIgnoreCase(updatedUser.getEmail())) {
                logOut();
            } else {
                // במקרה של שינוי שם בלבד: מעדכנים את ה-Navbar מבלי לרענן את כל הדפדפן בכוח
                refreshNavbarContent();
                
                // רענון טבלת הקבצים הקיימת מאחורי הדיאלוג
                UI.getCurrent().getChildren()
                        .filter(comp -> comp instanceof HomeView)
                        .map(comp -> (HomeView) comp)
                        .findFirst()
                        .ifPresent(homeView -> homeView.beforeEnter(null));
            }
        });

        profileDialog.open();
    }

    public void logOut() {
        // 1. קודם כל ולפני הכל: שולחים לדפדפן את פקודת הניווט החוצה (למסך הראשי)!
        // ברגע שהפקודה הזו נשלחת, הלקוח כבר בדרך החוצה.
        RouteHelper.navigateTo(MainView.class); 

        // 2. מנקים את המידע שלנו מהזיכרון
        SessionHelper.removeAttribute("loggedInUser");
        
        // 3. סוגרים את הסשן הפנימי של Vaadin בצורה מסודרת
        VaadinSession.getCurrent().close();
        
        // 4. משמידים לחלוטין את הסשן של השרת (Tomcat/HTTP)
        SessionHelper.invalidate();
    }

}