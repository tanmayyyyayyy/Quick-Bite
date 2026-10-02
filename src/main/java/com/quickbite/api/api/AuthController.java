package com.quickbite.api.api;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.api.dto.AuthTokenResponse;
import com.quickbite.api.api.dto.LoginRequest;
import com.quickbite.api.api.dto.RegisterRequest;
import com.quickbite.api.api.dto.UserResponse;
import com.quickbite.api.security.JwtService;
import com.quickbite.api.service.UserService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final ApiMapper apiMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthController(UserService userService, ApiMapper apiMapper,
            AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userService = userService;
        this.apiMapper = apiMapper;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = apiMapper.toResponse(userService.register(request.email(), request.password(),
                request.firstName(), request.lastName(), request.phone()));
        return ResponseEntity.created(URI.create("/api/users/me")).body(response);
    }

    @PostMapping("/login")
    public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
        UserDetails userDetails = (UserDetails) authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())).getPrincipal();
        return new AuthTokenResponse(jwtService.generateAccessToken(userDetails), "Bearer",
                jwtService.getAccessTokenTtlSeconds(), apiMapper.toResponse(userService.getByEmail(userDetails.getUsername())));
    }
}
