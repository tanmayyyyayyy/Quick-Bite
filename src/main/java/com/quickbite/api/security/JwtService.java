package com.quickbite.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration accessTokenTtl;

    public JwtService(
            @Value("${quickbite.security.jwt.secret:}") String encodedSecret,
            @Value("${quickbite.security.jwt.access-token-ttl-ms:900000}") long accessTokenTtlMillis) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET must be set to a Base64-encoded key of at least 256 bits");
        }
        byte[] secretBytes;
        try {
            secretBytes = Decoders.BASE64.decode(encodedSecret);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET must be valid Base64", exception);
        }
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must decode to at least 32 bytes");
        }
        if (accessTokenTtlMillis < 1) {
            throw new IllegalStateException("JWT access-token lifetime must be positive");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenTtl = Duration.ofMillis(accessTokenTtlMillis);
    }

    public String generateAccessToken(UserDetails userDetails) {
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(userDetails.getUsername())
            .claim("iat", issuedAt.getEpochSecond())
            .claim("exp", issuedAt.plus(accessTokenTtl).getEpochSecond())
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return claims(token).getSubject();
    }

    public boolean isValid(String token, UserDetails userDetails) {
        Claims tokenClaims = claims(token);
        return tokenClaims.getSubject().equals(userDetails.getUsername())
            && tokenClaims.getExpiration().toInstant().isAfter(Instant.now());
    }

    public long getAccessTokenTtlSeconds() {
        return accessTokenTtl.toSeconds();
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
