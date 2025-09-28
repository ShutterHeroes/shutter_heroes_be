package com.example.demo.config.security.jwt;

import com.example.demo.domain.enums.JwtRule;
import com.example.demo.exceptions.errorcode.UserErrorCode;
import com.example.demo.exceptions.exception.UserException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

import static java.util.Objects.isNull;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = getAccessToken(request);

        // 인증 정보가 없는 경우
        if (isNull(accessToken)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 인증 정보가 있는 경우
        setAuthenticationToContext(accessToken);

        filterChain.doFilter(request, response);
    }

    /**
     * 요청 헤더에 있는 쿠키 중 accessToken 값을 반환하는 메서드
     * 
     * @param request 요청 정보
     * @return accessToken 값
     */
    private String getAccessToken(HttpServletRequest request) {
        if (isNull(request.getCookies())) {
            return null;
        }
        return Arrays.stream(request.getCookies())
            .filter(cookie -> JwtRule.ACCESS_PREFIX.getValue().equals(cookie.getName()))
            .findFirst()
            .map(Cookie::getValue)
            .orElse(null);
    }

    /**
     * accessToken을 토대로 유저 정보를 조회하고, Context에 인증 정보를 저장하는 메서드
     *
     * @param accessToken accessToken 값
     */
    private void setAuthenticationToContext(String accessToken) {
        String username = jwtUtil.extractUsername(accessToken);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        // 계정 상태 검증
        if (!userDetails.isEnabled()) {
            throw new UserException(UserErrorCode.DEACTIVATED_USER);
        }

        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
