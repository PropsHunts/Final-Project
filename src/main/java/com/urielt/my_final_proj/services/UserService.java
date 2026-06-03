package com.urielt.my_final_proj.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.urielt.my_final_proj.datamodels.User;
import com.urielt.my_final_proj.repositories.UserRepository;
import com.urielt.my_final_proj.utils.PasswordHelper;

@Service
public class UserService {
    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean addUserToDB(User user) {
        // 1. validation
        if (!userRepository.existsById(user.getEmail())) {
            // 2. send user to repository DB
            userRepository.save(user);
            return true;
        }

        return false;
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.findByUsername(username) != null;
    }

    public boolean isEmailTaken(String email) {
        return userRepository.findById(email).isPresent();
    }

    public User loginUser(String un, String pw) {
        User usr = userRepository.findByUsername(un);
        if (usr == null)  return null;
        if (!PasswordHelper.getInstance().matches(pw, usr.getPassword())) return null;
        return usr;
        // return userRepository.findOneByUsernameAndPassword(un, pw);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getAllUsersLikeName(String un) {
        return userRepository.findByUsernameLike(un);
    }

    public boolean deleteItem(User user) {
        if (userRepository.findById(user.getEmail()) != null) {
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
