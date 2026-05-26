package com.ftp_proj.project_ftp_v1.services;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;

/**
 * מחלקת שירות (Service) מרכזית המיישמת את אלגוריתם הדחיסה LZ78.
 * המחלקה בנויה לעבוד בתצורת On-the-fly (הזרמה ישירה) ללא שמירת הקבצים המלאים
 * שיטה זו מאפשרת התמודדות עם קבצי ענק מבלי להקריס את השרת (OutOfMemoryError).
 */
@Service
public class LZ78Service {

    /**
     * גודל החוצץ (Buffer) הוגדר ל-256KB.
     * במקום לפנות לדיסק הקשיח עבור כל בית (Byte) בודד - פעולה איטית מאוד,
     * אנחנו אוספים נתונים בזיכרון עד 256KB ורק אז "שופכים" אותם לדיסק. זה משפר
     * דרמטית את זמני ה-I/O.
     */
    private static final int BUFFER_SIZE = 256 * 1024;

    /**
     * גבול עליון לגודל המילון (450,000 אינדקסים).
     * מונע דליפת זיכרון (Memory Leak). ברגע שהמילון מתמלא, הוא מתאפס.
     * זה שומר על טביעת זיכרון (Memory Footprint) קבועה וצנועה, גם בדחיסת קובץ של
     * 10GB.
     */
    private static final int MAX_DICT_SIZE = 450000;

    /**
     * מבנה נתונים מסוג צומת (Node) המרכיב את "עץ התחיליות" (Trie) של המילון בדחיסה.
     * שימוש בעץ תחיליות מאפשר חיפוש רצפים בסיבוכיות זמן של $O(1)$ לכל תו שנקרא.
     */
    private static class Node {
        int index; // המספר המזהה של התחילית שייכתב לקובץ
        Map<Byte, Node> children = new HashMap<>(); // ילדי הצומת (התווים הבאים האפשריים)

        Node(int index) {
            this.index = index;
        }
    }

