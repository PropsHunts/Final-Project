package com.ftp_proj.project_ftp_v1.services;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class FtpClientHandler implements Runnable {
    private final Socket controlSocket;
    private final String rootDir;
    private ServerSocket passiveServer;
    private PrintWriter writer;

    public FtpClientHandler(Socket socket, String rootDir) {
        this.controlSocket = socket;
        if (!rootDir.endsWith("/") && !rootDir.endsWith("\\")) {
            this.rootDir = rootDir + "/";
        } else {
            this.rootDir = rootDir;
        }
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8), true);
            writer.println("220 FTP Ready");

            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ", 2);
                    String cmd = parts[0].toUpperCase(), arg = parts.length > 1 ? parts[1] : "";
                    switch (cmd) {
                        case "USER" -> writer.println("331 OK");
                        case "PASS" -> writer.println("230 Logged in");
                        case "OPTS" -> writer.println("200 OK");
                        case "TYPE" -> writer.println("200 OK");
                        case "PASV" -> handlePasv();
                        case "STOR" -> handleStor(arg);
                        case "RETR" -> handleRetr(arg);
                        case "LIST" -> handleList(arg);
                        case "QUIT" -> { writer.println("221 Bye"); return; }
                        default -> writer.println("502 Command not implemented");
                    }
                }
            } catch (SocketException e) {}
        } catch (Exception e) { e.printStackTrace(); } 
        finally { closePassive(); try { controlSocket.close(); } catch (Exception e) {} }
    }

    private void handlePasv() throws IOException {
        closePassive();
        passiveServer = new ServerSocket(0);
        int port = passiveServer.getLocalPort();
        writer.println("227 Entering Passive Mode (127,0,0,1," + (port/256) + "," + (port%256) + ")");
    }

    private void handleStor(String filename) {
        if (passiveServer == null) { writer.println("425 Use PASV"); return; }
        File file = new File(rootDir + filename);
        file.getParentFile().mkdirs(); // יצירת תיקיית המשתמש אם חסרה
        try {
            writer.println("150 Sending data");
            try (Socket ds = passiveServer.accept(); InputStream in = ds.getInputStream(); OutputStream out = new FileOutputStream(file)) {
                in.transferTo(out); out.flush();
            }
            writer.println("226 Transfer complete");
        } catch (Exception e) { writer.println("550 Error"); } finally { closePassive(); }
    }

    private void handleRetr(String filename) {
        if (passiveServer == null) { writer.println("425 Use PASV"); return; }
        File file = new File(rootDir + filename);
        if (!file.exists()) { writer.println("550 Not found"); closePassive(); return; }
        try {
            writer.println("150 Sending data");
            try (Socket ds = passiveServer.accept(); OutputStream out = ds.getOutputStream(); InputStream in = new FileInputStream(file)) {
                in.transferTo(out); out.flush();
            }
            writer.println("226 Transfer complete");
        } catch (Exception e) { } finally { closePassive(); }
    }

    private void handleList(String dirName) {
        if (passiveServer == null) { writer.println("425 Use PASV"); return; }
        try {
            writer.println("150 Here comes the directory listing");
            try (Socket ds = passiveServer.accept(); PrintWriter dw = new PrintWriter(ds.getOutputStream(), true)) {
                File dir = new File(rootDir + dirName);
                if (dir.exists() && dir.listFiles() != null) {
                    for (File f : dir.listFiles()) dw.println("-rw-r--r-- 1 ftp ftp " + f.length() + " Jan 01 00:00 " + f.getName());
                }
            }
            writer.println("226 Directory send OK");
        } catch (Exception e) { writer.println("550 Error"); } finally { closePassive(); }
    }
    
    private void closePassive() { try { if (passiveServer != null) passiveServer.close(); } catch (Exception e) {} }
}