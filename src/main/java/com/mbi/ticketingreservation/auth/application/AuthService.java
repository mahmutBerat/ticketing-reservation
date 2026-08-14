package com.mbi.ticketingreservation.auth.application;

import com.mbi.ticketingreservation.auth.api.AccessTokenResponse;
import com.mbi.ticketingreservation.auth.api.LoginRequest;
import com.mbi.ticketingreservation.auth.api.RefreshTokenRequest;
import com.mbi.ticketingreservation.auth.api.RegisterRequest;
import com.mbi.ticketingreservation.auth.api.TokenPairResponse;
import com.mbi.ticketingreservation.auth.api.UpdateUserRolesRequest;
import com.mbi.ticketingreservation.auth.api.UserMapper;
import com.mbi.ticketingreservation.auth.api.UserResponse;
import com.mbi.ticketingreservation.auth.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final Clock clock;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userService.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = userMapper.toCustomer(request, passwordEncoder.encode(request.password()));
        try {
            return userMapper.toResponse(userService.save(user));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
        }
    }

    @Transactional
    public TokenPairResponse login(LoginRequest request) {
        User user = userService.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.recordLogin(Instant.now(clock));
        return tokenService.createTokenPair(user);
    }

    @Transactional(readOnly = true)
    public AccessTokenResponse refresh(RefreshTokenRequest request) {
        Long userId = tokenService.readRefreshTokenSubject(request.refreshToken());
        User user = userService.findById(userId).orElseThrow(InvalidRefreshTokenException::new);
        return tokenService.createAccessToken(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userService.findAll().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    @Transactional
    public UserResponse updateUserRoles(Long userId, UpdateUserRolesRequest request) {
        User user = userService.findById(userId).orElseThrow(UserNotFoundException::new);
        user.replaceRoles(request.roles());
        return userMapper.toResponse(user);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
