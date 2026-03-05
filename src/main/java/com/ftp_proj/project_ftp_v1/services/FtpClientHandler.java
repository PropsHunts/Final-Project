package com.ftp_proj.project_ftp_v1.services;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets; // חשוב!

public class FtpClientHandler implements Runnable {
    private final Socket controlSocket;
    private final String rootDir;
    private BufferedReader reader;
    private PrintWriter writer;
    private ServerSocket passiveServer;

    public FtpClientHandler(Socket socket, String rootDir) {
        this.controlSocket = socket;
        this.rootDir = rootDir.endsWith("/") ? rootDir : rootDir + "/";
    }

    @Override
    public void run() {
        try {
            // *** תיקון קריטי: הגדרת UTF-8 לקריאה וכתיבה ***
            // זה מה שיתקן את ה-????? בשם הקובץ
            reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            
            writer.println("220 FTP Ready (UTF-8)");

            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    // לוג בשרת כדי שנראה מה הוא קיבל
                    System.out.println("[SERVER] Received: " + line);

                    String[] parts = line.split(" ", 2);
                    String command = parts[0].toUpperCase();
                    String arg = parts.length > 1 ? parts[1] : "";

                    switch (command) {
                        case "USER" -> writer.println("331 OK");
                        case "PASS" -> writer.println("230 Logged in");
                        case "OPTS" -> { // תמיכה ב-UTF8 אם הלקוח מבקש
                            if (arg.equalsIgnoreCase("UTF8 ON")) writer.println("200 OK");
                            else writer.println("502 Unknown option");
                        }
                        case "TYPE" -> writer.println("200 Type set to " + arg);
                        case "PASV" -> handlePasv();
                        case "STOR" -> handleStor(arg);
                        case "RETR" -> handleRetr(arg);
                        case "LIST" -> handleList();
                        case "QUIT" -> { writer.println("221 Bye"); return; }
                        default -> writer.println("502 Command not implemented");
                    }
                }
            } catch (SocketException e) {
                System.out.println("[SERVER] Client disconnected abruptly.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeConnections();
        }
    }

    private void handlePasv() throws IOException {
        if (passiveServer != null && !passiveServer.isClosed()) passiveServer.close();
        try {
            passiveServer = new ServerSocket(0);
            int port = passiveServer.getLocalPort();
            int p1 = port / 256;
            int p2 = port % 256;
            writer.println("227 Entering Passive Mode (127,0,0,1," + p1 + "," + p2 + ")");
        } catch (IOException e) {
            writer.println("425 Can't open passive connection");
        }
    }

    private void handleStor(String filename) {
        if (passiveServer == null || passiveServer.isClosed()) {
            writer.println("425 Use PASV");
            return;
        }

        // ניקוי תווים בעייתיים אם עדיין נשארו (אבטחה)
        filename = new File(filename).getName(); 
        File outputFile = new File(rootDir + filename);
        
        System.out.println("[SERVER] Saving file to: " + outputFile.getAbsolutePath());

        try {
            writer.println("150 Sending data");
            try (Socket dataSocket = passiveServer.accept();
                 InputStream input = dataSocket.getInputStream();
                 OutputStream fileOut = new FileOutputStream(outputFile)) {
                
                // העתקה בבלוקים (שרת)
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    fileOut.write(buffer, 0, bytesRead);
                }
                fileOut.flush();
            }
            passiveServer.close();
            writer.println("226 Transfer complete");
            System.out.println("[SERVER] File saved successfully.");
        } catch (IOException e) {
            System.err.println("[SERVER] Error writing file: " + e.getMessage());
            writer.println("550 Error writing file");
        }
    }
    
    // handleRetr ו-handleList ו-closeConnections נשארים אותו דבר...
    // רק וודא שגם ב-handleRetr יש שימוש ב-buffer כמו ב-STOR אם צריך,
    // אבל input.transferTo עושה בדיוק את זה פנימית ב-Java 9+
     private void handleRetr(String filename) {
        if (passiveServer == null || passiveServer.isClosed()) { writer.println("425 Use PASV"); return; }
        File file = new File(rootDir + filename);
        if (!file.exists()) { writer.println("550 File not found"); closePassive(); return; }

        try {
            writer.println("150 Opening binary mode data connection");
            try (Socket dataSocket = passiveServer.accept();
                 OutputStream output = dataSocket.getOutputStream();
                 InputStream fileIn = new FileInputStream(file)) {
                // העתקה בבלוקים
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileIn.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.flush();
            }
            writer.println("226 Transfer complete");
        } catch (IOException e) { System.out.println("Download interrupted"); } 
        finally { closePassive(); }
    }
    
    private void handleList() {
        if (passiveServer == null || passiveServer.isClosed()) {
            writer.println("425 Use PASV");
            return;
        }
        try {
            writer.println("150 Here comes the directory listing.");
            try (Socket dataSocket = passiveServer.accept();
                 PrintWriter dataWriter = new PrintWriter(dataSocket.getOutputStream(), true)) {
                File dir = new File(rootDir);
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        dataWriter.println("-rw-r--r-- 1 ftp ftp " + f.length() + " Jan 01 00:00 " + f.getName());
                    }
                }
            }
            writer.println("226 Directory send OK.");
        } catch (IOException e) {
            writer.println("550 Error");
        } finally {
            closePassive();
        }
    }
    
    private void closePassive() {
        try {
            if (passiveServer != null && !passiveServer.isClosed()) {
                passiveServer.close();
            }
        } catch (IOException e) {}
    }

    private void closeConnections() {
        closePassive();
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (IOException e) {}
    }
}