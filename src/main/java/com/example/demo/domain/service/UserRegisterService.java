package com.example.demo.domain.service;

import com.example.demo.domain.dto.request.UserRegisterRequest;
import com.example.demo.domain.dto.response.UserRegisterResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.LoginPlatform;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.exceptions.exception.UserException;
import com.example.demo.exceptions.errorcode.UserErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class UserRegisterService {
    private final UserRepository userRepository;
    private final UserSearchService userSearchService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User toEntityAndSave(String email, String displayName, LoginPlatform loginPlatform) {
        User user = User.ofNewRegistration(email, displayName, loginPlatform);
        return userRepository.save(user);
    }

    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {
        // 이메일 중복 확인
        if (userSearchService.existsByEmail(request.getEmail())) {
            throw new UserException(UserErrorCode.DUPLICATE_EMAIL);
        }

        // User 엔티티의 toEntity 메서드 호출, User 생성
        User user = User.toEntity(passwordEncoder, request);

        // 저장
        User savedUser = userRepository.save(user);

        // 응답 반환
        return UserRegisterResponse.from(savedUser);
    }
}
