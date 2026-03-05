package com.ftp_proj.project_ftp_v1.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ftp_proj.project_ftp_v1.datamodels.User;
import com.ftp_proj.project_ftp_v1.repositories.UserRepository;

@Service
public class UserService {
    private UserRepository userRepository;
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean addUserToDB(User user) {
        //1. validation
        if (!userRepository.existsById(user.getUsername())) {
            //2. send user to repository DB
            userRepository.insert(user);
            return true;
        }
        
        return false;
    }

    public ArrayList<User> getAllUsers() {
        return (ArrayList<User>) userRepository.findAll();
    }

    public List<User> getAllUsersLikeName(String un) {
        return userRepository.findByUsernameLike(un);
    }

    public List<User> getAllUsersByName(String un) {
        return userRepository.findByUsername(un);
    }

    public User getOneByUsernameAndPassword(String un, String pw) {
        return userRepository.findOneByUsernameAndPassword(un, pw);
    }

    public boolean deleteItem(User user) {
        if (userRepository.findById(user.getUsername()) != null) {
            userRepository.delete(user);
            return true;
        }
        return false;
    }

    public User getUser(String un, String pw) {
        return userRepository.findOneByUsernameAndPassword(un, pw);
    }
    
}
