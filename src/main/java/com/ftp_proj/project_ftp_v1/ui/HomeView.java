package com.ftp_proj.project_ftp_v1.ui;

import com.ftp_proj.project_ftp_v1.datamodels.UploadedFileDTO;
import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.services.*;
import com.ftp_proj.project_ftp_v1.utils.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.streams.UploadHandler; // הייבוא החדש הנדרש


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

        H1 title = new H1("My FTP Cloud Storage");

        // הגדרת עמודות הטבלה
        grid.addColumn(UploadedFileDTO::name).setHeader("File Name").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::type).setHeader("Type").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::size).setHeader("Size").setAutoWidth(true);
        grid.addColumn(UploadedFileDTO::uploadTime).setHeader("Upload Date").setAutoWidth(true);

        // עמודת פעולות
        grid.addComponentColumn(file -> {
            Button downloadBtn = new Button("Download");
            Anchor downloadAnchor = new Anchor("/download/" + file.name(), "");
            downloadAnchor.getElement().setAttribute("download", true);
            downloadAnchor.add(downloadBtn);

            Button deleteBtn = new Button("Delete", e -> {
                if (ftpService.deleteFileFromFtp(activeUser, file.name())) {
                    Notification.show("File deleted successfully.");
                    refreshGrid();
                } else {
                    Notification.show("Failed to delete file.");
                }
            });
            deleteBtn.getStyle().set("color", "red");

            return new HorizontalLayout(downloadAnchor, deleteBtn);
        }).setHeader("Actions").setAutoWidth(true);

        refreshGrid();

        Upload upload = new Upload();

        UploadHandler uploadHandler = event -> {
            String filename = event.getFileName();

            try {
                // 1. Check if file already exists to prevent overwriting
                if (ftpService.doesFileExist(activeUser, filename)) {
                    throw new RuntimeException("File already exists on the server");
                }

                // 2. Start the upload and compression process directly from Vaadin's InputStream
                ftpService.uploadAndCompressFile(activeUser, filename, event.getInputStream());

                // 3. If successful, safely update the UI thread
                currentUI.access(() -> {
                    Notification.show("File '" + filename + "' uploaded and compressed successfully!");
                    refreshGrid();          // Update the data table
                    upload.clearFileList(); // Remove the file from the upload widget visually
                });

            } catch (Exception e) {
                // If anything fails (FTP error, duplicate file, compression crash), show the error
                currentUI.access(() -> {
                    Notification.show("Upload error: " + e.getMessage());
                    upload.clearFileList();
                });
            }
        };

        // מחברים את ההנדלר החדש לרכיב ההעלאה
        upload.setUploadHandler(uploadHandler);

        add(title, upload, grid);
    }

    private void refreshGrid() {
        if (activeUser != null) {
            grid.setItems(ftpService.getUploadedFiles(activeUser));
        }
    }
}