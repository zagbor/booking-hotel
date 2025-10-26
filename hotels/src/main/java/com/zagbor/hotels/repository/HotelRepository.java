package com.zagbor.hotels.repository;


import com.zagbor.hotels.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HotelRepository extends JpaRepository<Hotel, Long> {
}


