package com.ftp_proj.project_ftp_v1.controllers;

import com.ftp_proj.project_ftp_v1.services.FtpUiService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@RestController
public class FileDownloadController {

    private final FtpUiService ftpUiService;

    public FileDownloadController(FtpUiService ftpUiService) {
        this.ftpUiService = ftpUiService;
    }

    @GetMapping("/download/{filename}")
    public void downloadFile(@PathVariable String filename, HttpServletResponse response) {
        String downloadName = filename.replace(".lz78", "");
        
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + downloadName + "\"");

        try {
            ftpUiService.downloadFileFromFtp(filename, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException | RuntimeException e) {
            // תופסים שגיאות כמו "ClientAbortException" (כשהמשתמש מבטל הורדה)
            // ומדפיסים הודעה נקייה במקום שגיאה מפחידה
            System.out.println("Download cancelled or interrupted for file: " + filename);
        }
    }
}