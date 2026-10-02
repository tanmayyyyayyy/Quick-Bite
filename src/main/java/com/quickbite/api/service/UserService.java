package com.quickbite.api.service;

import com.quickbite.api.entity.User;
import com.quickbite.api.entity.UserRole;
import com.quickbite.api.exception.BadRequestException;
import com.quickbite.api.exception.ConflictException;
import com.quickbite.api.exception.ResourceNotFoundException;
import com.quickbite.api.repository.UserRepository;
import com.quickbite.api.service.command.ProfileUpdateCommand;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User register(String email, String rawPassword, String firstName, String lastName, String phone) {
        if (rawPassword == null || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadRequestException("Password must not exceed 72 UTF-8 bytes");
        }
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists");
        }
        if (phone != null && !phone.isBlank() && userRepository.existsByPhoneIgnoreCase(phone.trim())) {
            throw new ConflictException("An account with this phone number already exists");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPhone(phone == null || phone.isBlank() ? null : phone.trim());
        user.setRole(UserRole.CUSTOMER);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return requireUser(id);
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", email));
    }

    @Transactional
    public User updateProfile(Long userId, ProfileUpdateCommand command) {
        User user = requireUser(userId);
        String phone = command.phone() == null || command.phone().isBlank() ? null : command.phone().trim();
        if (phone != null && !phone.equalsIgnoreCase(user.getPhone())
                && userRepository.existsByPhoneIgnoreCaseAndIdNot(phone, userId)) {
            throw new ConflictException("An account with this phone number already exists");
        }
        user.setFirstName(command.firstName().trim());
        user.setLastName(command.lastName().trim());
        user.setPhone(phone);
        return user;
    }

    @Transactional
    public User setRestaurantOwnerAccess(Long userId, boolean enabled) {
        User user = requireUser(userId);
        if (user.getRole() == UserRole.ADMIN) {
            throw new ConflictException("Administrator accounts cannot be changed through this operation");
        }
        user.setRole(enabled ? UserRole.RESTAURANT_OWNER : UserRole.CUSTOMER);
        return user;
    }

    @Transactional(readOnly = true)
    public Page<User> searchUsers(String search, Pageable pageable) {
        String term = search == null || search.isBlank() ? "" : search.trim();
        return userRepository.findByEmailContainingIgnoreCaseOrLastNameContainingIgnoreCase(term, term, pageable);
    }
}