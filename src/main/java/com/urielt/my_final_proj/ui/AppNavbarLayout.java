package com.urielt.my_final_proj.ui;

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
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.FtpUiService;
import com.urielt.my_final_proj.services.UserService;
import com.urielt.my_final_proj.services.WeatherService;
import com.urielt.my_final_proj.utils.SessionHelper;

import java.util.List; // הוספנו

/**
 * מחלקה זו מייצגת את תפריט הניווט העליון (Navbar) של האפליקציה.
 * היא מורחבת מ-AppLayout של Vaadin ומשמשת כמעטפת (Layout) לכל שאר המסכים.
 */
public class AppNavbarLayout extends AppLayout {

    // מיכל קישורים (Home, My Cloud) המוצג במרכז
    private final HorizontalLayout navigationContainer = new HorizontalLayout();
    // מיכל צד ימין (התחברות/פרופיל משתמש)
    private final HorizontalLayout rightSectionContainer = new HorizontalLayout();
    
    // קומפוננטות עבור תצוגת מזג האוויר בתוך ה-Navbar
    private final HorizontalLayout weatherContainer = new HorizontalLayout();
    private final Select<City> citySelect = new Select<>();
    private final Span weatherText = new Span();

    /**
     * רשומת עזר פנימית (Record) לייצוג עיר.
     * Record ב-Java 14+ הוא דרך קצרה ליצור מחלקה ששומרת נתונים (Data Class).
     */
    private record City(String name, double latitude, double longitude) {
        // מתודת toString קובעת מה יוצג למשתמש בתוך תיבת הבחירה (Dropdown)
        @Override
        public String toString() { return name; }
    }

    public AppNavbarLayout() {
        createNavbar(); // בונה את השלד של התפריט (לוגו, מיקומים)
        refreshNavbarContent(); // ממלא את התפריט בתוכן דינמי (לפי מצב התחברות)
    }

    /**
     * פונקציה לבניית המבנה הסטטי של התפריט.
     * מגדירה את הלוגו, הכותרת, ואת סדר הרכיבים על המסך.
     */
    private void createNavbar() {
        Image logo = new Image("images/logo.png", "FTP Logo");
        logo.setHeight("50px");

        H1 title = new H1("FTP server");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)")
                .set("margin", "0");

        navigationContainer.setMargin(true);
        navigationContainer.setSpacing(true);

        // מאתחל ומעצב את רכיבי מזג האוויר (ה-Select והטקסט)
        initWeatherWidget();

        HorizontalLayout header = new HorizontalLayout();
        
        // הוספת הרכיבים מצד שמאל למרכז
        header.add(logo, title, navigationContainer, weatherContainer); 

