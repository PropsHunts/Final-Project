package com.ftp_proj.project_ftp_v1.services;

import com.ftp_proj.project_ftp_v1.utils.Lz78Utils;
import org.apache.commons.net.PrintCommandListener; // לוגים
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class FtpUiService {

    private static final String SERVER = "127.0.0.1";
    private static final int PORT = 2121;
    private static final String USER = "admin";
    private static final String PASS = "12345";
    private static final String STORAGE_PATH = "storage/";

    // פונקציית עזר להגדרת לקוח עם לוגים וקידוד נכון
    private FTPClient createClient() throws IOException {
        FTPClient ftpClient = new FTPClient();
        
        // 1. הוספת מאזין שמדפיס הכל לקונסול (Log)
        ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
        
        // 2. הגדרת קידוד לעברית
        ftpClient.setControlEncoding("UTF-8");

        ftpClient.connect(SERVER, PORT);
        ftpClient.login(USER, PASS);
        ftpClient.enterLocalPassiveMode();
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        
        return ftpClient;
    }

    public boolean doesFileExist(String filename) {
        FTPClient ftpClient = new FTPClient();
        boolean exists = false;
        try {
            ftpClient = createClient(); // שימוש בפונקציית העזר
            String remoteName = filename.endsWith(".lz78") ? filename : filename + ".lz78";
            String[] names = ftpClient.listNames(remoteName);
            exists = (names != null && names.length > 0);
            ftpClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return exists;
    }

    @Async
    public CompletableFuture<Boolean> uploadFileResumable(String filename, InputStream inputStream, long totalSize) {
        return CompletableFuture.supplyAsync(() -> {
            FTPClient ftpClient = new FTPClient();
            File tempCompressedFile = null;

            try {
                // דחיסה לקובץ זמני
                tempCompressedFile = File.createTempFile("upload_", ".lz78");
                try (FileOutputStream fos = new FileOutputStream(tempCompressedFile)) {
                    Lz78Utils.compress(inputStream, fos);
                }

                String remoteName = filename.endsWith(".lz78") ? filename : filename + ".lz78";
                
                ftpClient = createClient(); // חיבור עם לוגים ו-UTF8

                // *** כאן השינוי הגדול: שימוש בזרם ידני במקום storeFile אוטומטי ***
                try (InputStream fileIn = new FileInputStream(tempCompressedFile);
                     OutputStream ftpOut = ftpClient.storeFileStream(remoteName)) { // פותח צינור לכתיבה
                    
                    if (ftpOut == null) {
                        System.err.println("Failed to open output stream from FTP server.");
                        return false;
                    }

                    // העתקה ידנית בבלוקים (Buffer)
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        ftpOut.write(buffer, 0, bytesRead);
                        ftpOut.flush(); // וידוא שליחה
                    }
                }

                // חובה לקרוא לפקודה הזו כשמסיימים לעבוד עם Stream ב-commons-net
                return ftpClient.completePendingCommand();

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                try { if (ftpClient.isConnected()) ftpClient.disconnect(); } catch (Exception e) {}
                if (tempCompressedFile != null) tempCompressedFile.delete();
            }
        });
    }

    public void downloadFileFromFtp(String remoteFilename, OutputStream browserStream) {
        FTPClient ftpClient = new FTPClient();
        try {
            ftpClient = createClient(); // חיבור עם לוגים ו-UTF8

            // גם בהורדה - שימוש בזרם ידני
            try (InputStream ftpStream = ftpClient.retrieveFileStream(remoteFilename)) {
                if (ftpStream != null) {
                    // כאן ה-Lz78Utils כבר עושה את הקריאה בבלוקים
                    Lz78Utils.decompress(ftpStream, browserStream);
                }
            }
            ftpClient.completePendingCommand();
            ftpClient.disconnect();
        } catch (IOException ex) {
            System.out.println("Download interrupted: " + ex.getMessage());
        }
    }
    
    // getUploadedFiles נשאר אותו דבר...
    public List<UploadedFileDTO> getUploadedFiles() {
        List<UploadedFileDTO> filesList = new ArrayList<>();
        File folder = new File(STORAGE_PATH);
        if (!folder.exists()) folder.mkdirs();

        File[] files = folder.listFiles();
        if (files != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            for (File file : files) {
                try {
                    BasicFileAttributes attr = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                    LocalDateTime date = LocalDateTime.ofInstant(attr.creationTime().toInstant(), ZoneId.systemDefault());
                    filesList.add(new UploadedFileDTO(file.getName(), file.length()/1024+" KB", "", date.format(formatter)));
                } catch (IOException e) {}
            }
        }
        return filesList;
    }
}