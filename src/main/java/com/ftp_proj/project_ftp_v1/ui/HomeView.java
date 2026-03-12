package com.ftp_proj.project_ftp_v1.ui;
import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.services.*;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;

@Route("home")
public class HomeView extends VerticalLayout implements BeforeEnterObserver {
    private final FtpUiService ftpService;
    private final Grid<UploadedFileDTO> grid = new Grid<>(UploadedFileDTO.class);
    private User user;

    public HomeView(FtpUiService ftpService) {
        this.ftpService = ftpService;
        setSizeFull(); setAlignItems(Alignment.CENTER);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        user = (User) VaadinSession.getCurrent().getAttribute("loggedInUser");
        if (user == null) event.rerouteTo(LoginView.class); // אבטחה
        else buildUI();
    }

    private void buildUI() {
        removeAll();
        Button logout = new Button("התנתק", e -> { VaadinSession.getCurrent().setAttribute("loggedInUser", null); UI.getCurrent().navigate(MainView.class); });
        
        MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
        Upload upload = new Upload(buffer);
        
        upload.addSucceededListener(event -> {
            String name = event.getFileName();
            if (!ftpService.doesFileExist(user.getEmail(), name)) {
                upload.clearFileList(); Notification.show("מעלה קובץ ברקע...");
                ftpService.uploadFileResumable(user.getEmail(), name, buffer.getInputStream(name)).thenAccept(res -> {
                    UI.getCurrent().access(() -> { grid.setItems(ftpService.getUploadedFiles(user.getEmail())); });
                });
            } else { Notification.show("הקובץ קיים!"); upload.clearFileList(); }
        });

        grid.removeAllColumns();
        grid.addColumn(UploadedFileDTO::name).setHeader("שם");
        grid.addColumn(UploadedFileDTO::size).setHeader("גודל");
        grid.addComponentColumn(file -> {
            Anchor a = new Anchor("/download/" + user.getEmail() + "/" + file.name(), new Button("הורד"));
            a.getElement().setAttribute("download", true); return a;
        }).setHeader("פעולות");
        
        grid.setItems(ftpService.getUploadedFiles(user.getEmail()));
        add(new H2("הקבצים של " + user.getUsername()), logout, upload, grid);
    }
}