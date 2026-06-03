package com.urielt.my_final_proj.services;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.springframework.stereotype.Service;

import com.urielt.my_final_proj.datamodels.FileDocument;
import com.urielt.my_final_proj.datamodels.UploadedFileDTO;
import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.repositories.FileRepository;
import com.urielt.my_final_proj.repositories.UserRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service handling UI-to-FTP interactions, bridging the Web layer with the FTP
 * server.
 */
@Service
public class FtpUiService {

    private final UserRepository userRepository;

    public interface FileUploadCallback {
        void onProgress(int percent);

        void onComplete(boolean success);
    }

    private static final String SERVER = "127.0.0.1";
    private static final int PORT = 2121;

    private final FileRepository fileRepository;
    private final LZ78Service lz78Service;

    // Dependency Injection of our new services
    public FtpUiService(FileRepository fileRepository, LZ78Service lz78Service, UserRepository userRepository) {
        this.fileRepository = fileRepository;
        this.lz78Service = lz78Service;
        this.userRepository = userRepository;
    }

    /**
     * Creates an authenticated FTP connection using the dynamic user credentials.
     */
    private FTPClient createClient(User user) throws IOException {
        FTPClient ftp = new FTPClient();
        FTPClientConfig config = new FTPClientConfig(FTPClientConfig.SYST_UNIX);
        ftp.configure(config);
        ftp.setControlEncoding("UTF-8");

        ftp.connect(SERVER, PORT);
        // Login using the actual user's email and password!
        if (!ftp.login(user.getEmail(), user.getPassword())) {
            throw new IOException("FTP Authentication failed for user: " + user.getEmail());
        }

        ftp.enterLocalPassiveMode();
        ftp.setFileType(FTP.BINARY_FILE_TYPE);
        return ftp;
    }

    public boolean doesFileExist(User user, String filename) {
        boolean exists = false;
        try {
            FTPClient ftp = createClient(user);
            String name = filename.endsWith(".lz78") ? filename : filename + ".lz78";
            String[] names = ftp.listNames(user.getEmail() + "/" + name);
            exists = (names != null && names.length > 0);
            ftp.disconnect();
        } catch (Exception e) {
            // Silently ignore connection errors for existence check
        }
        return exists;
    }

