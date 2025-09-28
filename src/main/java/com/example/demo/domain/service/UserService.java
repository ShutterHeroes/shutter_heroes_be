package com.example.demo.domain.service;

import com.example.demo.config.security.jwt.JwtUtil;
import com.example.demo.domain.dto.request.UserLoginRequest;
import com.example.demo.domain.dto.request.UserUpdateRequest;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.JwtRule;
import com.example.demo.domain.enums.JwtType;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.exceptions.errorcode.AuthErrorCode;
import com.example.demo.exceptions.exception.AuthException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class UserService {

    private final UserSearchService userSearchService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Value("${jwt.access-token.expiration:3600000}")
    private long accessTokenExpiration;

    @Transactional
    public User login(UserLoginRequest request, HttpServletResponse response) {
        // 이메일로 사용자 찾기
        User user = userSearchService.findByEmailOrOptional(request.getEmail())
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        // JWT 토큰 생성
        String code = UUID.randomUUID().toString();
        String accessToken = jwtUtil.generateJwt(user.getEmail(), code, JwtType.ACCESS_TOKEN);
        String refreshToken = jwtUtil.generateJwt(user.getEmail(), code, JwtType.REFRESH_TOKEN);

        // 쿠키에 토큰 설정
        addTokenCookie(response, JwtRule.ACCESS_PREFIX.getValue(), accessToken, (int)(accessTokenExpiration / 1000));
        addTokenCookie(response, JwtRule.REFRESH_PREFIX.getValue(), refreshToken, (int)(accessTokenExpiration * 30 / 1000));

        log.info("User login successful: email={}", user.getEmail());

        return user;
    }

    @Transactional
    public User updateUser(String email, UserUpdateRequest request) {
        // 이메일로 사용자 찾기
        User user = userSearchService.findByEmail(email);
        user.update(request);

        // 변경사항 저장
        User updatedUser = userRepository.save(user);

        log.info("User profile updated successfully: email={}", email);

        return updatedUser;
    }

    private void addTokenCookie(HttpServletResponse response, String name, String value, int maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }
}
