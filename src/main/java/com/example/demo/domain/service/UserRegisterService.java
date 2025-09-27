package com.example.demo.domain.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.enums.LoginPlatform;
import com.example.demo.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class UserRegisterService {
    private final UserRepository userRepository;

    @Transactional
    public User toEntityAndSave(String email, String displayName, LoginPlatform loginPlatform) {
        User user = User.ofNewRegistration(email, displayName, loginPlatform);
        return userRepository.save(user);
    }
}
