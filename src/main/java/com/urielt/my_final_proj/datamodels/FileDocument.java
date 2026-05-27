package com.urielt.my_final_proj.datamodels;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;


@Document(collection = "Files")
public class FileDocument {

    @Id
    private String fileId; // Unique identifier for the file document
    private String ownerEmail; // Reference to the user who uploaded the file
    private String fileName;
    private long originalSize; // Size in bytes before LZ78 compression
    private long compressedSize; // Size in bytes after LZ78 compression
    private LocalDateTime uploadDate;

    public FileDocument() {
        this.uploadDate = LocalDateTime.now();
    }

    public FileDocument(String ownerEmail, String fileName, long originalSize) {
        this.ownerEmail = ownerEmail;
        this.fileName = fileName;
        this.originalSize = originalSize;
        this.uploadDate = LocalDateTime.now();
    }

    // Getters and Setters
    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getOwnerEmail() { return ownerEmail; }
    public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public long getOriginalSize() { return originalSize; }
    public void setOriginalSize(long originalSize) { this.originalSize = originalSize; }

    public long getCompressedSize() { return compressedSize; }
    public void setCompressedSize(long compressedSize) { this.compressedSize = compressedSize; }

    public LocalDateTime getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDateTime uploadDate) { this.uploadDate = uploadDate; }
}