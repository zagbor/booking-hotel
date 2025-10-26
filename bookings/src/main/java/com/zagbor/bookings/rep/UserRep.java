package com.zagbor.bookings.rep;

import com.zagbor.bookings.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRep extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
