package com.zagbor.bookingservice.service;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import com.zagbor.bookingservice.model.User;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.zagbor.bookingservice.rep.UserRep;

@Service

public class UserService {

    private final UserRep userRepository;

    public UserService(
            UserRep userRepository,
            @Value("${security.jwt.secret}") String rawSecret
    ) {
        this.userRepository = userRepository;
    }

    public User createUser(String username, String password, boolean isAdmin) {
        User entity = new User();
        entity.setUsername(username);
        entity.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
        entity.setRole(isAdmin ? "ADMIN" : "USER");
        return userRepository.save(entity);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }

    public User save(User user) {
      return userRepository.save(user);
    }

    public Optional<User> findByName(String name){
        return userRepository.findByUsername(name);
    }

}
