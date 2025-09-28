package com.example.demo.domain.service;

import com.example.demo.domain.entity.User;
import com.example.demo.domain.repository.UserRepository;
import com.example.demo.exceptions.errorcode.UserErrorCode;
import com.example.demo.exceptions.exception.UserException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class UserSearchService {
    private final UserRepository userRepository;

    public Optional<User> findByEmailOrOptional(String email) {
        return userRepository.findByEmail(email);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new UserException(UserErrorCode.NOT_EXIST));
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * 페이지네이션을 적용하여 사용자 목록을 조회
     * 기본적으로 생성일자 기준 내림차순 정렬
     *
     * @param pageable 페이지네이션 정보 (페이지 번호, 크기, 정렬)
     * @return 페이지네이션이 적용된 사용자 목록
     */
    public Page<User> findAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * 검색어를 포함한 사용자 목록을 조회
     * email 또는 displayName에서 검색어를 포함하는 사용자들을 조회
     *
     * @param pageable 페이지네이션 정보 (페이지 번호, 크기, 정렬)
     * @param search 검색어 (email 또는 displayName에서 검색)
     * @return 페이지네이션이 적용된 사용자 목록
     */
    public Page<User> findAllUsers(Pageable pageable, String search) {
        return userRepository.findAllWithSearch(search, pageable);
    }

    /**
     * 사용자 ID로 사용자 조회
     *
     * @param userId 사용자 ID
     * @return 조회된 사용자
     * @throws UserException 사용자가 존재하지 않을 경우
     */
    public User findById(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new UserException(UserErrorCode.NOT_EXIST));
    }
}
