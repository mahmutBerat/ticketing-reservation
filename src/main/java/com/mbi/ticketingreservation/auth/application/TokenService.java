package com.mbi.ticketingreservation.auth.application;

import com.mbi.ticketingreservation.auth.api.AccessTokenResponse;
import com.mbi.ticketingreservation.auth.api.TokenPairResponse;
import com.mbi.ticketingreservation.auth.domain.Role;
import com.mbi.ticketingreservation.auth.domain.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class TokenService {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder refreshJwtDecoder;
    private final JwtProperties properties;
    private final Clock clock;

    public TokenService(
            JwtEncoder jwtEncoder,
            @Qualifier("refreshJwtDecoder") JwtDecoder refreshJwtDecoder,
            JwtProperties properties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.refreshJwtDecoder = refreshJwtDecoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenPairResponse createTokenPair(User user) {
        return new TokenPairResponse(
                BEARER_TOKEN_TYPE,
                encode(user, ACCESS_TOKEN_TYPE, properties.accessTokenTtl()),
                properties.accessTokenTtl().toSeconds(),
                encode(user, REFRESH_TOKEN_TYPE, properties.refreshTokenTtl()),
                properties.refreshTokenTtl().toSeconds());
    }

    public AccessTokenResponse createAccessToken(User user) {
        return new AccessTokenResponse(
                BEARER_TOKEN_TYPE,
                encode(user, ACCESS_TOKEN_TYPE, properties.accessTokenTtl()),
                properties.accessTokenTtl().toSeconds());
    }

    public Long readRefreshTokenSubject(String token) {
        try {
            Jwt jwt = refreshJwtDecoder.decode(token);
            if (!REFRESH_TOKEN_TYPE.equals(jwt.getClaimAsString(TOKEN_TYPE_CLAIM))) {
                throw new InvalidRefreshTokenException();
            }
            return Long.valueOf(Objects.requireNonNull(jwt.getSubject()));
        } catch (JwtException | NumberFormatException exception) {
            throw new InvalidRefreshTokenException();
        }
    }

    private String encode(User user, String tokenType, Duration timeToLive) {
        Instant issuedAt = Instant.now(clock);
        List<String> roles = user.getRoles().stream().map(Role::name).sorted().toList();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(timeToLive))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }
}
