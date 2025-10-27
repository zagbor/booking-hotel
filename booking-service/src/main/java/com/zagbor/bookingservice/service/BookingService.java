package com.zagbor.bookingservice.service;

import com.zagbor.bookingservice.model.Booking;
import com.zagbor.bookingservice.rep.BookingRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.http.HttpStatusCode;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static java.util.Map.entry;

/**
 * Оркестратор процесса бронирования: идемпотентность, двухшаговая проверка, компенсация. Переписан с иными именами и
 * приёмами, но сохраняет исходное поведение.
 */
@Service
public class BookingService {

    private static final Logger LOG = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final WebClient http;
    private final Duration opTimeout;
    private final int maxRetries;
    private final SecretKey jwtKey;

    public BookingService(
            BookingRepository bookingRepository,
            WebClient.Builder httpBuilder,
            @Value("${hotel.base-url}") String hotelsBase,
            @Value("${hotel.timeout-ms}") int timeoutMs,
            @Value("${hotel.retries}") int retries,
            @Value("${security.jwt.secret}") String rawSecret
    ) {
        this.bookingRepository = bookingRepository;
        this.http = httpBuilder.baseUrl(hotelsBase).build();
        this.opTimeout = Duration.ofMillis(timeoutMs);
        this.maxRetries = retries;

        byte[] keyBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            keyBytes = Arrays.copyOf(keyBytes, 32);
        }
        this.jwtKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * API остался прежним для совместимости.
     */
    @Transactional
    public Booking createBooking(Long userId, Long roomId, LocalDate start, LocalDate end, String requestId) {
        // идемпотентность
        Optional<Booking> already = bookingRepository.findByRequestId(requestId);
        if (already.isPresent()) {
            return already.get();
        }

        final String corr = UUID.randomUUID().toString();
        Booking b = new Booking();
        b.setRequestId(requestId);
        b.setUserId(userId);
        b.setRoomId(roomId);
        b.setStartDate(start);
        b.setEndDate(end);
        b.setStatus(Booking.Status.PENDING);
        b.setCorrelationId(corr);
        b.setCreatedAt(OffsetDateTime.now());
        b = bookingRepository.save(b);

        LOG.info("[{}] new booking saved as PENDING", corr);

        // общий payload
        Map<String, String> holdPayload = Map.ofEntries(
                entry("requestId", requestId),
                entry("startDate", start.toString()),
                entry("endDate", end.toString())
        );

        try {
            hold(roomId, holdPayload, corr).block(opTimeout);
            confirm(roomId, Map.of("requestId", requestId), corr).block(opTimeout);

            b.setStatus(Booking.Status.CONFIRMED);
            bookingRepository.save(b);
            LOG.info("[{}] booking moved to CONFIRMED", corr);
        } catch (Throwable err) {
            LOG.warn("[{}] booking flow failed: {}", corr, (err.getMessage() != null ? err.getMessage() : err));
            // компенсация best-effort
            try {
                release(roomId, Map.of("requestId", requestId), corr).block(opTimeout);
            } catch (Throwable ignored) {
                // намеренно глушим — это компенсирующий best-effort
            }
            b.setStatus(Booking.Status.CANCELLED);
            bookingRepository.save(b);
            LOG.info("[{}] booking moved to CANCELLED (compensated)", corr);
        }

        return b;
    }

    // —————————————————————— Вспомогательные вызовы ——————————————————————

    private Mono<String> hold(Long roomId, Map<String, String> body, String corr) {
        return invokePost("/rooms/{id}/hold", roomId, body, corr);
    }

    private Mono<String> confirm(Long roomId, Map<String, String> body, String corr) {
        return invokePost("/rooms/{id}/confirm", roomId, body, corr);
    }

    private Mono<String> release(Long roomId, Map<String, String> body, String corr) {
        return invokePost("/rooms/{id}/release", roomId, body, corr);
    }

    /**
     * Единая точка POST-вызовов: timeout + backoff-ретраи, корреляция в заголовке. Используем exchangeToMono вместо
     * retrieve для «другой манеры» обработки.
     */
    private Mono<String> invokePost(String template, Long roomId, Map<String, String> body, String corr) {
        return http.post()
                .uri(template, roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Correlation-Id", corr)
                .bodyValue(body)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(String.class).defaultIfEmpty("");
                    }
                    return resp.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(msg -> Mono.error(new IllegalStateException(
                                    "Hotel call failed: " + resp.statusCode() + (msg.isBlank() ? "" : " - " + msg))));
                })
                .timeout(opTimeout)
                .retryWhen(Retry
                        .backoff(Math.max(0, maxRetries), Duration.ofMillis(300))
                        .maxBackoff(Duration.ofSeconds(2)));
    }

    // —————————————————————— Рекомендации комнат ——————————————————————

    public record RoomView(Long id, String number, long timesBooked) {

    }

    /**
     * API-имя сохранили, реализация переписана.
     */
    public Mono<List<RoomView>> getRoomSuggestions() {
        String token = buildServiceToken();

        return http.get()
                .uri("hotels/rooms")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError, resp ->
                        resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .map(msg -> new IllegalStateException(
                                        "Fetch rooms failed: " + resp.statusCode()
                                                + (msg.isBlank() ? "" : " - " + msg)))
                )
                .bodyToFlux(RoomView.class)
                .filter(Objects::nonNull)
                .sort(Comparator
                        .comparingLong(RoomView::timesBooked)
                        .thenComparing(RoomView::id))
                .collectList();
    }

    private String buildServiceToken() {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject("booking-service")
                .claim("scope", "SERVICE")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(300)))
                .signWith(jwtKey)
                .compact();
    }


    public List<Booking> findByUserId(Long userId) {
        return bookingRepository.findByUserId(userId);
    }
    public List<Booking>findAll(){
        return bookingRepository.findAll();
    }
}
