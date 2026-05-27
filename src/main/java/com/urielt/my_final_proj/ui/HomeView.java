package com.urielt.my_final_proj.ui;

import com.urielt.my_final_proj.datamodels.UploadedFileDTO;
import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.*;
import com.urielt.my_final_proj.utils.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.streams.UploadHandler;

@Route(value = "home", layout = AppNavbarLayout.class)
public class HomeView extends VerticalLayout implements BeforeEnterObserver {
    
    private final FtpUiService ftpService;
    private UI currentUI;
    private final Grid<UploadedFileDTO> grid = new Grid<>(UploadedFileDTO.class, false);
    private User activeUser;

    public HomeView(FtpUiService ftpService) {
        this.ftpService = ftpService;
        setSizeFull(); 
        setAlignItems(Alignment.CENTER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        activeUser = (User) SessionHelper.getAttribute("loggedInUser");
        if (activeUser == null) {
            event.rerouteTo(LoginView.class);
        } else {
            currentUI = UI.getCurrent(); 
            AppNavbarLayout.refreshCurrentNavbar();
            buildUI();
        }
    }

    private void buildUI() {
        removeAll();
        grid.removeAllColumns();

        H1 title = new H1("My FTP Storage");

        // עמודות הטבלה
        grid.addColumn(UploadedFileDTO::name).setHeader("File Name").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::type).setHeader("Type").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::size).setHeader("Size").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::uploadTime).setHeader("Upload Date").setAutoWidth(true);

        // כפתורי הורדה ומחיקה
        grid.addComponentColumn(file -> {
            Button downloadBtn = new Button("Download");
            
            String encodedFilename = java.net.URLEncoder.encode(file.name(), java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
            Anchor downloadAnchor = new Anchor("/download/" + encodedFilename, "");
            
            downloadAnchor.getElement().setAttribute("router-ignore", "true");
            downloadAnchor.setTarget("_blank");
            downloadAnchor.getElement().setAttribute("download", true);
            downloadAnchor.add(downloadBtn);

            Button deleteBtn = new Button("Delete", e -> {
                if (ftpService.deleteFileFromFtp(activeUser, file.name())) {
                    Notification.show("File deleted.");
                    refreshGrid();
                } else {
                    Notification.show("Delete failed.");
                }
            });
            deleteBtn.getStyle().set("color", "red");

            return new HorizontalLayout(downloadAnchor, deleteBtn);
        }).setHeader("Actions").setAutoWidth(true);

        refreshGrid();

       // --- אזור ההעלאה ---
        Upload upload = new Upload();
        upload.setDropLabel(new Span("Drag files here"));

upload.getStyle().set("padding", "20px");

upload.getElement().executeJs("""
    this.shadowRoot.querySelector('[part="file-list"]')
        ?.style.setProperty('display', 'none');
""");
        // דרישה 1: הגבלת גודל קובץ ל-7GB (7 * 1024 * 1024 * 1024 בתים)
        long maxFileSizeInBytes = 7L * 1024 * 1024 * 1024;
        upload.setMaxFileSize((int) Math.min(maxFileSizeInBytes, Integer.MAX_VALUE)); // Vaadin מקבל int, נגביל למקסימום האפשרי שלו
        // הערה: ב-Vaadin 24 עדיף להשאיר את ההגבלה העיקרית ב-application.properties, אבל הוספנו פה ליתר ביטחון
        upload.setDropAllowed(true);        // יצירת אזור (קונטיינר) שיכיל את כל פסי הטעינה של הקבצים שעולים במקביל
        upload.addFinishedListener(event -> {
    upload.getElement().executeJs("this.files = this.files.filter(f => f.name !== $0);", event.getFileName());
});

// 4. מאזין לקבצים שנחסמו (כי עברו את ה-7GB) כדי להציג הודעה למשתמש
upload.addFileRejectedListener(event -> {
    Notification.show("השרת אינו מקבל קבצים מעל 7GB!", 5000, Notification.Position.MIDDLE);
});
        VerticalLayout uploadsContainer = new VerticalLayout();
        uploadsContainer.setWidth("400px");
        uploadsContainer.setMaxWidth("90%");

        // ניהול תהליך ההעלאה
        UploadHandler uploadHandler = event -> {
            String filename = event.getFileName();
            long totalBytes = event.getFileSize();

            try {
                if (ftpService.doesFileExist(activeUser, filename)) {
                    throw new RuntimeException("File already exists on server");
                }

                // דרישה 2: יצירת פס טעינה *ייעודי* לקובץ הספציפי הזה
                ProgressBar progressBar = new ProgressBar();
                progressBar.setWidthFull();
                Span statusText = new Span("Uploading " + filename + ": ");
                Span percentText = new Span("0%");
                percentText.getStyle().set("font-weight", "bold").set("min-width", "45px");

                HorizontalLayout progressLayout = new HorizontalLayout(statusText, progressBar, percentText);
                progressLayout.setWidthFull();
                progressLayout.setAlignItems(Alignment.CENTER);

                // הוספת פס הטעינה למסך מיד עם תחילת ההעלאה
                currentUI.access(() -> uploadsContainer.add(progressLayout));

                ftpService.uploadAndCompressFile(activeUser, filename, event.getInputStream(), totalBytes, new FtpUiService.FileUploadCallback() {
                    
                    @Override
                    public void onProgress(int percent) {
                        currentUI.access(() -> {
                            progressBar.setValue(percent / 100.0);
                            percentText.setText(percent + "%");
                        });
                    }

                    @Override
                    public void onComplete(boolean success) {
                        currentUI.access(() -> {
                            // מחיקת פס הטעינה *הספציפי* הזה בלבד! לא פוגע בקבצים אחרים שעולים
                            uploadsContainer.remove(progressLayout);
                            
                            // תיקון קריטי: הסרנו את upload.clearFileList() כדי לא לנתק קבצים אחרים
                            
                            if (success) {
                                Notification.show("'" + filename + "' uploaded successfully!");
                                
                                refreshGrid();
                            } else {
                                Notification.show("Upload failed for '" + filename + "'.");
                            }
                        });
                    }
                });

            } catch (Exception e) {
                currentUI.access(() -> {
                    Notification.show("Error: " + e.getMessage());
                });
            }
        };

        upload.setUploadHandler(uploadHandler);

        // --- הנדסת ה-UI החדש (ימין ושמאל) ---
        HorizontalLayout topSection = new HorizontalLayout();
        topSection.setWidthFull();
        topSection.setJustifyContentMode(JustifyContentMode.BETWEEN); // דוחף אותם לקצוות
        topSection.setAlignItems(Alignment.START); // מיישר אותם למעלה
        
        // מוסיפים משמאל לימין: קודם פסי הטעינה, אחר כך כפתור ההעלאה
        topSection.add(uploadsContainer, upload);

        // הוספה למסך הראשי: כותרת, האזור העליון (שמכיל ימין ושמאל), והטבלה למטה
        add(title, topSection, grid);
    }

    private void refreshGrid() {
        if (activeUser != null) {
            grid.setItems(ftpService.getUploadedFiles(activeUser));
        }
    }
}