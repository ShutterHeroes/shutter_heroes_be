package com.example.demo.config.security.oauth2.handler;

import com.example.demo.config.security.jwt.JwtUtil;
import com.example.demo.domain.enums.JwtRule;
import com.example.demo.domain.enums.JwtType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtUtil jwtUtil;

    @Value("${jwt.access-token.expiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;
    @Value("${spring.security.oauth2.uri.base}")
    private String redirectUrl;

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        final String provider = token.getAuthorizedClientRegistrationId();

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = "";

        switch (provider) {
            case "kakao" -> {
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                email = (String) kakaoAccount.get("email");
            }
            case "naver" -> {
                Map<String, Object> naverAccount = (Map<String, Object>) attributes.get("response");
                email = (String) naverAccount.get("email");
            }
            case "google" -> {
                email = (String) attributes.get("email");
            }
        }

        String code = UUID.randomUUID().toString();
        generateJwtAndSetCookie(response, email, code, JwtType.ACCESS_TOKEN);

        log.info("OAUTH2_SUCCESS", StructuredArguments.keyValue("request", "OAuth2SuccessHandler"));
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * 유저의 인증 토큰을 발급하고, 쿠키에 세팅하는 메서드
     *
     * @param response 응답 정보
     * @param username 유저의 이메일
     * @param code     토큰의 세트를 구분하기 위한 값 (하나의 refreshToken은 하나 이상의 accessToken과 연결)
     * @param jwtType  jwt 토큰 유형
     */
    private void generateJwtAndSetCookie(HttpServletResponse response, String username, String code, JwtType jwtType) {
        String jwt = jwtUtil.generateJwt(username, code, jwtType);
        ResponseCookie tokenCookie = setTokenToCookie(jwt, jwtType);
        response.addHeader(JwtRule.JWT_ISSUE_HEADER.getValue(), tokenCookie.toString());
    }

    /**
     * 유저의 jwt 토큰을 쿠키로 세팅하는 메서드
     *
     * @param jwt     jwt 토큰
     * @param jwtType jwt 토큰 유형
     * @return 쿠키
     */
    public ResponseCookie setTokenToCookie(String jwt, JwtType jwtType) {
        return ResponseCookie.from(
                jwtType.isAccessToken() ? JwtRule.ACCESS_PREFIX.getValue() : JwtRule.REFRESH_PREFIX.getValue(),
                jwt
            )
            .path("/")
            .maxAge((jwtType.isAccessToken() ? ACCESS_TOKEN_EXPIRATION_TIME : (ACCESS_TOKEN_EXPIRATION_TIME * 30)) /1000 )
            .httpOnly(true)
            .sameSite("None")
            .secure(true)
            .build();
    }
}
