package com.urielt.my_final_proj.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.urielt.my_final_proj.datamodels.User;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    public List<User> findByUsernameLike(String name);
    public User findByUsername(String name);
    public User findOneByUsernameAndPassword(String un, String pw);
    public User findOneByEmailAndPassword(String email, String password);
    public User findByEmail(String email);


    /*
        SELECT * FROM DemoDB 
        WHERE username=un AND password=pw
    */

}
