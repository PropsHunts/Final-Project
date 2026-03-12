package com.ftp_proj.project_ftp_v1.services;

import com.ftp_proj.project_ftp_v1.utils.Lz78Utils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class FtpUiService {
    private static final String SERVER = "127.0.0.1";
    private static final int PORT = 2121;
    private static final String USER = "admin";
    private static final String PASS = "12345";
    private static final String STORAGE_PATH = "storage/";

    private FTPClient createClient() throws IOException {
        FTPClient ftp = new FTPClient();
        ftp.setControlEncoding("UTF-8");
        ftp.connect(SERVER, PORT);
        ftp.login(USER, PASS);
        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        return ftp;
    }

    public boolean doesFileExist(String email, String filename) {
        boolean exists = false;
        try {
            FTPClient ftp = createClient();
            String name = filename.endsWith(".lz78") ? filename : filename + ".lz78";
            String[] names = ftp.listNames(email + "/" + name);
            exists = (names != null && names.length > 0);
            ftp.disconnect();
        } catch (Exception e) {
        }
        return exists;
    }

    @Async
    public CompletableFuture<Boolean> uploadFileResumable(String email, String filename, InputStream inputStream) {
        return CompletableFuture.supplyAsync(() -> {
            File temp = null;
            FTPClient ftp = null;
            try {
                temp = File.createTempFile("up_", ".lz78");
                try (FileOutputStream fos = new FileOutputStream(temp)) {
                    Lz78Utils.compress(inputStream, fos);
                }

                ftp = createClient();
                String remoteName = email + "/" + filename;
                try (InputStream in = new FileInputStream(temp); OutputStream out = ftp.storeFileStream(remoteName)) {
                    if (out != null) {
                        byte[] buffer = new byte[8192];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                            out.write(buffer, 0, read);
                            out.flush();
                        }
                    }
                }
                return ftp.completePendingCommand();
            } catch (Exception e) {
                return false;
            } finally {
                try {
                    if (ftp != null)
                        ftp.disconnect();
                } catch (Exception e) {
                }
                if (temp != null)
                    temp.delete();
            }
        });
    }

    public void downloadFileFromFtp(String email, String filename, OutputStream browserStream) {
        try {
            FTPClient ftp = createClient();
            try (InputStream in = ftp.retrieveFileStream(email + "/" + filename)) {
                if (in != null)
                    Lz78Utils.decompress(in, browserStream);
            }
            ftp.completePendingCommand();
            ftp.disconnect();
        } catch (Exception e) {
        }
    }

    public List<UploadedFileDTO> getUploadedFiles(String email) {
        List<UploadedFileDTO> list = new ArrayList<>();

        // התיקון: פסיק במקום פלוס. Java כבר תדאג לשים סלאש באמצע בצורה תקנית
        File folder = new File(STORAGE_PATH, email);

        if (!folder.exists())
            folder.mkdirs();
        File[] files = folder.listFiles();
        if (files != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            for (File f : files) {
                try {
                    BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                    LocalDateTime date = LocalDateTime.ofInstant(attr.creationTime().toInstant(),
                            ZoneId.systemDefault());
                    list.add(new UploadedFileDTO(f.getName(), f.length() / 1024 + " KB", "", date.format(fmt)));
                } catch (Exception e) {
                }
            }
        }
        return list;
    }
}