package com.urielt.my_final_proj.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox; // הוספנו
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.FtpUiService;
import com.urielt.my_final_proj.services.UserService;
import com.urielt.my_final_proj.services.WeatherService; // הוספנו
import com.urielt.my_final_proj.utils.SessionHelper;

import java.util.List; // הוספנו

public class AppNavbarLayout extends AppLayout {

    private final HorizontalLayout navigationContainer = new HorizontalLayout();
    private final HorizontalLayout rightSectionContainer = new HorizontalLayout();
    
    // קומפוננטות חדשות עבור תצוגת מזג האוויר בתוך ה-Navbar
    private final HorizontalLayout weatherContainer = new HorizontalLayout();
    private final ComboBox<City> citySelect = new ComboBox<>();
    private final Span weatherText = new Span();

    // רשומת עזר פנימית לייצוג עיר
    private record City(String name, double latitude, double longitude) {
        @Override
        public String toString() { return name; }
    }

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

        // מאתחל ומעצב את רכיבי מזג האוויר
        initWeatherWidget();

        HorizontalLayout header = new HorizontalLayout();
        // הוספת ה-weatherContainer מיד אחרי כפתורי הניווט הראשיים
        header.add(logo, title, navigationContainer, weatherContainer); 

        header.addAndExpand(new HorizontalLayout()); 
        header.add(rightSectionContainer);

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setWidthFull();
        header.getStyle().set("padding", "0 1rem")
                .set("background-color", "var(--lumo-base-color)")
                .set("box-shadow", "var(--lumo-box-shadow-s)");

        addToNavbar(header);
    }

    private void initWeatherWidget() {
        weatherContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        weatherContainer.setSpacing(true);
        weatherContainer.getStyle().set("margin-left", "25px").set("direction", "rtl");

        // רשימת ערים בארץ בלבד
        List<City> israelCities = List.of(
            new City("ירושלים", 31.7683, 35.2137),
            new City("תל אביב", 32.0853, 34.7818),
            new City("חיפה", 32.7940, 34.9896),
            new City("באר שבע", 31.2529, 34.7915),
            new City("אילת", 29.5577, 34.9519),
            new City("טבריה", 32.7922, 35.5312)
        );

        citySelect.setItems(israelCities);
        citySelect.setPlaceholder("מזג אוויר ב...");
        citySelect.setWidth("120px");
        citySelect.getElement().setAttribute("theme", "small"); // הופך את התיבה לקטנה ועדינה
        
        weatherText.getStyle()
                .set("font-weight", "bold")
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)");

        WeatherService weatherService = VaadinService.getCurrent().getInstantiator()
                .getOrCreate(WeatherService.class);

        // מאזין לשינוי בחירה: קורא ל-API ומעדכן רק את הטקסט ליד התיבה
        citySelect.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                City selected = event.getValue();
                String temp = weatherService.fetchCurrentWeather(selected.latitude(), selected.longitude());
                weatherText.setText(temp);
            }
        });

        // קביעת ירושלים כברירת מחדל (אינדקס 0) - יפעיל אוטומטית את מאזין השינוי בטעינה הראשונה
        citySelect.setValue(israelCities.get(0));

        weatherContainer.add(citySelect, weatherText);
    }

    public void refreshNavbarContent() {
        navigationContainer.removeAll();
        rightSectionContainer.removeAll();
        
        rightSectionContainer.setAlignItems(FlexComponent.Alignment.CENTER);
        rightSectionContainer.setSpacing(true);

        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");

        Avatar userAvatar = new Avatar();
        Span greeting = new Span();
        greeting.getStyle().set("font-weight", "bold").set("margin-right", "15px");

        if (currentUser != null) {
            userAvatar.setName(currentUser.getUsername());
            greeting.setText("Hello, " + currentUser.getUsername());
            
            navigationContainer.add(new RouterLink("Main", MainView.class));
            navigationContainer.add(new RouterLink("My Cloud", HomeView.class));

            Button profileBtn = new Button("Profile", e -> profile());
            
            Button logoutBtn = new Button("Logout", e -> logOut());
            logoutBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);

            rightSectionContainer.add(userAvatar, greeting, profileBtn, logoutBtn);
            
        } else {
            userAvatar.setName("Guest"); 
            greeting.setText("Hello, Guest");
            
            navigationContainer.add(new RouterLink("Home", MainView.class));

            Button loginBtn = new Button("Login", e -> UI.getCurrent().navigate(LoginView.class));
            loginBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button registerBtn = new Button("Register", e -> UI.getCurrent().navigate(RegisterView.class));

            rightSectionContainer.add(userAvatar, greeting, loginBtn, registerBtn);
        }
    }

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

            if (updatedUser == null || !originalEmail.equalsIgnoreCase(updatedUser.getEmail())) {
                logOut();
            } else {
                refreshNavbarContent();

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
        UI.getCurrent().getPage().setLocation("/"); 
        SessionHelper.removeAttribute("loggedInUser");
        VaadinSession.getCurrent().close();
        SessionHelper.invalidate();
    }
}