package com.ftp_proj.project_ftp_v1.controllers;
import com.ftp_proj.project_ftp_v1.services.FtpUiService;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class FileDownloadController {
    private final FtpUiService service;
    public FileDownloadController(FtpUiService service) { this.service = service; }

    @GetMapping("/download/{email}/{filename}")
    public void downloadFile(@PathVariable String email, @PathVariable String filename, HttpServletResponse response) {
        String cleanName = filename.replace(".lz78", "");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + cleanName + "\"");
        try {
            service.downloadFileFromFtp(email, filename, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {}
    }
}