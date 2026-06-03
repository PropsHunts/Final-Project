package com.urielt.my_final_proj.datamodels;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "Users")
public class User {

    @Id
    private String email; // Unique identifier for the user
    @Indexed(unique = true)
    private String username;
    private String password; // Plain text for now, should be hashed later
    private LocalDateTime createdAt; // Date the account was created
    private List<String> accessibleFilesIds; // List of file IDs owned by this user

    public User() {
        this.accessibleFilesIds = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    public User(String email, String username, String password) {
        this.email = email;
        this.username = username;
        this.password = password;
        this.accessibleFilesIds = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<String> getAccessibleFilesIds() { return accessibleFilesIds; }
    public void setAccessibleFilesIds(List<String> accessibleFilesIds) { this.accessibleFilesIds = accessibleFilesIds; }
    public void setFileId(String fileID) { this.accessibleFilesIds.add(fileID); }
    public void printAllFilesID() {
        System.out.println("=======" + this.username + "=======");
        for (int i = 0; i < this.accessibleFilesIds.size(); i ++) {
            System.out.print(this.accessibleFilesIds.get(i) + ", ");
        }
        System.out.println("\n=====================");
    }
    public void removeFileID(String fileID) {
        this.accessibleFilesIds.removeIf(fileid -> fileid.equals(fileID));
    }
}