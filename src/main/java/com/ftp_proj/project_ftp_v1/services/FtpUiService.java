package com.ftp_proj.project_ftp_v1.services;

import com.ftp_proj.project_ftp_v1.datamodels.FileDocument;
import com.ftp_proj.project_ftp_v1.datamodels.UploadedFileDTO;
import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.repositories.FileRepository;
import com.ftp_proj.project_ftp_v1.repositories.UserRepository;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    public void uploadAndCompressFile(User user, String filename, InputStream fileData) throws IOException {
        FTPClient ftp = createClient(user);
        // Append .lz78 extension to mark it as compressed
        String remoteName = filename.endsWith(".lz78") ? filename : filename + ".lz78";

        // Open an output stream to the FTP server
        try (OutputStream ftpOut = ftp.storeFileStream(remoteName)) {
            if (ftpOut == null)
                throw new IOException("Failed to open FTP data connection.");

            // Pipe the raw file data through the LZ78 compressor directly into the FTP
            // stream
            lz78Service.compress(fileData, ftpOut);
        }

        // Wait for the FTP server to acknowledge the transfer is complete
        if (ftp.completePendingCommand()) {
            // Retrieve the final compressed size from the FTP server to save in the
            // Database
            FTPFile[] files = ftp.listFiles(remoteName);
            long compressedSize = (files != null && files.length > 0) ? files[0].getSize() : 0;

            // Update or create the document in MongoDB
            FileDocument doc = fileRepository.findByOwnerEmailAndFileName(user.getEmail(), filename);
            if (doc == null)
                doc = new FileDocument(user.getEmail(), filename, 0);
            doc.setCompressedSize(compressedSize);
            fileRepository.save(doc);
            user.setFileId(doc.getFileId());
            user.printAllFilesID();
            userRepository.save(user);
        }
        ftp.disconnect();
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

                // 2. הזרמה טהורה (Zero RAM)
                // ה-LZ78 שואב את הביטים הדחוסים מה-FTP (in), פורס אותם,
                // וזורק את המקור ישירות לדפדפן (browserStream) בזמן אמת!
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

        for (FileDocument doc : dbFiles) {
            // Calculate size: if smaller than 1KB, show 1KB
            long sizeInKb = doc.getCompressedSize() / 1024;
            String sizeStr = (sizeInKb > 0 ? sizeInKb : 1) + " KB";

            list.add(new UploadedFileDTO(
                    doc.getFileName(),
                    sizeStr,
                    "LZ78 Compressed",
                    doc.getUploadDate().toString()));
        }
        return list;
    }

    // תוסיף את זה בתוך FtpUiService.java
    public void updateFilesOwnerEmail(String oldEmail, String newEmail) {
        // 1. שליפת כל מסמכי הקבצים השייכים לאימייל הקודם
        List<FileDocument> userFiles = fileRepository.findByOwnerEmail(oldEmail);

        if (userFiles != null && !userFiles.isEmpty()) {
            // 2. עדכון שדה ה-ownerEmail לכל קובץ
            for (FileDocument doc : userFiles) {
                doc.setOwnerEmail(newEmail);
            }
            // 3. שמירה קבוצתית מעודכנת ב-MongoDB
            fileRepository.saveAll(userFiles);
            System.out.println("[DEBUG] Updated " + userFiles.size() + " files to new owner email: " + newEmail);
        }
    }
}