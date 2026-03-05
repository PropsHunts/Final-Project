package com.ftp_proj.project_ftp_v1.services;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

@Service
public class MyFtpServer {
private static final int PORT = 2121;
    public static final String STORAGE_DIR = "storage";
    private boolean isRunning = true;
    private ServerSocket serverSocket;

    @PostConstruct
    public void startServer() {
        new File(STORAGE_DIR).mkdirs(); // יצירת תיקייה
        Thread.ofVirtual().start(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println(">>> Custom FTP Server started on port " + PORT);
                while (isRunning) {
                    Socket clientSocket = serverSocket.accept();
                    FtpClientHandler handler = new FtpClientHandler(clientSocket, STORAGE_DIR);
                    Thread.ofVirtual().start(handler);
                }
            } catch (IOException e) {
                if (isRunning) e.printStackTrace();
            }
        });
    }
    
    @PreDestroy
    public void stopServer() {
        isRunning = false;
        try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) {}
    }
}