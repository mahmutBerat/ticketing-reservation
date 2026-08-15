package com.mbi.ticketingreservation.auth.application;

import com.mbi.ticketingreservation.auth.api.*;
import com.mbi.ticketingreservation.auth.domain.Role;
import com.mbi.ticketingreservation.auth.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2030-01-01T12:00:00Z");
    private static final String ORGANIZER_EMAIL = "organizerUser@example.com";
    private static final String NORMALIZED_ORGANIZER_EMAIL = "organizeruser@example.com";

    @Spy
    private final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void registersCustomerWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest(" New.Customer@Example.COM ", "StrongPassword123!");
        User user = createCustomerUser("new.customer@example.com", "encoded-password");
        UserResponse expectedResponse = new UserResponse(
                1000L,
                "new.customer@example.com",
                Set.of(Role.CUSTOMER),
                NOW,
                null);
        when(userService.existsByEmail("new.customer@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userMapper.toCustomer(request, "encoded-password")).thenReturn(user);
        when(userService.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(expectedResponse);

        UserResponse response = authService.register(request);

        assertSame(expectedResponse, response);
        verify(userService).existsByEmail("new.customer@example.com");
        verify(passwordEncoder).encode(request.password());
        verify(userService).save(user);
    }

    @Test
    void rejectsAlreadyRegisteredEmailBeforeEncodingPassword() {
        RegisterRequest request = new RegisterRequest("Existing@Example.com", "StrongPassword123!");
        when(userService.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyRegisteredException.class, () -> authService.register(request));

        verifyNoInteractions(passwordEncoder, userMapper, tokenService);
        verify(userService, never()).save(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void translatesConcurrentDuplicateRegistrationToDomainError() {
        RegisterRequest request = new RegisterRequest("customerUser@example.com", "StrongPassword123!");
        User user = createCustomerUser("customerUser@example.com", "encoded-password");
        when(userService.existsByEmail("customeruser@example.com")).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(userMapper.toCustomer(request, "encoded-password")).thenReturn(user);
        when(userService.save(user)).thenThrow(new DataIntegrityViolationException("duplicate email"));

        assertThrows(EmailAlreadyRegisteredException.class, () -> authService.register(request));

        verify(userMapper, never()).toResponse(user);
    }

    @Test
    void logsInAndRecordsLoginTime() {
        LoginRequest request = new LoginRequest(" Organizer@Example.com ", "correct-password");
        User user = createOrganizerUser();
        TokenPairResponse expectedResponse = new TokenPairResponse("Bearer", "access", 900, "refresh", 604800);
        when(userService.findByEmail("organizer@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(true);
        when(tokenService.createTokenPair(user)).thenReturn(expectedResponse);

        TokenPairResponse response = authService.login(request);

        assertSame(expectedResponse, response);
        assertEquals(NOW, user.getLastLoginAt());
        verify(tokenService).createTokenPair(user);
    }

    @Test
    void rejectsLoginWhenEmailDoesNotExist() {
        LoginRequest request = new LoginRequest("missing@example.com", "password");
        when(userService.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        verifyNoInteractions(passwordEncoder, tokenService);
    }

    @Test
    void rejectsLoginWhenPasswordDoesNotMatch() {
        LoginRequest request = new LoginRequest(ORGANIZER_EMAIL, "wrong-password");
        User user = createOrganizerUser();
        when(userService.findByEmail(NORMALIZED_ORGANIZER_EMAIL)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.password(), user.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () -> authService.login(request));

        assertNull(user.getLastLoginAt());
        verifyNoInteractions(tokenService);
    }

    @Test
    void refreshesAccessTokenForExistingUser() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        User user = createCustomerUser("customerUser@example.com", "password-hash");
        AccessTokenResponse expectedResponse = new AccessTokenResponse("Bearer", "new-access-token", 900);
        when(tokenService.readRefreshTokenSubject(request.refreshToken())).thenReturn(3L);
        when(userService.findById(3L)).thenReturn(Optional.of(user));
        when(tokenService.createAccessToken(user)).thenReturn(expectedResponse);

        AccessTokenResponse response = authService.refresh(request);

        assertSame(expectedResponse, response);
    }

    @Test
    void rejectsRefreshWhenTokenUserNoLongerExists() {
        RefreshTokenRequest request = new RefreshTokenRequest("refresh-token");
        when(tokenService.readRefreshTokenSubject(request.refreshToken())).thenReturn(99L);
        when(userService.findById(99L)).thenReturn(Optional.empty());

        assertThrows(InvalidRefreshTokenException.class, () -> authService.refresh(request));

        verify(tokenService, never()).createAccessToken(org.mockito.ArgumentMatchers.any(User.class));
    }

    @Test
    void returnsAllUsersWithTheirIds() {
        User admin = new User("admin@example.com", "password-hash", Set.of(Role.ADMIN));
        User customer = createCustomerUser("customer@example.com", "password-hash");
        UserResponse adminResponse = new UserResponse(1L, "admin@example.com", Set.of(Role.ADMIN), NOW, null);
        UserResponse customerResponse = new UserResponse(
                2L, "customer@example.com", Set.of(Role.CUSTOMER), NOW, null);
        when(userService.findAll()).thenReturn(List.of(admin, customer));
        when(userMapper.toResponse(admin)).thenReturn(adminResponse);
        when(userMapper.toResponse(customer)).thenReturn(customerResponse);

        List<UserResponse> response = authService.getAllUsers();

        assertEquals(List.of(adminResponse, customerResponse), response);
        assertEquals(List.of(1L, 2L), response.stream().map(UserResponse::id).toList());
    }

    private User createOrganizerUser() {
        return new User(ORGANIZER_EMAIL, "password-hash", Set.of(Role.ORGANIZER));
    }

    private User createCustomerUser(String email, String passwordHash) {
        return new User(email, passwordHash, Set.of(Role.CUSTOMER));
    }
}