        // הוספת מרווח גמיש שדוחף את ה-rightSectionContainer עד קצה ימין של המסך
        header.addAndExpand(new HorizontalLayout()); 
        header.add(rightSectionContainer);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding", "0 1rem")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        addToNavbar(header); // פקודה של AppLayout שמוסיפה את כל זה לחלק העליון של הדף
    }

    /**
     * אתחול הווידג'ט של מזג האוויר.
     */
    private void initWeatherWidget() {
        weatherContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        weatherContainer.setSpacing(true);
        weatherContainer.getStyle().set("margin-left", "25px").set("direction", "rtl");

        // רשימת ערים בישראל עם קואורדינטות קשיחות (Hardcoded)
        List<City> israelCities = List.of(
            new City("ירושלים", 31.7683, 35.2137),
            new City("תל אביב", 32.0853, 34.7818),
            new City("חיפה", 32.7940, 34.9896),
            new City("באר שבע", 31.2529, 34.7915),
            new City("אילת", 29.5577, 34.9519),
            new City("טבריה", 32.7922, 35.5312)
        );

        citySelect.setItems(israelCities); // טעינת הערים לתוך הרשימה הנגללת
        citySelect.setWidth("120px");
        citySelect.getElement().setAttribute("theme", "small"); 
        
        weatherText.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)");

        weatherText.setEnabled(false); // הופך את הטקסט ללא-לחיץ

        // שליפת ה-Service של מזג האוויר באופן ידני (מכיוון שאנחנו לא ב-Controller רגיל של Spring)
        WeatherService weatherService = VaadinService.getCurrent().getInstantiator()
                .getOrCreate(WeatherService.class);

        // מאזין לשינוי בחירה: בכל פעם שהמשתמש בוחר עיר אחרת
        citySelect.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                City selected = event.getValue();
                // קריאה ל-API (WeatherService) עם הקואורדינטות של העיר שנבחרה
                String temp = weatherService.fetchCurrentWeather(selected.latitude(), selected.longitude());
                weatherText.setText(temp); // עדכון הטקסט בתפריט
            }
        });

        // קביעת ירושלים כברירת מחדל (אינדקס 0) - ה-ValueChangeListener יופעל אוטומטית עכשיו!
        citySelect.setValue(israelCities.get(0));

        weatherContainer.add(citySelect, weatherText);
    }

    /**
     * פונקציה שמתעדכנת בכל מעבר דף או שינוי משתמש.
     * היא בודקת אם יש משתמש מחובר ב-Session, ולפי זה מציגה כפתורים מתאימים.
     */
    public void refreshNavbarContent() {
        navigationContainer.removeAll(); // ניקוי הקישורים הישנים
        rightSectionContainer.removeAll(); // ניקוי כפתורי ימין
        
        rightSectionContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSectionContainer.setSpacing(true);

        // מנסים לשלוף את המשתמש הנוכחי מהזיכרון
        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");

        Avatar userAvatar = new Avatar(); // אייקון של פנים קטנות
        Span greeting = new Span();
        greeting.getStyle().set("font-weight", "bold").set("margin-right", "15px");

        if (currentUser != null) {
            // ----- מצב: משתמש מחובר -----
            userAvatar.setName(currentUser.getUsername());
            greeting.setText("Hello, " + currentUser.getUsername());
            
            navigationContainer.add(new RouterLink("Main", MainView.class));
            navigationContainer.add(new RouterLink("My Cloud", HomeView.class));

            // יצירת כפתור "פרופיל" שיפתח את הדיאלוג כשלוחצים עליו
            Button profileBtn = new Button("Profile", e -> profile());
            
            Button logoutBtn = new Button("Logout", e -> logOut());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR); // צבע אדום

            rightSectionContainer.add(userAvatar, greeting, profileBtn, logoutBtn);
            
        } else {
            // ----- מצב: אורח (לא מחובר) -----
            userAvatar.setName("Guest"); 
            greeting.setText("Hello, Guest");
            
            navigationContainer.add(new RouterLink("Home", MainView.class));

            Button loginBtn = new Button("Login", e -> UI.getCurrent().navigate(LoginView.class));
            loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button registerBtn = new Button("Register", e -> UI.getCurrent().navigate(RegisterView.class));

            rightSectionContainer.add(userAvatar, greeting, loginBtn, registerBtn);
        }
    }

    /**
     * פונקציה סטטית שניתן לקרוא לה מכל מקום באפליקציה (למשל אחרי התחברות מוצלחת).
     * היא מחפשת את ה-Navbar הנוכחי במסך ומרעננת אותו.
     */
    public static void refreshCurrentNavbar() {
        UI.getCurrent().getChildren()
                .filter(component -> component instanceof AppNavbarLayout)
                .map(component -> (AppNavbarLayout) component)
                .findFirst()
                .ifPresent(AppNavbarLayout::refreshNavbarContent);
    }

    /**
     * פונקציה המופעלת בלחיצה על כפתור Profile.
     * היא אחראית ליצור, להציג, ולטפל בתוצאות של חלון הדיאלוג לעריכת פרופיל.
     */
    public void profile() {
        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");
        if (currentUser == null) return; // הגנת סרק

        // שומרים את האימייל המקורי, כדי לדעת מאוחר יותר אם הוא שונה בעריכה
        String originalEmail = currentUser.getEmail();

        // שליפת ה-Services הנחוצים להעברה לדיאלוג
        UserService userService = VaadinService.getCurrent().getInstantiator()
                .getOrCreate(UserService.class);
        FtpUiService ftpService = VaadinService.getCurrent().getInstantiator()
                .getOrCreate(FtpUiService.class);

        // --- יצירת הדיאלוג ---
        // הדיאלוג מקבל 3 פרמטרים: שירות היוזרים, שירות ה-FTP, ופונקציית CallBack (מה יקרה אחרי שמירה מוצלחת)
        ProfileDialog profileDialog = new ProfileDialog(userService, ftpService, () -> {
            
            // הקוד בבלוק הזה ירוץ אך ורק אם השמירה ב-ProfileDialog הצליחה לחלוטין!
            
            User updatedUser = (User) SessionHelper.getAttribute("loggedInUser");

            // 1. האם המשתמש שינה את האימייל שלו?
            if (updatedUser == null || !originalEmail.equalsIgnoreCase(updatedUser.getEmail())) {
                // אם כן, אנחנו מנתקים אותו ומכריחים אותו להתחבר מחדש עם האימייל החדש (לצורכי אבטחה ורענון)
                logOut();
            } else {
                // 2. אם לא שינה אימייל - מרעננים את ה-Navbar (כדי לעדכן את השם ב-Greeting)
                refreshNavbarContent();

                // רענון טבלת הקבצים במסך הבית, במידה והוא פתוח כרגע
                UI.getCurrent().getChildren()
                        .filter(comp -> comp instanceof HomeView)
                        .map(comp -> (HomeView) comp)
                        .findFirst()
                        .ifPresent(homeView -> homeView.beforeEnter(null));
            }
        });

        // מציג את חלון הדיאלוג מול פני המשתמש
        profileDialog.open();
    }

    /**
     * פונקציית התנתקות מהמערכת.
     */
    public void logOut() {
        UI.getCurrent().getPage().setLocation("/"); // הפניה מיידית חזרה לדף הראשי
        SessionHelper.removeAttribute("loggedInUser"); // ניקוי הזיכרון
        VaadinSession.getCurrent().close(); // סגירת הסשן של Vaadin
        SessionHelper.invalidate(); // השמדת ה-Session של Spring
    }
}