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

import java.util.concurrent.atomic.AtomicInteger; // <-- הוספנו את המונה

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

        grid.addColumn(UploadedFileDTO::name).setHeader("File Name").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::type).setHeader("Type").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::size).setHeader("Size").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::uploadTime).setHeader("Upload Date").setAutoWidth(true);

        grid.addComponentColumn(file -> {
            Button downloadBtn = new Button("Download");

            String encodedFilename = java.net.URLEncoder.encode(file.name(), java.nio.charset.StandardCharsets.UTF_8)
                    .replace("+", "%20");
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

        long maxFileSizeInBytes = 7L * 1024 * 1024 * 1024;
        upload.setMaxFileSize((int) Math.min(maxFileSizeInBytes, Integer.MAX_VALUE));
        upload.setDropAllowed(true);

        upload.addFileRejectedListener(event -> {
            Notification.show("השרת אינו מקבל קבצים מעל 7GB!", 5000, Notification.Position.MIDDLE);
        });

        VerticalLayout uploadsContainer = new VerticalLayout();
        uploadsContainer.setWidth("400px");
        uploadsContainer.setMaxWidth("90%");

        // *** התיקון ההנדסי - מונה העלאות חי ***
        AtomicInteger activeUploads = new AtomicInteger(0);

        UploadHandler uploadHandler = event -> {
            String filename = event.getFileName();
            long totalBytes = event.getFileSize();

            // קובץ חדש מתחיל לעלות - מוסיפים 1 למונה!
            activeUploads.incrementAndGet();

            try {
                if (ftpService.doesFileExist(activeUser, filename)) {
                    throw new RuntimeException("File already exists on server");
                }

                ProgressBar progressBar = new ProgressBar();
                progressBar.setWidthFull();
                Span statusText = new Span("Uploading " + filename + ": ");
                Span percentText = new Span("0%");
                percentText.getStyle().set("font-weight", "bold").set("min-width", "45px");

                HorizontalLayout progressLayout = new HorizontalLayout(statusText, progressBar, percentText);
                progressLayout.setWidthFull();
                progressLayout.setAlignItems(Alignment.CENTER);

                currentUI.access(() -> uploadsContainer.add(progressLayout));

                ftpService.uploadAndCompressFile(activeUser, filename, event.getInputStream(), totalBytes,
                        new FtpUiService.FileUploadCallback() {

                            @Override
                            public void onProgress(int percent) {
                                // התיקון: בודקים אם המסך עדיין קיים (המשתמש לא עשה ריפרש)
                                if (currentUI.isAttached()) {
                                    currentUI.access(() -> {
                                        progressBar.setValue(percent / 100.0);
                                        percentText.setText(percent + "%");
                                    });
                                }
                            }

                            @Override
                            public void onComplete(boolean success) {
                                // התיקון: בודקים אם המסך עדיין קיים לפני שמעדכנים אותו
                                if (currentUI.isAttached()) {
                                    currentUI.access(() -> {
                                        uploadsContainer.remove(progressLayout);

                                        // מורידים 1 מהמונה. אם הגענו ל-0, ננקה את המסך
                                        if (activeUploads.decrementAndGet() == 0) {
                                            upload.clearFileList();
                                        }

                                        if (success) {
                                            Notification.show("'" + filename + "' uploaded successfully!");
                                            refreshGrid();
                                        } else {
                                            Notification.show("Upload failed for '" + filename + "'.");
                                        }
                                    });
                                } else {
                                    // אם המסך כבר מת (נעשה ריפרש), עדיין צריך להוריד את המונה מאחורי הקלעים!
                                    activeUploads.decrementAndGet();
                                }
                            }
                        });

            } catch (Exception e) {
                currentUI.access(() -> {
                    Notification.show("Error: " + e.getMessage());

                    // במידה והייתה שגיאה (למשל קובץ שכבר קיים), גם מורידים מהמונה ובודקים האם צריך
                    // לנקות
                    if (activeUploads.decrementAndGet() == 0) {
                        upload.clearFileList();
                    }
                });
            }
        };

        upload.setUploadHandler(uploadHandler);

        HorizontalLayout topSection = new HorizontalLayout();
        topSection.setWidthFull();
        topSection.setJustifyContentMode(JustifyContentMode.BETWEEN);
        topSection.setAlignItems(Alignment.START);

        topSection.add(uploadsContainer, upload);

        add(title, topSection, grid);
    }

    private void refreshGrid() {
        if (activeUser != null) {
            grid.setItems(ftpService.getUploadedFiles(activeUser));
        }
    }
}