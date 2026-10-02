package com.quickbite.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {
    private JwtService jwtService;
    private UserDetails customer;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]);
        jwtService = new JwtService(secret, 60_000);
        customer = User.withUsername("customer@example.com")
                .password("not-used-by-token-service")
                .roles("CUSTOMER")
                .build();
    }

    @Test
    void signsTokenForSubjectAndAcceptsMatchingUser() {
        String token = jwtService.generateAccessToken(customer);

        assertThat(jwtService.extractUsername(token)).isEqualTo("customer@example.com");
        assertThat(jwtService.isValid(token, customer)).isTrue();
        assertThat(jwtService.getAccessTokenTtlSeconds()).isEqualTo(60);
    }

    @Test
    void rejectsTokenForDifferentUser() {
        String token = jwtService.generateAccessToken(customer);
        UserDetails otherUser = User.withUsername("other@example.com")
                .password("not-used-by-token-service")
                .roles("CUSTOMER")
                .build();

        assertThat(jwtService.isValid(token, otherUser)).isFalse();
    }

    @Test
    void rejectsSigningKeysShorterThan256Bits() {
        String weakSecret = Base64.getEncoder().encodeToString(new byte[16]);

        assertThatThrownBy(() -> new JwtService(weakSecret, 60_000))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32 bytes");
    }
}
