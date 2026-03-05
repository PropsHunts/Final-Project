package com.ftp_proj.project_ftp_v1.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.ftp_proj.project_ftp_v1.datamodels.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    public List<User> findByUsernameLike(String name);
    public List<User> findByUsername(String name);
    public User findOneByUsernameAndPassword(String un, String pw);
    

    /*
        SELECT * FROM DemoDB 
        WHERE username=un AND password=pw
    */

}
