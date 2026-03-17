package com.ftp_proj.project_ftp_v1.utils;

import java.io.*;
import java.util.*;

public class Lz78Utils {
    // באפר גדול להאצת קריאה/כתיבה של קבצים ענקיים
    private static final int BUFFER_SIZE = 256 * 1024; // 256KB
    
    // גודל מילון אופטימלי: שומר על יחס דחיסה מעולה וצורך רק כ-20MB של RAM
    private static final int MAX_DICT_SIZE = 500000;

    private static class Node {
        int index;
        Map<Byte, Node> children = new HashMap<>();

        Node(int index) {
            this.index = index;
        }
    }

    public static void compress(InputStream in, OutputStream out) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out, BUFFER_SIZE));
        InputStream bufferedIn = (in instanceof BufferedInputStream) ? in : new BufferedInputStream(in, BUFFER_SIZE);

        Node root = new Node(0);
        Node current = root;
        int dictSize = 1;
        int read;

        while ((read = bufferedIn.read()) != -1) {
            byte b = (byte) read;
            Node next = current.children.get(b);

            if (next != null) {
                current = next;
            } else {
                dataOut.writeInt(current.index);
                dataOut.writeBoolean(true);
                dataOut.writeByte(b);

                // סנכרון מילון: מוסיפים רק אם יש מקום
                if (dictSize < MAX_DICT_SIZE) {
                    current.children.put(b, new Node(dictSize++));
                } else {
                    // המילון מלא - מאפסים אותו בשקט ומתחילים מחדש!
                    root = new Node(0);
                    dictSize = 1;
                }
                current = root;
            }
        }

        if (current != root) {
            dataOut.writeInt(current.index);
            dataOut.writeBoolean(false);
        }
        dataOut.flush();
    }

    public static void decompress(InputStream in, OutputStream out) throws IOException {
        DataInputStream dataIn = new DataInputStream(new BufferedInputStream(in, BUFFER_SIZE));
        BufferedOutputStream dataOut = new BufferedOutputStream(out, BUFFER_SIZE);

        List<byte[]> dict = new ArrayList<>(MAX_DICT_SIZE);
        dict.add(new byte[0]); // אינדקס 0

        try {
            while (true) {
                int index = dataIn.readInt();
                boolean hasValue = dataIn.readBoolean();

                // הגנת אבטחה וסנכרון
                if (index < 0 || index >= dict.size()) {
                    throw new IOException("Corrupted file or desynced dictionary. Index: " + index);
                }

                byte[] prefix = dict.get(index);

                if (hasValue) {
                    byte value = dataIn.readByte();
                    byte[] entry = new byte[prefix.length + 1];
                    System.arraycopy(prefix, 0, entry, 0, prefix.length);
                    entry[entry.length - 1] = value;

                    dataOut.write(entry);

                    // סנכרון מילון: זהה לחלוטין ללוגיקה של ה-compress!
                    if (dict.size() < MAX_DICT_SIZE) {
                        dict.add(entry);
                    } else {
                        dict.clear();
                        dict.add(new byte[0]);
                    }
                } else {
                    dataOut.write(prefix);
                }
            }
        } catch (EOFException e) {
            // סוף הקובץ - תקין לחלוטין
        }
        dataOut.flush();
    }
}