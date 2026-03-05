package com.ftp_proj.project_ftp_v1.ui;

import com.ftp_proj.project_ftp_v1.services.FtpUiService;
import com.ftp_proj.project_ftp_v1.services.UploadedFileDTO;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.Route;
import java.io.InputStream;

@Route("")
public class MainView extends VerticalLayout {
    private final FtpUiService ftpUiService;
    private final Grid<UploadedFileDTO> grid = new Grid<>(UploadedFileDTO.class);
    private final MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();

    public MainView(FtpUiService ftpUiService) {
        this.ftpUiService = ftpUiService;
        setSizeFull();
        setAlignItems(Alignment.CENTER);

        H1 title = new H1("LZ78 FTP Server");
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes("image/*", ".txt", ".pdf", ".zip", ".mp4");
        // upload.setMaxFileSize(10L * 1024 * 1024 * 1024); // 10GB

        upload.addSucceededListener(event -> {
            String fileName = event.getFileName();
            InputStream inputStream = buffer.getInputStream(fileName);
            long size = event.getContentLength();
            if (ftpUiService.doesFileExist(fileName)) {
                openOverwriteDialog(fileName, inputStream, size, upload);
            } else {
                startBackgroundUpload(fileName, inputStream, size, upload);
            }
        });

        Button refreshBtn = new Button("Refresh", VaadinIcon.REFRESH.create(), e -> refreshGrid());
        configureGrid();
        refreshGrid();
        add(title, upload, refreshBtn, grid);
    }

    private void startBackgroundUpload(String fileName, InputStream inputStream, long size, Upload upload) {
        UI ui = UI.getCurrent();
        showNotification("Started uploading: " + fileName, false);
        upload.clearFileList();
        
        ftpUiService.uploadFileResumable(fileName, inputStream, size)
                .thenAccept(success -> {
                    ui.access(() -> {
                        if (success) {
                            showNotification("Finished: " + fileName, false);
                            refreshGrid();
                        } else {
                            showNotification("Failed: " + fileName, true);
                        }
                    });
                });
    }

    private void openOverwriteDialog(String fileName, InputStream inputStream, long size, Upload upload) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("File Exists");
        dialog.add(new Span("Overwrite " + fileName + "?"));
        Button confirm = new Button("Yes", e -> {
            startBackgroundUpload(fileName, inputStream, size, upload);
            dialog.close();
        });
        confirm.addThemeVariants(ButtonVariant.LUMO_ERROR);
        Button cancel = new Button("No", e -> {
            upload.clearFileList();
            dialog.close();
        });
        dialog.getFooter().add(cancel, confirm);
        dialog.open();
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.addColumn(UploadedFileDTO::name).setHeader("Name");
        grid.addColumn(UploadedFileDTO::size).setHeader("Size");
        grid.addColumn(UploadedFileDTO::uploadTime).setHeader("Date");
        grid.addComponentColumn(file -> {
            Button btn = new Button(VaadinIcon.DOWNLOAD.create());
            btn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            Anchor anchor = new Anchor("/download/" + file.name(), btn);
            anchor.getElement().setAttribute("download", true);
            return anchor;
        }).setHeader("Download");
    }

    private void refreshGrid() { grid.setItems(ftpUiService.getUploadedFiles()); }
    
    private void showNotification(String text, boolean isError) {
        Notification n = Notification.show(text);
        n.addThemeVariants(isError ? NotificationVariant.LUMO_ERROR : NotificationVariant.LUMO_SUCCESS);
        n.setDuration(4000);
    }
}
// import com.vaadin.flow.component.button.Button;
// import com.vaadin.flow.component.html.Div;
// import com.vaadin.flow.component.html.H1;
// import com.vaadin.flow.component.html.H4;
// import com.vaadin.flow.component.html.Paragraph;
// import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
// import com.vaadin.flow.component.orderedlayout.VerticalLayout;
// import com.vaadin.flow.router.Route;

// @Route("/")
// public class HomeView extends VerticalLayout {

//     public HomeView() {

//         // מסך מלא וממורכז
//         setSizeFull();
//         setAlignItems(Alignment.CENTER);
//         setJustifyContentMode(JustifyContentMode.CENTER);

//         // כרטיס מרכזי
//         Div card = new Div();
//         card.getStyle()
//                 .set("width", "600px")
//                 .set("padding", "40px")
//                 .set("border-radius", "12px")
//                 .set("box-shadow", "0 10px 25px rgba(0,0,0,0.1)")
//                 .set("background-color", "white")
//                 .set("text-align", "center");

//         // כותרת ראשית
//         H1 title = new H1("שרת הענן שלנו");
//         title.getStyle()
//                 .set("margin-bottom", "10px");

//         // כותרת משנה
//         H4 subtitle = new H4("אחסון, ניהול ושיתוף קבצים בצורה מאובטחת");
//         subtitle.getStyle()
//                 .set("color", "gray")
//                 .set("margin-bottom", "20px");

//         // תיאור
//         Paragraph description = new Paragraph(
//                 "המערכת מאפשרת למשתמשים להעלות קבצים לשרת ענן אישי, "
//               + "לנהל אותם, להוריד אותם בכל זמן ולגשת אליהם מכל מקום דרך הדפדפן."
//         );
//         description.getStyle()
//                 .set("font-size", "16px")
//                 .set("line-height", "1.6");

//         // יכולות
//         Paragraph features = new Paragraph(
//                 "✔ העלאת קבצים\n"
//               + "✔ הורדת קבצים\n"
//               + "✔ ניהול קבצים אישי\n"
//         );
//         features.getStyle()
//                 .set("white-space", "pre-line")
//                 .set("margin-top", "20px")
//                 .set("margin-bottom", "30px");

//         // כפתורים
//         Button loginButton = new Button("כניסה");
//         loginButton.getStyle()
//                 .set("background-color", "#2563eb")
//                 .set("color", "white")
//                 .set("border-radius", "8px")
//                 .set("padding", "10px 25px");

//         Button registerButton = new Button("הצטרפות");
//         registerButton.getStyle()
//                 .set("background-color", "#16a34a")
//                 .set("color", "white")
//                 .set("border-radius", "8px")
//                 .set("padding", "10px 25px");

//         // בעתיד: ניווט למסכים
//         loginButton.addClickListener(e -> loginButton.getUI().ifPresent(ui -> ui.navigate(LoginView.class)));
//         registerButton.addClickListener(e -> registerButton.getUI().ifPresent(ui -> ui.navigate(RegisterView.class)));


//         HorizontalLayout buttons = new HorizontalLayout(loginButton, registerButton);
//         buttons.setJustifyContentMode(JustifyContentMode.CENTER);
//         buttons.setSpacing(true);

//         // הרכבת הכרטיס
//         card.add(title, subtitle, description, features, buttons);

//         // הוספה למסך
//         add(card);

//         // רקע כללי
//         getStyle()
//                 .set("background", "linear-gradient(135deg, #e0e7ff, #f8fafc)");
//     }
// }
