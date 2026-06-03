package com.urielt.my_final_proj.controllers;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.services.FtpUiService;

import java.io.IOException;

@RestController
public class FileDownloadController {

    private final FtpUiService ftpService;

    public FileDownloadController(FtpUiService ftpService) {
        this.ftpService = ftpService;
    }

    @GetMapping("/download/{filename}")
    public void downloadFile(@PathVariable String filename, 
                             HttpSession session, 
                             HttpServletResponse response) {
        try {
            User user = ((User)session.getAttribute("loggedInUser"));
            
            // 1. בדיקת Authentication (האם מחובר בכלל?)
            if (user == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "You must be logged in to download files.");
                return;
            }

            // 2. בדיקת Authorization (האם הקובץ שייך לו?)
            // * הערה: עליך לממש את הפונקציה הזו ב-FtpUiService או לבדוק מול הרשימה שיש ב-User
            if (!ftpService.hasAccessToFile(user, filename)) {
                // מחזירים 403 Forbidden - גישה נדחתה!
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied! This file does not belong to you.");
                return;
            }

            // 3. הגדרת ה-Headers להורדה תקינה בדפדפן
            response.setContentType("application/octet-stream");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");

            // 4. הזרמת הקובץ ישירות מה-FTP לדפדפן (Zero RAM!)
            ftpService.downloadFileFromFtp(user, filename, response.getOutputStream());

        } catch (IOException e) {
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "שגיאה בהורדת הקובץ");
            } catch (IOException ignored) {}
        }
    }
}