    /**
     * Uploads a file, compresses it on-the-fly, and saves it to the FTP server.
     * By passing the InputStream directly to the compressor and then to the FTP,
     * we avoid loading the entire file into RAM, saving memory.
     */
    public void uploadAndCompressFile(User user, String filename, InputStream fileData, long totalBytes,
            FileUploadCallback callback) throws IOException {
        FTPClient ftp = null;
        try {
            ftp = createClient(user);
            String remoteName = filename.endsWith(".lz78") ? filename : filename + ".lz78";

            InputStream progressStream = new InputStream() {
                private long totalRead = 0;
                private int lastPercent = 0;

                @Override
                public int read() throws IOException {
                    int b = fileData.read();
                    if (b != -1)
                        updateProgress(1);
                    return b;
                }

                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int read = fileData.read(b, off, len);
                    if (read != -1)
                        updateProgress(read);
                    return read;
                }

                private void updateProgress(int bytesRead) {
                    totalRead += bytesRead;
                    int currentPercent = (int) ((totalRead * 100) / totalBytes);
                    if (currentPercent > lastPercent) {
                        callback.onProgress(currentPercent);
                        lastPercent = currentPercent;
                    }
                }
            };

            try (OutputStream ftpOut = ftp.storeFileStream(remoteName)) {
                if (ftpOut == null)
                    throw new IOException("Failed to open FTP data connection.");
                lz78Service.compress(progressStream, ftpOut);
            }

            if (ftp.completePendingCommand()) {
                // במקום לסמוך על פקודת LIST של ה-FTP, נקרא את הגודל ישירות מהכונן הקשיח
                Path physicalPath = Paths.get("storage", user.getEmail(), remoteName);
                long compressedSize = 0;

                if (Files.exists(physicalPath)) {
                    compressedSize = Files.size(physicalPath); // מביא את הגודל המדויק על הבייט!
                }

                FileDocument doc = fileRepository.findByOwnerEmailAndFileName(user.getEmail(), filename);
                if (doc == null)
                    doc = new FileDocument(user.getEmail(), filename, totalBytes);

                // מעדכנים ושומרים את הגודל האמיתי (הדחוס או התפוח)
                doc.setCompressedSize(compressedSize);

                fileRepository.save(doc);
                user.setFileId(doc.getFileId());
                userRepository.save(user);

                callback.onComplete(true);
            } else {
                callback.onComplete(false);
            }

        } catch (Exception e) {
            callback.onComplete(false);
            throw new IOException("Upload failed: " + e.getMessage(), e);
        } finally {
            if (ftp != null && ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (IOException ignored) {
                }
            }
        }
    }

    public void downloadFileFromFtp(User user, String filename, OutputStream browserStream) {
        FTPClient ftp = null;
        try {
            ftp = createClient(user);
            String path = user.getEmail() + "/" + filename + ".lz78";

            // 1. פתיחת צינור מול שרת ה-FTP
            try (InputStream in = ftp.retrieveFileStream(path)) {
                if (in == null) {
                    System.err.println("[ERROR] File not found on FTP: " + path);
                    return;
                }

                lz78Service.decompress(in, browserStream);
            }

            // 3. אישור על סיום ההורדה התקינה מול פרוטוקול ה-FTP
            ftp.completePendingCommand();

        } catch (Exception e) {
            System.err.println("[ERROR] Failed to download and decompress file: " + filename);
            e.printStackTrace();
        } finally {
            // ניתוק בטוח וסגירת ערוץ השליטה
            if (ftp != null && ftp.isConnected()) {
                try {
                    ftp.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Aggressively deletes a file. First clears it from MongoDB to prevent "Zombie
     * files" in the UI,
     * then attempts to delete the physical file from the FTP server.
     */
    public boolean deleteFileFromFtp(User user, String filename) {
        boolean success = false;
        try {
            // 1. ניקוי unconditionally מ-MongoDB (עובד ומנקה את ה-UI)
            FileDocument doc = fileRepository.findByOwnerEmailAndFileName(user.getEmail(), filename);
            if (doc != null) {
                user.removeFileID(doc.getFileId());
                user.printAllFilesID();
                fileRepository.delete(doc);
                userRepository.save(user);
                success = true;
            }

            // 2. התחברות ל-FTP למחיקה פיזית
            FTPClient ftp = createClient(user);

            // סיומת הקובץ כפי שהיא שמורה בשרת
            String cleanFileName = filename.endsWith(".lz78") ? filename : filename + ".lz78";

            // תיקון נתיב ה-FTP: הלקוח כבר נמצא בתוך תיקיית האימייל שלו! שולחים רק את שם
            // הקובץ.
            boolean ftpDeleted = ftp.deleteFile(cleanFileName);

            System.out.println("[DEBUG] FTP delete operation returned: " + ftpDeleted);

            // תיקון נתיב הדיסק של Java: ג'אווה צריכה לדעת את הנתיב המלא משורש הפרויקט
            Path physicalPath = Paths.get("storage", user.getEmail(), cleanFileName);

            // הדפסה מחוץ ל-if כדי שתראה בטוח בטרמינל מה קורה
            System.out.println("======= ניסיון מחיקת קובץ פיזי =======");
            System.out.println("נתיב פיזי מבוקש: " + physicalPath.toAbsolutePath());
            System.out.println("האם קיים על הדיסק לפני מחיקה? " + Files.exists(physicalPath));
            System.out.println("====================================");

            // מחיקה סופית מהדיסק של השרת
            // אנחנו משתמשים ב-deleteIfExists כדי למנוע קריסה אם ה-FTP כבר הספיק למחוק בעצמו
            boolean diskDeleted = Files.deleteIfExists(physicalPath);
            System.out.println("[DEBUG] Disk physical delete status: " + diskDeleted);

            if (ftpDeleted || diskDeleted) {
                success = true;
            }

            ftp.disconnect();
        } catch (Exception e) {
            System.err.println("[ERROR] Failed to delete file physically: " + e.getMessage());
            e.printStackTrace();
        }

        return success;
    }

    /**
     * Lists files by reading metadata from MongoDB (faster and includes sizes).
     */
    public List<UploadedFileDTO> getUploadedFiles(User user) {
        List<UploadedFileDTO> list = new ArrayList<>();
        List<FileDocument> dbFiles = fileRepository.findByOwnerEmail(user.getEmail());

        // מגדירים את הפורמט הרצוי: תאריך, רווח, ואז שעה מדוייקת ללא מילי-שניות
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (FileDocument doc : dbFiles) {
            long compKb = doc.getCompressedSize() / 1024;
            long origKb = doc.getOriginalSize() / 1024;

            String sizeStr = (compKb > 0 ? compKb : 1) + "KB (Orig: " + (origKb > 0 ? origKb : 1) + "KB)";

            String fileName = doc.getFileName();
            String fileType = "FILE";
            int lastDotIndex = fileName.lastIndexOf('.');
            if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
                fileType = fileName.substring(lastDotIndex + 1).toUpperCase();
            }

            // שימוש בפורמט שיצרנו במקום ב-toString() הרגיל
            String formattedDate = doc.getUploadDate().format(formatter);

            list.add(new UploadedFileDTO(
                    doc.getFileName(),
                    sizeStr,
                    fileType,
                    formattedDate)); // מכניסים את התאריך המעוצב לכאן
        }
        return list;
    }

    // תוסיף את זה בתוך FtpUiService.java
    public void updateFilesOwnerEmail(String oldEmail, String newEmail) {
        // 1. עדכון מסמכי הקבצים ב-MongoDB
        List<FileDocument> userFiles = fileRepository.findByOwnerEmail(oldEmail);
        if (userFiles != null && !userFiles.isEmpty()) {
            for (FileDocument doc : userFiles) {
                doc.setOwnerEmail(newEmail);
            }
            fileRepository.saveAll(userFiles);
            System.out.println("[DEBUG] Updated " + userFiles.size() + " files in DB to new owner email: " + newEmail);
        }

        // 2. שינוי שם התיקייה הפיזית על הדיסק (מערכת ההפעלה)
        try {
            Path oldFolderPath = Paths.get("storage", oldEmail);
            Path newFolderPath = Paths.get("storage", newEmail);

            // אם התיקייה הישנה קיימת, אנחנו פשוט משנים לה את השם (Move) לתיקייה החדשה
            if (Files.exists(oldFolderPath)) {
                Files.move(oldFolderPath, newFolderPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[DEBUG] Physical folder renamed from " + oldEmail + " to " + newEmail);
            }
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to rename physical folder from " + oldEmail + " to " + newEmail);
            e.printStackTrace();
        }
    }

    public boolean hasAccessToFile(User user, String filename) {
        // 1. הגנת Null בסיסית
        if (user == null || user.getEmail() == null || filename == null) {
            return false;
        }

        // 2. חיפוש מדויק במסד הנתונים לפי השילוב של אימייל המשתמש ושם הקובץ
        FileDocument fileDoc = fileRepository.findByOwnerEmailAndFileName(user.getEmail(), filename);

        // 3. אם מונגו מצא מסמך (הוא לא null) - הקובץ באמת שייך לו!
        return fileDoc != null;
    }
}