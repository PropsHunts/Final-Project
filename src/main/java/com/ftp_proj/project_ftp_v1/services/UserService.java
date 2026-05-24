package com.ftp_proj.project_ftp_v1.services;

import java.util.ArrayList;
import java.util.List;

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
        // 1. validation
        if (!userRepository.existsById(user.getUsername())) {
            // 2. send user to repository DB
            userRepository.save(user);
            return true;
        }

        return false;
    }

    public User loginUser(String email, String password) {
        return userRepository.findOneByUsernameAndPassword(email, password);
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

    public boolean updateProfile(User currentUser, String newUsername, String newEmail) {
        String oldEmail = currentUser.getEmail();
        boolean isEmailChanged = !oldEmail.equalsIgnoreCase(newEmail);

        // 1. אם האימייל השתנה, נבדוק שהאימייל החדש לא תפוס כבר במערכת
        if (isEmailChanged) {
            if (userRepository.existsById(newEmail)) {
                return false; // האימייל החדש כבר תפוס על ידי משתמש אחר
            }
        }

        // 2. עדכון שדות הנתונים באובייקט
        currentUser.setUsername(newUsername);

        if (isEmailChanged) {
            // מחיקת המשתמש תחת האימייל הישן (ה-ID הישן)
            userRepository.deleteById(oldEmail);

            // הגדרת ה-ID החדש ושמירה כרשומה חדשה
            currentUser.setEmail(newEmail);
            userRepository.save(currentUser);
        } else {
            // אם רק ה-username השתנה, save רגיל מעדכן לפי ה-email הקיים
            userRepository.save(currentUser);
        }

        return true;
    }
}
