package com.urielt.my_final_proj.services;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class FtpClientHandler implements Runnable {
    private final Socket controlSocket;
    private final String rootDir;
    private ServerSocket passiveServer;
    private PrintWriter writer;

    // Stores the directory specific to the logged-in user to isolate their files
    private String currentUserDir = "";

    public FtpClientHandler(Socket socket, String rootDir) {
        this.controlSocket = socket;
        this.rootDir = rootDir.endsWith(File.separator) ? rootDir : rootDir + File.separator;
    }

    @Override
    public void run() {
        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(controlSocket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(controlSocket.getOutputStream(), StandardCharsets.UTF_8),
                    true);
            writer.println("220 FTP Ready");

            String line;
            try {
                // Process incoming FTP commands from the client
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ", 2);
                    String cmd = parts[0].toUpperCase();
                    String arg = parts.length > 1 ? parts[1] : "";

                    switch (cmd) {
                        case "USER" -> {
                            String username = arg.trim();
                            // SECURITY: Prevent Directory Traversal Attacks (e.g., trying to access
                            // "../other_user")
                            if (username.contains("..") || username.contains("/") || username.contains("\\")) {
                                writer.println("501 Invalid username format");
                            } else {
                                // Lock the user into their own specific directory
                                currentUserDir = username + File.separator;
                                writer.println("331 OK");
                            }
                        }
                        case "PASS" -> writer.println("230 Logged in");
                        case "OPTS", "TYPE" -> writer.println("200 OK");
                        case "PASV" -> handlePasv(); // Enter Passive mode for data transfer
                        case "STOR" -> handleStor(arg); // Upload a file
                        case "RETR" -> handleRetr(arg); // Download a file
                        case "LIST" -> handleList(); // List files in directory
                        case "QUIT" -> {
                            writer.println("221 Bye");
                            return;
                        }
                        default -> writer.println("502 Command not implemented");
                    }
                }
            } catch (SocketException e) {
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closePassive();
            try {
                controlSocket.close();
            } catch (Exception e) {
            }
        }
    }

    private void handlePasv() throws IOException {
        closePassive();
        passiveServer = new ServerSocket(0); // Let the OS choose an available port
        int port = passiveServer.getLocalPort();
        writer.println("227 Entering Passive Mode (127,0,0,1," + (port / 256) + "," + (port % 256) + ")");
    }

    /**
     * Extracts only the file name, ignoring any malicious paths provided by the
     * client.
     */
    private String getSafePath(String filename) {
        return new File(filename).getName();
    }

    private void handleStor(String filename) {
        if (passiveServer == null) {
            writer.println("425 Use PASV first");
            return;
        }
        if (currentUserDir.isEmpty()) {
            writer.println("530 Please log in first");
            return;
        }

        // Build the isolated path: Root Storage -> User's Folder -> Safe File Name
        File file = new File(rootDir + currentUserDir + getSafePath(filename));
        file.getParentFile().mkdirs(); // Create the user's folder if it doesn't exist

        try {
            writer.println("150 Sending data");
            // Accept the data connection and save the incoming bytes to the file
            try (Socket ds = passiveServer.accept();
                    InputStream in = ds.getInputStream();
                    OutputStream out = new FileOutputStream(file)) {
                in.transferTo(out);
                out.flush();
            }
            writer.println("226 Transfer complete");
        } catch (Exception e) {
            writer.println("550 Error");
        } finally {
            closePassive();
        }
    }

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
     * Handles the LIST command to provide directory contents to the client.
     * This version ensures the user can only see files within their own isolated
     * directory.
     */
    private void handleList() {
        // 1. Check if the client initiated a passive connection (PASV) first
        if (passiveServer == null) {
            writer.println("425 Use PASV first to establish a data connection");
            return;
        }

        // 2. Ensure the user is identified so we know which directory to list
        if (currentUserDir.isEmpty()) {
            writer.println("530 Please log in first");
            closePassive();
            return;
        }

        try {
            // 3. Signal that we are ready to start the data transfer
            writer.println("150 Here comes the directory listing");

            // 4. Open the data connection (Data Socket) using the passive server
            try (Socket ds = passiveServer.accept();
                    PrintWriter dw = new PrintWriter(
                            new OutputStreamWriter(ds.getOutputStream(), StandardCharsets.UTF_8), true)) {

                // 5. Access the physical directory assigned to this specific user
                File dir = new File(rootDir + currentUserDir);

                // 6. Verify the directory exists and iterate through the files
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            // Formatting the output in standard Unix-like FTP list format
                            // Format: [Permissions] [Links] [Owner] [Group] [Size] [Date] [Name]
                            String fileInfo = String.format("-rw-r--r-- 1 ftp ftp %d Jan 01 00:00 %s",
                                    f.length(), f.getName());
                            dw.println(fileInfo);
                        }
                    }
                }
            }

            // 7. Inform the client that the data transfer finished successfully
            writer.println("226 Directory send OK");

        } catch (Exception e) {
            // Handle unexpected I/O errors and notify the client
            writer.println("550 Error retrieving directory listing");
            e.printStackTrace();
        } finally {
            // 8. Always close the passive server after a transfer to free up the port
            closePassive();
        }
    }

    private void closePassive() {
        try {
            if (passiveServer != null)
                passiveServer.close();
        } catch (Exception e) {
        }
    }
}