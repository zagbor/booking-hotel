package com.zagbor.hotels.repository;


import com.zagbor.hotels.model.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}


