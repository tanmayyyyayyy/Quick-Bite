package com.quickbite.api.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickbite.api.api.dto.ApiMapper;
import com.quickbite.api.config.OpenApiConfiguration;
import com.quickbite.api.entity.User;
import com.quickbite.api.exception.GlobalExceptionHandler;
import com.quickbite.api.security.JwtService;
import com.quickbite.api.service.UserService;
import java.util.List;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.webmvc.core.configuration.SpringDocWebMvcConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ApiMapper.class, GlobalExceptionHandler.class, OpenApiConfiguration.class,
    SpringDocConfiguration.class, SpringDocWebMvcConfiguration.class})
@EnableConfigurationProperties(org.springdoc.core.properties.SpringDocConfigProperties.class)
class AuthControllerWebTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;
    @MockitoBean
    private AuthenticationManager authenticationManager;
    @MockitoBean
    private JwtService jwtService;

    @Test
    void swaggerOpenApiDocumentLoadsWithJwtSchemeAndAuthRoutes() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("QuickBite API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.components.schemas.ApiErrorResponse.properties.timestamp.format").value("date-time"))
                .andExpect(jsonPath("$.paths['/api/auth/register'].post").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/register'].post.responses['409']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post").exists());
    }

    @Test
    void registrationReturnsCreatedWithSafeDto() throws Exception {
        User user = new User();
        user.setEmail("alex@example.com");
        user.setFirstName("Alex");
        user.setLastName("Doe");
        user.setPasswordHash("not-returned");
        when(userService.register(eq("alex@example.com"), eq("valid-password-123"), eq("Alex"), eq("Doe"), any()))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alex@example.com","password":"valid-password-123",
                                 "firstName":"Alex","lastName":"Doe"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("alex@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void invalidRegistrationReturnsStructuredValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bad","password":"short","firstName":"","lastName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.path").value("/api/auth/register"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

            @Test
            void loginReturnsBearerAccessToken() throws Exception {
            UserDetails principal = org.springframework.security.core.userdetails.User.withUsername("alex@example.com")
                .password("not-used")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
            when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
            when(jwtService.generateAccessToken(principal)).thenReturn("signed-access-token");
            when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
            User user = new User();
            user.setEmail("alex@example.com");
            user.setFirstName("Alex");
            user.setLastName("Doe");
            when(userService.getByEmail("alex@example.com")).thenReturn(user);

            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"alex@example.com\",\"password\":\"valid-password-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("signed-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.user.email").value("alex@example.com"));
            }

    @Test
    void unknownPathReturnsStructuredNotFound() throws Exception {
        mockMvc.perform(get("/api/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/does-not-exist"));
    }
}
