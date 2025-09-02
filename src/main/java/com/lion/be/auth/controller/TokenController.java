package com.lion.be.auth.controller;

import com.lion.be.auth.controller.dto.AuthToken;
import com.lion.be.auth.controller.dto.AuthTokenResponse;
import com.lion.be.auth.service.AuthCodeService;
import com.lion.be.auth.service.RefreshTokenService;
import com.lion.be.global.util.CookieUtil;
import com.lion.be.global.util.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TokenController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthCodeService authCodeService;
    private final CookieUtil cookieUtil;

    @Value("${jwt.refresh-token-expire-time}")
    private long refreshTokenExpireTime;

    @Value("${cookie.domain}")
    private String cookieDomain;

    @PostMapping("/api/auth/token")
    public ResponseEntity<?> exchangeCodeForToken(@RequestBody Map<String, String> payload,
                                                  HttpServletResponse response) {
        String code = payload.get("code");
        if (code == null) {
            return ResponseEntity.badRequest().body("인증 코드가 없습니다.");
        }

        Optional<AuthToken> tokenOptional = authCodeService.getToken(code);
        if (tokenOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("틀리거나 만료된 토큰입니다.");
        }
        AuthToken tokens = tokenOptional.get();

        int cookieMaxAge = (int) (refreshTokenExpireTime / 1000);
        cookieUtil.addCookie(response, "refresh_token", tokens.getRefreshToken(), cookieMaxAge, cookieDomain);

        return ResponseEntity.ok(new AuthTokenResponse(tokens.getAccessToken()));
    }

    @PostMapping("/api/auth/refresh")
    public ResponseEntity<?> refreshAccessToken(HttpServletRequest request) {
        String refreshToken = cookieUtil.getCookie(request, "refresh_token")
                .map(Cookie::getValue)
                .orElse(null);

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("잘못된 refresh token입니다.");
        }

        // Redis에 저장된 토큰과 맞는지 재확인
        String email = jwtTokenProvider.getEmailFromToken(refreshToken);
        String savedToken = refreshTokenService.getToken(email);
        if (savedToken == null || !savedToken.equals(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("틀리거나 만료된 refreshToken입니다.");
        }

        Authentication authentication = jwtTokenProvider.getAuthentication(refreshToken);
        String newAccessToken = jwtTokenProvider.generateAccessToken(authentication);

        return ResponseEntity.ok(new AuthTokenResponse(newAccessToken));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Optional<Cookie> refreshTokenCookie = cookieUtil.getCookie(request, "refresh_token");

        if (refreshTokenCookie.isPresent()) {
            String refreshToken = refreshTokenCookie.get().getValue();
            if (jwtTokenProvider.validateToken(refreshToken)) {
                String email = jwtTokenProvider.getEmailFromToken(refreshToken);
                refreshTokenService.deleteToken(email);
            }
        }

        cookieUtil.deleteCookie(response, "refresh_token", cookieDomain);

        return ResponseEntity.noContent().build();
    }

}
