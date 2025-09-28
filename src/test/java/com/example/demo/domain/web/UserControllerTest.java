package com.example.demo.domain.web;

import com.example.demo.domain.dto.request.UserRegisterRequest;
import com.example.demo.domain.dto.request.UserLoginRequest;
import com.example.demo.domain.dto.response.UserRegisterResponse;
import com.example.demo.domain.entity.User;
import com.example.demo.domain.fixture.UserFixture;
import com.example.demo.domain.service.UserRegisterService;
import com.example.demo.domain.service.UserService;
import com.example.demo.domain.service.UserSearchService;
import com.example.demo.exceptions.errorcode.UserErrorCode;
import com.example.demo.exceptions.errorcode.AuthErrorCode;
import com.example.demo.exceptions.exception.UserException;
import com.example.demo.exceptions.exception.AuthException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@AutoConfigureMockMvc
@AutoConfigureRestDocs
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("UserController 테스트")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRegisterService userRegisterService;

    @MockitoBean
    private UserSearchService userSearchService;

    @MockitoBean
    private UserService userService;

    @Nested
    @DisplayName("회원가입 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class RegisterTests {

        private final String REGISTER_URL = "/api/v1/users/register";

        @BeforeEach
        void setUp() {
            reset(userRegisterService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공 - 모든 필드 입력")
        void success_AllFields() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createDefaultRegisterRequest();
            UserRegisterResponse response = UserFixture.createDefaultRegisterResponse();
            when(userRegisterService.register(any(UserRegisterRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("테스트유저"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

            verify(userRegisterService, times(1)).register(any(UserRegisterRequest.class));
        }

        @Test
        @Order(2)
        @DisplayName("성공 - 필수 필드만 입력")
        void success_OnlyRequiredFields() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createMinimalRegisterRequest();
            UserRegisterResponse response = UserFixture.createMinimalRegisterResponse();
            when(userRegisterService.register(any(UserRegisterRequest.class))).thenReturn(response);

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("test"))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.role").value("user"));

            verify(userRegisterService, times(1)).register(any(UserRegisterRequest.class));
        }

        // ========== 실패 케이스 - 필수 필드 누락 ==========
        @Test
        @Order(3)
        @DisplayName("실패 - 이메일 누락")
        void fail_EmailMissing() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createRequestWithoutEmail();

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userRegisterService, never()).register(any());
        }

        @Test
        @Order(4)
        @DisplayName("실패 - 비밀번호 누락")
        void fail_PasswordMissing() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createRequestWithoutPassword();

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userRegisterService, never()).register(any());
        }

        // ========== 실패 케이스 - 빈 값 ==========
        @Test
        @Order(5)
        @DisplayName("실패 - 빈 이메일")
        void fail_EmptyEmail() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createRequestWithEmptyEmail();

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userRegisterService, never()).register(any());
        }

        @Test
        @Order(6)
        @DisplayName("실패 - 빈 비밀번호")
        void fail_EmptyPassword() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createRequestWithEmptyPassword();

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userRegisterService, never()).register(any());
        }

        // ========== 실패 케이스 - 형식 오류 ==========
        @Test
        @Order(7)
        @DisplayName("실패 - 잘못된 이메일 형식")
        void fail_InvalidEmailFormat() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createRequestWithInvalidEmail();

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userRegisterService, never()).register(any());
        }

        @Test
        @Order(8)
        @DisplayName("실패 - 비밀번호 길이 부족")
        void fail_PasswordTooShort() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createRequestWithShortPassword();

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userRegisterService, never()).register(any());
        }

        // ========== 실패 케이스 - 비즈니스 로직 오류 ==========
        @Test
        @Order(9)
        @DisplayName("실패 - 중복된 이메일")
        void fail_DuplicateEmail() throws Exception {
            // given
            UserRegisterRequest request = UserFixture.createRequestWithDuplicateEmail();
            when(userRegisterService.register(any(UserRegisterRequest.class)))
                .thenThrow(new UserException(UserErrorCode.DUPLICATE_EMAIL));

            // when & then
            mockMvc.perform(post(REGISTER_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U_0003"))
                .andExpect(jsonPath("$.errorMessage").value("이미 존재하는 이메일입니다"));

            verify(userRegisterService, times(1)).register(any(UserRegisterRequest.class));
        }
    }

    @Nested
    @DisplayName("로그인 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class LoginTests {

        private final String LOGIN_URL = "/api/v1/users/login";

        @BeforeEach
        void setUp() {
            reset(userService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공")
        void success() throws Exception {
            // given
            UserLoginRequest request = UserFixture.createDefaultLoginRequest();
            User user = UserFixture.createDefaultUser();
            when(userService.login(any(UserLoginRequest.class), any())).thenReturn(user);

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("테스트유저"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());

            verify(userService, times(1)).login(any(UserLoginRequest.class), any());
        }

        // ========== 실패 케이스 - 필수 필드 누락 ==========
        @Test
        @Order(2)
        @DisplayName("실패 - 이메일 누락")
        void fail_EmailMissing() throws Exception {
            // given
            UserLoginRequest request = UserFixture.createLoginRequestWithoutEmail();

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userService, never()).login(any(), any());
        }

        @Test
        @Order(3)
        @DisplayName("실패 - 비밀번호 누락")
        void fail_PasswordMissing() throws Exception {
            // given
            UserLoginRequest request = UserFixture.createLoginRequestWithoutPassword();

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userService, never()).login(any(), any());
        }

        // ========== 실패 케이스 - 형식 오류 ==========
        @Test
        @Order(4)
        @DisplayName("실패 - 잘못된 이메일 형식")
        void fail_InvalidEmailFormat() throws Exception {
            // given
            UserLoginRequest request = UserFixture.createLoginRequest("invalid-email", "password123");

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userService, never()).login(any(), any());
        }

        // ========== 실패 케이스 - 인증 오류 ==========
        @Test
        @Order(5)
        @DisplayName("실패 - 잘못된 이메일 또는 비밀번호")
        void fail_InvalidCredentials() throws Exception {
            // given
            UserLoginRequest request = UserFixture.createLoginRequest("wrong@example.com", "wrongpassword");
            when(userService.login(any(UserLoginRequest.class), any()))
                .thenThrow(new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

            // when & then
            mockMvc.perform(post(LOGIN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_0002"))
                .andExpect(jsonPath("$.errorMessage").value("이메일 또는 비밀번호가 올바르지 않습니다"));

            verify(userService, times(1)).login(any(UserLoginRequest.class), any());
        }
    }

    @Nested
    @DisplayName("이메일 존재 확인 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class EmailExistsTests {

        private final String EMAIL_EXISTS_URL = "/api/v1/users/exists";

        @BeforeEach
        void setUp() {
            reset(userSearchService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공 - 존재하는 이메일")
        void success_ExistingEmail() throws Exception {
            // given
            String email = "existing@example.com";
            when(userSearchService.existsByEmail(email)).thenReturn(true);

            // when & then
            mockMvc.perform(get(EMAIL_EXISTS_URL)
                    .param("email", email))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));

            verify(userSearchService, times(1)).existsByEmail(email);
        }

        @Test
        @Order(2)
        @DisplayName("성공 - 존재하지 않는 이메일")
        void success_NonExistingEmail() throws Exception {
            // given
            String email = "nonexisting@example.com";
            when(userSearchService.existsByEmail(email)).thenReturn(false);

            // when & then
            mockMvc.perform(get(EMAIL_EXISTS_URL)
                    .param("email", email))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));

            verify(userSearchService, times(1)).existsByEmail(email);
        }

        // ========== 실패 케이스 - 필수 파라미터 누락 ==========
        @Test
        @Order(3)
        @DisplayName("실패 - 이메일 파라미터 누락")
        void fail_MissingEmailParameter() throws Exception {
            // when & then
            mockMvc.perform(get(EMAIL_EXISTS_URL))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userSearchService, never()).existsByEmail(any());
        }
    }

    @Nested
    @DisplayName("나의 정보 조회 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class MyProfileTests {

        private final String MY_PROFILE_URL = "/api/v1/users/me";

        @BeforeEach
        void setUp() {
            reset(userSearchService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공 - 인증된 사용자의 프로필 조회")
        @WithMockUser(username = "test@example.com")
        void success_GetMyProfile() throws Exception {
            // given
            User user = UserFixture.createDefaultUser();
            when(userSearchService.findByEmail("test@example.com")).thenReturn(user);

            // when & then
            mockMvc.perform(get(MY_PROFILE_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.displayName").value("테스트유저"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdAt").exists());

            verify(userSearchService, times(1)).findByEmail("test@example.com");
        }

        // ========== 실패 케이스 - 인증 오류 ==========
        @Test
        @Order(2)
        @DisplayName("실패 - 인증되지 않은 사용자")
        void fail_Unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(get(MY_PROFILE_URL))
                .andDo(print())
                .andExpect(status().isUnauthorized());

            verify(userSearchService, never()).findByEmail(any());
        }

        @Test
        @Order(3)
        @DisplayName("실패 - 존재하지 않는 사용자")
        @WithMockUser(username = "nonexistent@example.com")
        void fail_UserNotFound() throws Exception {
            // given
            when(userSearchService.findByEmail("nonexistent@example.com"))
                .thenThrow(new UserException(UserErrorCode.NOT_EXIST));

            // when & then
            mockMvc.perform(get(MY_PROFILE_URL))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U_0001"))
                .andExpect(jsonPath("$.errorMessage").value("존재하지 않는 유저입니다"));

            verify(userSearchService, times(1)).findByEmail("nonexistent@example.com");
        }
    }
}
