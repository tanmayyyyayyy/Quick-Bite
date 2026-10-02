package com.quickbite.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.ConflictException;
import com.quickbite.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void registersCustomerWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(false);
        when(passwordEncoder.encode("a-strong-password")).thenReturn("bcrypt-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = userService.register(" Person@Example.com ", "a-strong-password", " Alex ", " Doe ", null);

        assertThat(user.getEmail()).isEqualTo("person@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("bcrypt-hash");
        assertThat(user.getPasswordHash()).doesNotContain("a-strong-password");
        assertThat(user.getRole()).isEqualTo(UserRole.CUSTOMER);
        assertThat(user.getFirstName()).isEqualTo("Alex");
        verify(passwordEncoder).encode("a-strong-password");
    }

    @Test
    void rejectsDuplicateEmailBeforeHashingPassword() {
        when(userRepository.existsByEmailIgnoreCase("person@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.register("person@example.com", "a-strong-password", "A", "B", null))
                .isInstanceOf(ConflictException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void promotesUserToRestaurantOwnerWithoutAllowingAdminDowngrade() {
        User customer = org.mockito.Mockito.mock(User.class);
        when(userRepository.findById(7L)).thenReturn(java.util.Optional.of(customer));
        when(customer.getRole()).thenReturn(UserRole.CUSTOMER);

        userService.setRestaurantOwnerAccess(7L, true);

        verify(customer).setRole(UserRole.RESTAURANT_OWNER);
    }
}