    /**
     * פונקציה לדחיסת (Compression) זרם נתונים באלגוריתם LZ78.
     * * * מה הפונקציה עושה:
     * קוראת נתונים גולמיים מהלקוח (InputStream), מזהה תבניות חוזרות תוך כדי תנועה,
     * וכותבת זוגות של (אינדקס תחילית, בית חדש) ישירות ל-FTP.
     * * * איך זה עובד:
     * 1. בונה עץ תחיליות (Trie) שמתחיל בצומת שורש ריק.
     * 2. קוראת בית אחר בית. כל עוד הרצף מוכר (קיים בעץ), יורדים שלב למטה בעץ.
     * 3. ברגע שנתקלים ברצף חדש שלא מוכר:
     * א. פולטים לקובץ את האינדקס של הצומת האחרון שהכרנו.
     * ב. פולטים את הבית (Byte) החדש שברר את הרצף.
     * ג. מוסיפים את הרצף החדש לעץ, וחוזרים לשורש.
     * * * סיבוכיות זמן ריצה:
     * $O(N)$ - כאשר N הוא כמות הבתים (Bytes) בקובץ המקורי.
     * עוברים על הקובץ בדיוק פעם אחת (Single Pass). השליפה וההכנסה למבנה ה-HashMap
     * שבעץ מתבצעות ב-$O(1)$.
     * * @param in זרם הקלט הגולמי (ישירות מהעלאת הדפדפן)
     * 
     * @param out זרם הפלט לדחיפה (ישירות לשרת ה-FTP)
     * @throws IOException במקרה של שגיאת I/O
     */
    public void compress(InputStream in, OutputStream out) throws IOException {
        // עטיפת הזרמים בחוצצים לייעול הביצועים (כתיבה וקריאה בגושים של 256KB)
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out, BUFFER_SIZE));
        InputStream bufferedIn = (in instanceof BufferedInputStream) ? in : new BufferedInputStream(in, BUFFER_SIZE);

        // אתחול המילון: יצירת שורש העץ (אינדקס 0 מייצג מחרוזת ריקה)
        Node root = new Node(0);
        Node current = root;
        int dictSize = 1; // מונה גודל המילון הנוכחי
        int read;

        // קריאת הקובץ בית (Byte) אחר בית עד להגעה לסופו (1- מסמל EOF)
        while ((read = bufferedIn.read()) != -1) {
            byte b = (byte) read;

            // חיפוש הבית בענפים של הצומת הנוכחי (האם הרצף כבר קיים במילון?)
            Node next = current.children.get(b);

            if (next != null) {
                // הרצף מוכר: מתקדמים לצומת הבא בעץ ולא פולטים עדיין כלום לקובץ
                current = next;
            } else {
                // נתקלנו ברצף חדש שלא קיים במילון. כותבים את הפלט:
                dataOut.writeInt(current.index); // כותבים את האינדקס של הרצף הארוך ביותר שהכרנו
                dataOut.writeBoolean(true); // מציינים שיש תו חדש שמצטרף לאינדקס
                dataOut.writeByte(b); // כותבים את הבית החדש עצמו

                // מוסיפים את הרצף החדש למילון, בתנאי שלא חרגנו מהגבול המקסימלי
                if (dictSize < MAX_DICT_SIZE) {
                    current.children.put(b, new Node(dictSize++));
                } else {
                    // הגנה על ה-RAM: הגענו לגבול, מאפסים את המילון ומתחילים לאסוף רצפים מחדש
                    root = new Node(0);
                    dictSize = 1;
                }
                // מאפסים את הצומת הנוכחי בחזרה לשורש העץ לקראת הרצף הבא
                current = root;
            }
        }

        // טיפול במקרה קצה: הגענו לסוף הקובץ בדיוק כשהצטבר לנו רצף מוכר בזיכרון שעוד לא
        // פלטנו
        if (current != root) {
            dataOut.writeInt(current.index); // פולטים את האינדקס שלו
            dataOut.writeBoolean(false); // מסמנים false כי אין בית חדש שאחריו
        }

        // כתיבת שאריות החוצץ (Buffer) באופן סופי לקובץ ב-FTP
        dataOut.flush();
    }

    /**
     * פונקציה לפריסת (Decompression) זרם נתונים שנדחס באלגוריתם LZ78.
     * * * מה הפונקציה עושה:
     * מקבלת קובץ בינארי דחוס, משחזרת אותו לפורמט המקורי שלו (ללא אובדן מידע -
     * Lossless)
     * ומזרימה את המידע הפרוס ישירות אל דפדפן המשתמש בזמן אמת.
     * * * איך זה עובד:
     * 1. קוראת בצורה סדרתית צמדים מזרם הקלט: מספר שלם (Index) וערך בוליאני המציין
     * אם יש בית (Byte) חדש.
     * 2. משחזרת את הרצף המקורי על ידי שליפת תחילית מהמילון הדינמי לפי האינדקס,
     * וחיבור הבית החדש אליו.
     * 3. הרצף המשוחזר נכתב ישירות לזרם הפלט, ובמקביל מתווסף למילון כדי לשמור על
     * סנכרון מול כיווץ הדחיסה.
     * 4. מיושם מנגנון Catch נקודתי שעוצר את הפריסה באופן יזום אם הלקוח סוגר את
     * ההורדה באמצע.
     * * * סיבוכיות זמן ריצה:
     * $O(N)$ - כאשר N הוא כמות רשומות הקידוד (הצמדים) בקובץ הדחוס.
     * מעבר סדרתי יחיד. שליפת הרצף מהמילון (מבוסס ArrayList) והוספה אליו מתבצעות
     * בסיבוכיות $O(1)$.
     * * @param in זרם הקלט הדחוס משרת ה-FTP
     * 
     * @param out זרם הפלט המקושר ישירות לצינור התגובה של הדפדפן
     *            (ServletOutputStream)
     * @throws IOException במקרה של שגיאות קריאה חריגות
     */
    public void decompress(InputStream in, OutputStream out) throws IOException {
        DataInputStream dataIn = new DataInputStream(new BufferedInputStream(in, BUFFER_SIZE));
        BufferedOutputStream dataOut = new BufferedOutputStream(out, BUFFER_SIZE);

        // בנייה מחדש של מילון הפריסה כרשימה דינמית של מערכי בתים (האינדקס ברשימה מקביל
        // לאינדקס בעץ הדחיסה)
        List<byte[]> dict = new ArrayList<>(MAX_DICT_SIZE);
        dict.add(new byte[0]); // אינדקס 0 תמיד מייצג מחרוזת ריקה

        try {
            while (true) {
                // קריאת רשומת LZ78: אינדקס מהמילון והאם יש אחריו ערך
                int index = dataIn.readInt();
                boolean hasValue = dataIn.readBoolean();

                // בקרת שלמות: אם קראנו אינדקס שלא קיים במילון עדיין, הקובץ פגום
                if (index < 0 || index >= dict.size()) {
                    throw new IOException("Desynced dictionary. Corrupted file.");
                }

                // שליפת הרצף המוכר מהמילון ב-O(1)
                byte[] prefix = dict.get(index);
                byte[] entry;

                if (hasValue) {
                    // קריאת הבית החדש שמצטרף לתחילית
                    byte value = dataIn.readByte();

                    // יצירת מערך חדש המשלב את התחילית הקיימת יחד עם הבית החדש
                    entry = new byte[prefix.length + 1];
                    System.arraycopy(prefix, 0, entry, 0, prefix.length);
                    entry[entry.length - 1] = value;

                    // הוספת הרצף המלא החדש למילון, כדי שנוכל להשתמש בו בהמשך הפריסה
                    if (dict.size() < MAX_DICT_SIZE) {
                        dict.add(entry);
                    } else {
                        // המילון התמלא - מאפסים בדיוק כפי שעשינו בפונקציית ה-compress
                        dict.clear();
                        dict.add(new byte[0]);
                    }
                } else {
                    // אם אין ערך חדש (סוף קובץ), הרצף שמשוחזר הוא התחילית בעצמה
                    entry = prefix;
                }

                try {
                    // הזרמת הרצף המשוחזר ללקוח (דפדפן)
                    dataOut.write(entry);
                } catch (IOException e) {
                    // לקוח עשה "ביטול" הורדה. עוצרים את הפעולה ישר ולא מבזבזים משאבים להמשך הקריאה
                    // מה-FTP
                    break;
                }
            }
        } catch (EOFException e) {
            // הגענו לסוף הקובץ, סיום קריאה תקין
        }

        try {
            // דחיפה סופית של השאריות מהחוצץ לדפדפן
            dataOut.flush();
        } catch (IOException e) {
            // הלקוח סגר את החיבור בשבריר השנייה האחרון. השתקה - אין צורך לזרוק שגיאה
        }
    }
}