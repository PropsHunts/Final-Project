package com.ftp_proj.project_ftp_v1.datamodels;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "Users")
public class User {

    @Id
    private String email;
    private String username;
    private String password;

    public User() {

    }

    public User(String email, String username, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return super.toString();
    }
    
}
