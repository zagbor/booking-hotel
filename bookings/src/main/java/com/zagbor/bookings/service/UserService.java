package com.zagbor.bookings.service;

import com.zagbor.bookings.model.User;

import com.zagbor.bookings.rep.BookingRepository;
import com.zagbor.bookings.rep.UserRep;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

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

}
