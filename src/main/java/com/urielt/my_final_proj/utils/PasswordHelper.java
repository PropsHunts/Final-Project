package com.urielt.my_final_proj.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * מחלקת עזר לניהול אבטחת סיסמאות במסד הנתונים. *
 * המחלקה משמתמשת ב BcryptPasswordEncoder לצורך קידוד סיסמאות (Hashing (ואימותן.
 * *
 * מימוש המחלקה נעשה בתבנית-עיצוב Singleton המבטיח קיום מופע יחיד, *
 * של ה Encoder לאורך כל חיי האפליקציה. *
 */
public class PasswordHelper {
// מופע יחיד
//     של המחלקה, נוצר
//     ברגע טעינת
    // המחלקה לזיכרון //
    private static final PasswordHelper INSTANCE = new PasswordHelper();
    private final PasswordEncoder passwordEncoder;

    /**
     * בנאי פרטי למניעת יצירת מופעים חיצוניים. *
     * מאתחל את האובייקט BcryptPasswordEncoder באמצעותו ניתן לקודד ולאמת סיסמאות. *
     */
    private PasswordHelper() {
        // יצירת אובייקט מהמחלקה BcryptPasswordEncoder השייכת ל Boot Spring //
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * מחזיר את המופע היחיד (Singleton (של מחלקת העזר. *
     * מופע של PasswordHelper return* @
     */
    public static PasswordHelper getInstance() {
        return INSTANCE;
    }

    /**
     * מקודד סיסמה גולמית (Text Plain / String (לפורמט Hash מאובטח, הכולל Salt מובנה
     * *
     * הסיסמה הגולמית שהתקבלה מהמשתמש rawPassword param* @
     * מחרוזת ה Hash עבור הסיסמא המקודדת, שניתן לשמור בבסיס הנתונים return* @
     */
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    /**
     * מאמת סיסמה גולמית מול Hash קיים, שנשמר במסד הנתונים. *
     * הפעולה מחלצת את ה Salt מתוך ה Hash השמור, ומבצעת את בדיקת האימות. *
     * הסיסמה שהוזנה בטופס ההתחברות rawPassword param* @
     * ה Hash השמור במסד הנתונים encodedPassword param* @
     * אם הסיסמאות תואמות יוחזר true, אחרת מוחזר false return* @
     */
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
