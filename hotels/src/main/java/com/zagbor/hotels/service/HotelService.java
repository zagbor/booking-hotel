package com.zagbor.hotels.service;


import com.zagbor.hotels.model.Hotel;
import com.zagbor.hotels.model.Room;
import com.zagbor.hotels.model.ReservationLock;
import com.zagbor.hotels.repository.HotelRepository;
import com.zagbor.hotels.repository.RoomRepository;
import com.zagbor.hotels.repository.RoomReservationLockRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class HotelService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationLockRepository lockRepository;

    public HotelService(HotelRepository hotelRepository, RoomRepository roomRepository, RoomReservationLockRepository lockRepository) {
        this.hotelRepository = hotelRepository;
        this.roomRepository = roomRepository;
        this.lockRepository = lockRepository;
    }

    // CRUD-операции
    public List<Hotel> listHotels() { return hotelRepository.findAll(); }
    public Optional<Hotel> getHotel(Long id) { return hotelRepository.findById(id); }
    public Hotel saveHotel(Hotel h) { return hotelRepository.save(h); }
    public void deleteHotel(Long id) { hotelRepository.deleteById(id); }

    public List<Room> listRooms() { return roomRepository.findAll(); }
    public Optional<Room> getRoom(Long id) { return roomRepository.findById(id); }
    public Room saveRoom(Room r) { return roomRepository.save(r); }
    public void deleteRoom(Long id) { roomRepository.deleteById(id); }

    // Доступность: удержание/подтверждение/освобождение с идемпотентностью по requestId
    @Transactional
    public ReservationLock holdRoom(String requestId, Long roomId, LocalDate startDate, LocalDate endDate) {
        Optional<ReservationLock> existing = lockRepository.findByRequestId(requestId);
        if (existing.isPresent()) {
            return existing.get();
        }
        // Проверка конфликтующих удержаний или подтверждений
        List<ReservationLock> conflicts = lockRepository
                .findByRoomIdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        roomId,
                        Arrays.asList(ReservationLock.Status.HELD, ReservationLock.Status.CONFIRMED),
                        endDate,
                        startDate
                );
        if (!conflicts.isEmpty()) {
            throw new IllegalStateException("Номер недоступен на указанные даты");
        }
        ReservationLock lock = new ReservationLock();
        lock.setRequestId(requestId);
        lock.setRoomId(roomId);
        lock.setStartDate(startDate);
        lock.setEndDate(endDate);
        lock.setStatus(ReservationLock.Status.HELD);
        return lockRepository.save(lock);
    }

    @Transactional
    public ReservationLock confirmHold(String requestId) {
        ReservationLock lock = lockRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Hold not found"));
        if (lock.getStatus() == ReservationLock.Status.CONFIRMED) {
            return lock; // идемпотентность
        }
        if (lock.getStatus() == ReservationLock.Status.RELEASED) {
            throw new IllegalStateException("Удержание уже снято");
        }
        lock.setStatus(ReservationLock.Status.CONFIRMED);
        // Увеличиваем счётчик бронирований для статистики
        roomRepository.findById(lock.getRoomId()).ifPresent(room -> {
            room.setTimesBooked(room.getTimesBooked() + 1);
            roomRepository.save(room);
        });
        return lockRepository.save(lock);
    }

    @Transactional
    public ReservationLock releaseHold(String requestId) {
        ReservationLock lock = lockRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Hold not found"));
        if (lock.getStatus() == ReservationLock.Status.RELEASED) {
            return lock; // идемпотентность
        }
        if (lock.getStatus() == ReservationLock.Status.CONFIRMED) {
            return lock; // уже подтверждено; ничего не делаем для идемпотентности
        }
        lock.setStatus(ReservationLock.Status.RELEASED);
        return lockRepository.save(lock);
    }
}


