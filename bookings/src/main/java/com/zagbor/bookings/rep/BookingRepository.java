package com.zagbor.bookings.rep;


import com.zagbor.bookings.model.Booking;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByRequestId(String requestId);
    List<Booking> findByUserId(Long userId);


}


