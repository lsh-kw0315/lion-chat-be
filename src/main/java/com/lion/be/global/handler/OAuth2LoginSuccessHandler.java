package com.lion.be.global.handler;

import com.lion.be.auth.controller.dto.AuthToken;
import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.auth.service.AuthCodeService;
import com.lion.be.auth.service.RefreshTokenService;
import com.lion.be.global.util.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthCodeService authCodeService;

    @Value("${url.base}")
    private String url;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String email = userPrincipal.getEmail();
        boolean isNewUser = userPrincipal.isNewUser();

        String accessToken = jwtTokenProvider.generateAccessToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        refreshTokenService.saveToken(email, refreshToken);

        AuthToken authToken = new AuthToken(accessToken, refreshToken);
        String authorizationCode = authCodeService.generateTokens(authToken);

        String redirectUrl = determineTargetUrl(authorizationCode);

        if (response.isCommitted()) {
            return;
        }

        clearAuthenticationAttributes(request);
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    protected String determineTargetUrl(String code) {
        String targetUrl = url + "/callback";

        return UriComponentsBuilder.fromUriString(targetUrl)
                .queryParam("code", code)
                .build().toUriString();
    }

}