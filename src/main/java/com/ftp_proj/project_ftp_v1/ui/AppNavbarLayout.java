package com.ftp_proj.project_ftp_v1.ui;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;

import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.utils.RouteHelper;
import com.ftp_proj.project_ftp_v1.utils.SessionHelper;

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

        HorizontalLayout header = new HorizontalLayout();
        header.add(logo, title, navigationContainer);

        header.addAndExpand(new HorizontalLayout());
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

        User currentUser = (User) SessionHelper.getAttribute("loggedInUser");

        Avatar userAvatar = new Avatar();
        MenuBar menuBar = new MenuBar();
        menuBar.getStyle().set("background", "transparent");

        MenuItem userMenu = menuBar.addItem(userAvatar);

        if (currentUser != null) {
            userAvatar.setName(currentUser.getUsername());

            userMenu.getSubMenu().addItem("Profile", e -> {
                profile();
            });
            userMenu.getSubMenu().addItem("Logout", e -> logOut());

            navigationContainer.add(new RouterLink("Home", HomeView.class));
        } else {
            userAvatar.setName("Guest");

            userMenu.getSubMenu().addItem("Login", e -> UI.getCurrent().navigate(LoginView.class));

            navigationContainer.add(new RouterLink("Home", StartView.class));
        }

        rightSectionContainer.add(menuBar);
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

        // דרך נקייה ופשוטה של Vaadin לשלוף Beans מתוך Spring Context
        var context = com.vaadin.flow.server.VaadinService.getCurrent().getInstantiator()
                .getOrCreate(com.ftp_proj.project_ftp_v1.services.UserService.class);
        var ftpService = com.vaadin.flow.server.VaadinService.getCurrent().getInstantiator()
                .getOrCreate(com.ftp_proj.project_ftp_v1.services.FtpUiService.class);

        // פתיחת הדיאלוג בצורה נקייה עם ה-Callback
        ProfileDialog profileDialog = new ProfileDialog(context, ftpService, () -> {
            User updatedUser = (User) SessionHelper.getAttribute("loggedInUser");

            // אם האימייל (ה-ID שלו) השתנה, מנתקים אותו ומבקשים ממנו להתחבר מחדש
            if (updatedUser == null || !originalEmail.equalsIgnoreCase(updatedUser.getEmail())) {
                com.vaadin.flow.component.notification.Notification.show(
                        "Email changed successfully. Please log in again.",
                        4000, com.vaadin.flow.component.notification.Notification.Position.MIDDLE);
                logOut();
            } else {
                // אם רק ה-Username או הסיסמה השתנו, מרעננים את ה-Navbar ואת המסך הנוכחי
                refreshNavbarContent();

                // מרעננים את ה-Grid ב-HomeView הקיים אם הוא על המסך
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
        if (SessionHelper.getAttribute("loggedInUser") != null) {
            SessionHelper.removeAttribute("loggedInUser");
            SessionHelper.invalidate();
            refreshNavbarContent();
            RouteHelper.navigateTo(StartView.class);
        }
    }
}