package com.mbi.ticketingreservation.auth.api;

public record TokenPairResponse(
        String tokenType,
        String accessToken,
        long accessTokenExpiresIn,
        String refreshToken,
        long refreshTokenExpiresIn
) {
}
