package com.zagbor.bookings.service;

import com.zagbor.bookings.model.User;
import com.zagbor.bookings.rep.BookingRepository;
import com.zagbor.bookings.rep.UserRep;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final UserRep repo;
    private final SecretKey signerKey;

    public AuthenticationService(
            UserRep repo,
            @Value("${security.jwt.secret}") String rawSecret
    ) {
        this.repo = repo;
        this.signerKey = deriveKey(rawSecret);
    }
    public String authenticate(String username, String password) {
        User entity = repo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!BCrypt.checkpw(password, entity.getPasswordHash())) {
            throw new IllegalArgumentException("Bad credentials");
        }

        Instant now = Instant.now();

        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", entity.getRole());
        claims.put("username", entity.getUsername());

        return Jwts.builder()
                .setSubject(entity.getId().toString())
                .addClaims(claims)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(3600)))
                .signWith(signerKey)
                .compact();
    }

    static SecretKey deriveKey(String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        // jjwt рекомендует минимум 256 бит для HMAC-SHA
        if (bytes.length < 32) {
            bytes = Arrays.copyOf(bytes, 32);
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
