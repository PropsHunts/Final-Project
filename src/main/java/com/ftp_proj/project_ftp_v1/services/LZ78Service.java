package com.ftp_proj.project_ftp_v1.services;

import org.springframework.stereotype.Service;
import java.io.*;
import java.util.*;

@Service
public class LZ78Service {
    // We use a 256KB buffer to optimize I/O operations and speed up processing
    private static final int BUFFER_SIZE = 256 * 1024;
    // Maximum size for our compression dictionary to prevent memory leaks
    private static final int MAX_DICT_SIZE = 450000;

    // Node structure for the compression dictionary tree
    private static class Node {
        int index;
        Map<Byte, Node> children = new HashMap<>();
        Node(int index) { this.index = index; }
    }

    /**
     * Compresses the input stream using the LZ78 algorithm and writes to the output stream.
     */
    public void compress(InputStream in, OutputStream out) throws IOException {
        DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(out, BUFFER_SIZE));
        InputStream bufferedIn = (in instanceof BufferedInputStream) ? in : new BufferedInputStream(in, BUFFER_SIZE);

        Node root = new Node(0);
        Node current = root;
        int dictSize = 1;
        int read;

        // Read byte by byte from the uploaded file
        while ((read = bufferedIn.read()) != -1) {
            byte b = (byte) read;
            Node next = current.children.get(b);

            // If the sequence exists in our dictionary, move to the next node
            if (next != null) {
                current = next;
            } else {
                // Sequence not found: output current index and the new byte
                dataOut.writeInt(current.index);
                dataOut.writeBoolean(true);
                dataOut.writeByte(b);

                // Add the new sequence to the dictionary if we have space
                if (dictSize < MAX_DICT_SIZE) {
                    current.children.put(b, new Node(dictSize++));
                } else {
                    // Reset dictionary if it gets too large
                    root = new Node(0);
                    dictSize = 1;
                }
                current = root; // Reset current node back to root
            }
        }

        // Handle the last remaining sequence if the stream ends
        if (current != root) {
            dataOut.writeInt(current.index);
            dataOut.writeBoolean(false);
        }
        dataOut.flush();
    }

    /**
     * Decompresses an LZ78 compressed input stream back into its original form.
     */
    public void decompress(InputStream in, OutputStream out) throws IOException {
        DataInputStream dataIn = new DataInputStream(new BufferedInputStream(in, BUFFER_SIZE));
        BufferedOutputStream dataOut = new BufferedOutputStream(out, BUFFER_SIZE);

        // Rebuild the dictionary list
        List<byte[]> dict = new ArrayList<>(MAX_DICT_SIZE);
        dict.add(new byte[0]); // Index 0 represents an empty sequence

        try {
            while (true) {
                // Read the index and check if a new byte value follows
                int index = dataIn.readInt();
                boolean hasValue = dataIn.readBoolean();

                // Security/Integrity check
                if (index < 0 || index >= dict.size()) {
                    throw new IOException("Desynced dictionary. Corrupted file.");
                }

                byte[] prefix = dict.get(index);

                if (hasValue) {
                    byte value = dataIn.readByte();
                    
                    // Combine the existing prefix with the new byte
                    byte[] entry = new byte[prefix.length + 1];
                    System.arraycopy(prefix, 0, entry, 0, prefix.length);
                    entry[entry.length - 1] = value;

                    // Write the reconstructed original data to the output stream
                    dataOut.write(entry);

                    // Add to dictionary to keep in sync with the compressor
                    if (dict.size() < MAX_DICT_SIZE) {
                        dict.add(entry);
                    } else {
                        dict.clear();
                        dict.add(new byte[0]);
                    }
                } else {
                    // If there's no new value, just write the prefix
                    dataOut.write(prefix);
                }
            }
        } catch (EOFException e) {
            // End of stream reached successfully, nothing to do here
        }
        dataOut.flush();
    }
}