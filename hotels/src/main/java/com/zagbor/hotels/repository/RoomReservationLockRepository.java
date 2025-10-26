package com.zagbor.hotels.repository;


import com.zagbor.hotels.model.ReservationLock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RoomReservationLockRepository extends JpaRepository<ReservationLock, Long> {
    Optional<ReservationLock> findByRequestId(String requestId);
    List<ReservationLock> findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long roomId,
            List<ReservationLock.Status> statuses,
            LocalDate endInclusive,
            LocalDate startInclusive
    );
}


