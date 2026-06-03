package com.urielt.my_final_proj.services;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

/**
 * מחלקה המטפלת בלקוח FTP בודד.
 * המחלקה מיישמת את ממשק Runnable כדי לרוץ כתהליכון (Thread) נפרד לכל משתמש שמתחבר.
 * תפקידה הוא לפענח את פקודות ה-FTP שמגיעות מהלקוח (כמו STOR, RETR, PASV) 
 * ולבצע את הפעולות הפיזיות על הדיסק של השרת.
 */
public class FtpClientHandler implements Runnable {
    private final Socket controlSocket;
    private final String rootDir;
    private ServerSocket passiveServer;
    private PrintWriter writer;

    // שומר את התיקייה הספציפית של המשתמש המחובר כדי לבודד את הקבצים שלו
    private String currentUserDir = "";

    public FtpClientHandler(Socket socket, String rootDir) {
        this.controlSocket = socket;
        this.rootDir = rootDir.endsWith(File.separator) ? rootDir : rootDir + File.separator;
    }

    /**
     * הפונקציה המרכזית שרצה ברקע מרגע התחברות הלקוח.
     * מאזינה לערוץ השליטה (Control Socket), קוראת פקודות FTP טקסטואליות (כמו USER, PASS, STOR),
     * ומנתבת אותן לפונקציות הטיפול המתאימות.
     */
    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8),
                    true);
            
            // שליחת הודעת פתיחה המאשרת ללקוח שהשרת מוכן
            writer.println("220 FTP Ready");

            String line;
            try {
                // לולאה שרצה כל עוד הלקוח מחובר ושולח פקודות
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ", 2);
                    String cmd = parts[0].toUpperCase();
                    String arg = parts.length > 1 ? parts[1] : "";

                    switch (cmd) {
                        case "USER" -> {
                            String username = arg.trim();
                            // אבטחת מידע: מניעת מתקפת Directory Traversal (כדי שלא יוכל לגשת לתיקיות של אחרים)
                            if (username.contains("..") || username.contains("/") || username.contains("\\")) {
                                writer.println("501 Invalid username format");
                            } else {
                                // נועלים את המשתמש לתיקייה האישית שלו בלבד
                                currentUserDir = username + File.separator;
                                writer.println("331 OK");
                            }
                        }
                        case "PASS" -> writer.println("230 Logged in");
                        case "OPTS", "TYPE" -> writer.println("200 OK");
                        case "PASV" -> handlePasv(); // כניסה למצב סביל להעברת נתונים
                        case "STOR" -> handleStor(arg); // העלאת קובץ לשרת
                        case "RETR" -> handleRetr(arg); // הורדת קובץ מהשרת
                        case "LIST" -> handleList(); // בקשת רשימת קבצים
                        case "QUIT" -> {
                            writer.println("221 Bye");
                            return; // יציאה מהלולאה וסיום התהליכון
                        }
                        default -> writer.println("502 Command not implemented");
                    }
                }
            } catch (SocketException e) {
                // הלקוח התנתק באופן פתאומי
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // ניקוי משאבים תמיד בסיום החיבור
            closePassive();
            try {
                controlSocket.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * מטפלת בפקודת PASV (Passive Mode).
     * פקודה זו מכינה את השרת לקבלת או שליחת נתונים (קבצים).
     * היא פותחת פורט אקראי חדש בשרת, ומדווחת ללקוח לאיזה פורט וכתובת IP הוא צריך להתחבר 
     * כדי להתחיל את הזרמת המידע.
     */
    private void handlePasv() throws IOException {
        closePassive();
        passiveServer = new ServerSocket(0); // מערכת ההפעלה תבחר פורט פנוי אקראי
        int port = passiveServer.getLocalPort();
        // מחזיר ללקוח את הכתובת והפורט בפורמט התקני של FTP
        writer.println("227 Entering Passive Mode (127,0,0,1," + (port / 256) + "," + (port % 256) + ")");
    }

    /**
     * פונקציית עזר לאבטחת מידע.
     * מנקה את נתיב הקובץ שנשלח מהלקוח, ומשאירה רק את שם הקובץ עצמו,
     * כדי למנוע ניסיונות פריצה ושמירת קבצים מחוץ לתיקיית המשתמש.
     * @param filename השם או הנתיב שהלקוח שלח
     * @return שם הקובץ הנקי
     */
    private String getSafePath(String filename) {
        return new File(filename).getName();
    }

    /**
     * מטפלת בפקודת STOR (Store / העלאה).
     * מקבלת קובץ מהלקוח וכותבת אותו אל הדיסק הקשיח של השרת.
     * * @param filename שם הקובץ שהלקוח רוצה לשמור
     */
    private void handleStor(String filename) {
        if (passiveServer == null) {
            writer.println("425 Use PASV first");
            return;
        }
        if (currentUserDir.isEmpty()) {
            writer.println("530 Please log in first");
            return;
        }

        // בניית הנתיב הפיזי: תיקיית שורש -> תיקיית המשתמש -> שם הקובץ הנקי
        File file = new File(rootDir + currentUserDir + getSafePath(filename));
        file.getParentFile().mkdirs(); // יצירת התיקייה במידה ואינה קיימת

        try {
            writer.println("150 Sending data");
            // המתנה לחיבור הנתונים מהלקוח אל הפורט הפסיבי שלנו, ואז כתיבת הזרם לקובץ
            try (Socket ds = passiveServer.accept();
                    InputStream in = ds.getInputStream();
                    OutputStream out = new FileOutputStream(file)) {
                in.transferTo(out); // העברה ישירה מהרשת אל הדיסק
                out.flush();
            }
            writer.println("226 Transfer complete"); // דיווח על סיום מוצלח
        } catch (Exception e) {
            writer.println("550 Error");
        } finally {
            closePassive(); // סגירת פורט הנתונים
        }
    }

    /**
     * מטפלת בפקודת RETR (Retrieve / הורדה).
     * פותחת קובץ קיים מהדיסק הקשיח של השרת ומזרימה אותו דרך הרשת אל הלקוח.
     * * @param filename שם הקובץ שהלקוח מבקש להוריד
     */
    private void handleRetr(String filename) {
        if (passiveServer == null) {
            writer.println("425 Use PASV");
            return;
        }
        File file = new File(rootDir + filename);
        if (!file.exists()) {
            writer.println("550 Not found");
            closePassive();
            return;
        }
        try {
            writer.println("150 Sending data");
            // המתנה לחיבור הנתונים מהלקוח, ואז שאיבת הקובץ מהדיסק לרשת
            try (Socket ds = passiveServer.accept();
                    OutputStream out = ds.getOutputStream();
                    InputStream in = new FileInputStream(file)) {
                in.transferTo(out);
                out.flush();
            }
            writer.println("226 Transfer complete");
        } catch (Exception e) {
        } finally {
            closePassive();
        }
    }

    /**
     * מטפלת בפקודת LIST.
     * סורקת את התיקייה הפיזית של המשתמש ומחזירה לו רשימה טקסטואלית של כל הקבצים שקיימים בה.
     * פונקציה זו מוודאת שהמשתמש רואה אך ורק את הקבצים שלו.
     */
    private void handleList() {
        // 1. מוודא שהלקוח פתח ערוץ נתונים (PASV)
        if (passiveServer == null) {
            writer.println("425 Use PASV first to establish a data connection");
            return;
        }

        // 2. מוודא שהמשתמש מחובר כדי שנדע איזו תיקייה להציג לו
        if (currentUserDir.isEmpty()) {
            writer.println("530 Please log in first");
            closePassive();
            return;
        }

        try {
            // 3. דיווח ללקוח שרשימת הקבצים מתחילה להישלח
            writer.println("150 Here comes the directory listing");

            // 4. פתיחת צינור הנתונים לשליחת הטקסט של הרשימה
            try (Socket ds = passiveServer.accept();
                    PrintWriter dw = new PrintWriter(
                            new OutputStreamWriter(ds.getOutputStream(), StandardCharsets.UTF_8), true)) {

                // 5. גישה לתיקייה הפיזית של המשתמש הנוכחי
                File dir = new File(rootDir + currentUserDir);

                // 6. מעבר על הקבצים ושליחתם בפורמט לינוקס סטנדרטי של שרתי FTP
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            // עיצוב השורה: הרשאות, בעלים, גודל, תאריך ושם הקובץ
                            String fileInfo = String.format("-rw-r--r-- 1 ftp ftp %d Jan 01 00:00 %s",
                                    f.length(), f.getName());
                            dw.println(fileInfo);
                        }
                    }
                }
            }

            // 7. עדכון הלקוח שהרשימה נשלחה במלואה
            writer.println("226 Directory send OK");

        } catch (Exception e) {
            writer.println("550 Error retrieving directory listing");
            e.printStackTrace();
        } finally {
            // 8. תמיד סוגרים את הפורט הפסיבי בסיום
            closePassive();
        }
    }

    /**
     * פונקציית ניקוי (Cleanup).
     * סוגרת את ה-ServerSocket הפסיבי (ערוץ הנתונים) כדי לשחרר את הפורט במערכת ההפעלה,
     * כך שיוכל לשמש לבקשות העלאה/הורדה עתידיות.
     */
    private void closePassive() {
        try {
            if (passiveServer != null)
                passiveServer.close();
        } catch (Exception e) {
        }
    }
}