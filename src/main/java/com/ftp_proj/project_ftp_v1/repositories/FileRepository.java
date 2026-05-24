package com.ftp_proj.project_ftp_v1.repositories;

import com.ftp_proj.project_ftp_v1.datamodels.FileDocument;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/**
 * Repository interface for FileDocument metadata.
 * Handles all MongoDB operations for the 'Files' collection.
 */
@Repository
public interface FileRepository extends MongoRepository<FileDocument, String> {
    
    // Finds all files belonging to a specific user
    List<FileDocument> findByOwnerEmail(String ownerEmail);
    
    // Finds a specific file by owner and name
    FileDocument findByOwnerEmailAndFileName(String ownerEmail, String fileName);
    
    // Checks if a file exists for a specific user
    boolean existsByOwnerEmailAndFileName(String ownerEmail, String fileName);
}