package com.urielt.my_final_proj.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.urielt.my_final_proj.datamodels.FileDocument;

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