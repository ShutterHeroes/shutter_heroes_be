package com.example.demo.domain.web;

import com.example.demo.domain.dto.request.UserRegisterRequest;
import com.example.demo.domain.dto.request.UserLoginRequest;
import com.example.demo.domain.dto.response.UserRegisterResponse;
import com.example.demo.domain.dto.response.UserLoginResponse;
import com.example.demo.domain.dto.response.UserEmailExistsResponse;
import com.example.demo.domain.dto.response.UserProfileResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.service.UserRegisterService;
import com.example.demo.domain.service.UserService;
import com.example.demo.domain.service.UserSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
@Tag(name = "User", description = "사용자 관련 API")
public class UserController {

    private final UserRegisterService userRegisterService;
    private final UserSearchService userSearchService;
    private final UserService userService;

    @PostMapping("/register")
    @Operation(summary = "회원가입", description = "이메일과 비밀번호를 사용한 회원가입")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "회원가입 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
        @ApiResponse(responseCode = "409", description = "이미 존재하는 이메일")
    })
    public ResponseEntity<UserRegisterResponse> register(
        @Valid @RequestBody UserRegisterRequest request
    ) {
        UserRegisterResponse response = userRegisterService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호를 사용한 로그인 (JWT 토큰은 쿠키로 전달)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (이메일 또는 비밀번호 불일치)")
    })
    public ResponseEntity<UserLoginResponse> login(
        @Valid @RequestBody UserLoginRequest request,
        HttpServletResponse httpResponse
    ) {
        User user = userService.login(request, httpResponse);
        UserLoginResponse response = UserLoginResponse.from(user);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/exists")
    @Operation(summary = "이메일 존재 확인", description = "해당 이메일로 가입된 사용자가 있는지 확인")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "이메일 존재 여부 확인 성공"),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 파라미터 누락)")
    })
    public ResponseEntity<UserEmailExistsResponse> checkEmailExists(
        @RequestParam String email
    ) {
        boolean exists = userSearchService.existsByEmail(email);
        UserEmailExistsResponse response = UserEmailExistsResponse.of(exists);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "나의 정보 조회", description = "로그인한 사용자의 프로필 정보 조회 (JWT 토큰 필요)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
        @ApiResponse(responseCode = "401", description = "인증 실패 (JWT 토큰 없음 또는 유효하지 않음)")
    })
    public ResponseEntity<UserProfileResponse> getMyProfile(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        String email = userDetails.getUsername();
        User user = userSearchService.findByEmail(email);
        UserProfileResponse response = UserProfileResponse.from(user);
        return ResponseEntity.ok(response);
    }
}
