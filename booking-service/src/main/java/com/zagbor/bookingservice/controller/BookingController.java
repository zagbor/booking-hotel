package com.zagbor.bookingservice.controller;

import com.zagbor.bookingservice.model.Booking;
import com.zagbor.bookingservice.service.BookingService;
import com.zagbor.bookingservice.service.BookingService.RoomView;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/bookings")
@SecurityRequirement(name = "bearer-jwt")
public class BookingController {
    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping
    public Booking create(@AuthenticationPrincipal Jwt jwt, @RequestBody Map<String, String> req) {
        Long userId = Long.parseLong(jwt.getSubject());
        Long roomId = Long.valueOf(req.get("roomId"));
        LocalDate start = LocalDate.parse(req.get("startDate"));
        LocalDate end = LocalDate.parse(req.get("endDate"));
        String requestId = req.get("requestId");
        return bookingService.createBooking(userId, roomId, start, end, requestId);
    }

    @GetMapping
    public List<Booking> myBookings(@AuthenticationPrincipal Jwt jwt) {
        Long userId = Long.parseLong(jwt.getSubject());
        return bookingService.findByUserId(userId);
    }

    @GetMapping("/suggestions")
    public Mono<List<RoomView>> suggestions() {
        return bookingService.getRoomSuggestions();
    }

    @GetMapping("/all")
    public ResponseEntity<List<Booking>> all(@AuthenticationPrincipal Jwt jwt) {
        String scope = jwt.getClaimAsString("scope");
        if ("ADMIN".equals(scope)) {
            return ResponseEntity.ok(bookingService.findAll());
        }
        return ResponseEntity.status(403).build();
    }
}


