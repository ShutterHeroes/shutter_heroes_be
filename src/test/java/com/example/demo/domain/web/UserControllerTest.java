package com.example.demo.domain.web;

import com.example.demo.domain.dto.request.UserRegisterRequest;
import com.example.demo.domain.dto.request.UserLoginRequest;
import com.example.demo.domain.dto.request.UserUpdateRequest;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @DisplayName("로그아웃 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class LogoutTests {

        private final String LOGOUT_URL = "/api/v1/users/logout";

        @BeforeEach
        void setUp() {
            reset(userService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공 - 로그아웃 처리")
        @WithMockUser(username = "test@example.com", roles = "USER")
        void success_Logout() throws Exception {
            // given
            doNothing().when(userService).logout(any());

            // when & then
            mockMvc.perform(post(LOGOUT_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Successfully logged out"))
                .andExpect(jsonPath("$.success").value(true));

            verify(userService, times(1)).logout(any());
        }

        // ========== 실패 케이스 - 인증 오류 ==========
        @Test
        @Order(2)
        @DisplayName("실패 - 인증되지 않은 사용자")
        void fail_Unauthenticated() throws Exception {
            // when & then
            mockMvc.perform(post(LOGOUT_URL)
                    .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isUnauthorized());

            verify(userService, never()).logout(any());
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

    @Nested
    @DisplayName("사용자 목록 조회 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UserListTests {

        private final String USER_LIST_URL = "/api/v1/users";

        @BeforeEach
        void setUp() {
            reset(userSearchService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공 - 기본 페이지네이션으로 사용자 목록 조회")
        void success_GetUserListWithDefaultPagination() throws Exception {
            // given
            List<User> users = Arrays.asList(
                UserFixture.createDefaultUser(),
                UserFixture.createDefaultUser(),
                UserFixture.createDefaultUser()
            );
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 3);
            when(userSearchService.findAllUsers(any(Pageable.class), any())).thenReturn(userPage);

            // when & then
            mockMvc.perform(get(USER_LIST_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(3))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false))
                .andExpect(jsonPath("$.users[0].displayName").value("테스트유저"))
                .andExpect(jsonPath("$.users[0].role").value("user"))
                .andExpect(jsonPath("$.users[0].id").exists())
                .andExpect(jsonPath("$.users[0].createdAt").exists())
                .andExpect(jsonPath("$.users[0].email").doesNotExist())
                .andExpect(jsonPath("$.users[0].lastLoginAt").doesNotExist());

            verify(userSearchService, times(1)).findAllUsers(any(Pageable.class), any());
        }

        @Test
        @Order(2)
        @DisplayName("성공 - 커스텀 페이지네이션으로 사용자 목록 조회")
        void success_GetUserListWithCustomPagination() throws Exception {
            // given
            List<User> users = Arrays.asList(
                UserFixture.createDefaultUser(),
                UserFixture.createDefaultUser()
            );
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(1, 2, Sort.by(Sort.Direction.ASC, "displayName")), 5);
            when(userSearchService.findAllUsers(any(Pageable.class), any())).thenReturn(userPage);

            // when & then
            mockMvc.perform(get(USER_LIST_URL)
                    .param("page", "1")
                    .param("size", "2")
                    .param("sortBy", "displayName")
                    .param("sortDirection", "asc"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(1))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.hasNext").value(true))
                .andExpect(jsonPath("$.hasPrevious").value(true));

            verify(userSearchService, times(1)).findAllUsers(any(Pageable.class), any());
        }

        @Test
        @Order(3)
        @DisplayName("성공 - 빈 목록 조회")
        void success_GetEmptyUserList() throws Exception {
            // given
            Page<User> emptyPage = new PageImpl<>(Arrays.asList(), PageRequest.of(0, 10), 0);
            when(userSearchService.findAllUsers(any(Pageable.class), any())).thenReturn(emptyPage);

            // when & then
            mockMvc.perform(get(USER_LIST_URL))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(0))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));

            verify(userSearchService, times(1)).findAllUsers(any(Pageable.class), any());
        }

        // ========== 실패 케이스 - 잘못된 파라미터 ==========
        @Test
        @Order(4)
        @DisplayName("성공 - 잘못된 페이지 번호 (음수)는 0으로 처리")
        void success_HandleNegativePageNumber() throws Exception {
            // given
            List<User> users = Arrays.asList(UserFixture.createDefaultUser());
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
            when(userSearchService.findAllUsers(any(Pageable.class), any())).thenReturn(userPage);

            // when & then
            mockMvc.perform(get(USER_LIST_URL)
                    .param("page", "-1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPage").value(0));

            verify(userSearchService, times(1)).findAllUsers(any(Pageable.class), any());
        }

        @Test
        @Order(5)
        @DisplayName("성공 - 검색어로 사용자 목록 조회")
        void success_GetUserListWithSearch() throws Exception {
            // given
            User user1 = UserFixture.createUserWithEmailAndDisplayName("john@example.com", "John Doe");
            User user2 = UserFixture.createUserWithEmailAndDisplayName("jane@example.com", "Jane Smith");
            List<User> searchResults = Arrays.asList(user1, user2);
            Page<User> userPage = new PageImpl<>(searchResults, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 2);
            when(userSearchService.findAllUsers(any(Pageable.class), eq("john"))).thenReturn(userPage);

            // when & then
            mockMvc.perform(get(USER_LIST_URL)
                    .param("search", "john"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(2))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.totalElements").value(2));

            verify(userSearchService, times(1)).findAllUsers(any(Pageable.class), eq("john"));
        }

        @Test
        @Order(6)
        @DisplayName("성공 - 빈 검색어로 전체 사용자 목록 조회")
        void success_GetUserListWithEmptySearch() throws Exception {
            // given
            List<User> users = Arrays.asList(UserFixture.createDefaultUser());
            Page<User> userPage = new PageImpl<>(users, PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")), 1);
            when(userSearchService.findAllUsers(any(Pageable.class), eq(""))).thenReturn(userPage);

            // when & then
            mockMvc.perform(get(USER_LIST_URL)
                    .param("search", ""))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users.length()").value(1));

            verify(userSearchService, times(1)).findAllUsers(any(Pageable.class), eq(""));
        }
    }

    @Nested
    @DisplayName("특정 사용자 프로필 조회 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UserProfileTests {

        private final String USER_PROFILE_URL = "/api/v1/users";

        @BeforeEach
        void setUp() {
            reset(userSearchService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공 - 특정 사용자 프로필 조회")
        void success_GetUserProfile() throws Exception {
            // given
            UUID userId = UUID.randomUUID();
            User user = UserFixture.createUserWithEmailAndDisplayName("john@example.com", "John Doe");
            when(userSearchService.findById(userId)).thenReturn(user);

            // when & then
            mockMvc.perform(get(USER_PROFILE_URL + "/{userId}", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.displayName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("user"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.lastLoginAt").doesNotExist());

            verify(userSearchService, times(1)).findById(userId);
        }

        // ========== 실패 케이스 ==========
        @Test
        @Order(2)
        @DisplayName("실패 - 존재하지 않는 사용자 ID로 조회")
        void fail_GetUserProfileWithNonExistentUserId() throws Exception {
            // given
            UUID nonExistentUserId = UUID.randomUUID();
            when(userSearchService.findById(nonExistentUserId))
                .thenThrow(new UserException(UserErrorCode.NOT_EXIST));

            // when & then
            mockMvc.perform(get(USER_PROFILE_URL + "/{userId}", nonExistentUserId))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U_0001"))
                .andExpect(jsonPath("$.errorMessage").value("존재하지 않는 유저입니다"));

            verify(userSearchService, times(1)).findById(nonExistentUserId);
        }

        @Test
        @Order(3)
        @DisplayName("실패 - 잘못된 UUID 형식으로 조회")
        void fail_GetUserProfileWithInvalidUUID() throws Exception {
            // when & then
            mockMvc.perform(get(USER_PROFILE_URL + "/{userId}", "invalid-uuid"))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userSearchService, never()).findById(any(UUID.class));
        }
    }

    @Nested
    @DisplayName("나의 정보 수정 API")
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UserUpdateTests {

        private final String UPDATE_PROFILE_URL = "/api/v1/users/me";

        @BeforeEach
        void setUp() {
            reset(userSearchService, userService);
        }

        // ========== 성공 케이스 ==========
        @Test
        @Order(1)
        @DisplayName("성공 - 프로필 정보 전체 수정")
        @WithMockUser(username = "test@example.com")
        void success_UpdateAllProfileFields() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("새로운이름")
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

            User updatedUser = UserFixture.createDefaultUser();
            updatedUser.update(request);

            when(userService.updateUser(eq("test@example.com"), any(UserUpdateRequest.class))).thenReturn(updatedUser);

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("새로운이름"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.jpg"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.updatedAt").exists());

            verify(userService, times(1)).updateUser(eq("test@example.com"), any(UserUpdateRequest.class));
        }

        @Test
        @Order(2)
        @DisplayName("성공 - displayName만 수정")
        @WithMockUser(username = "test@example.com")
        void success_UpdateDisplayNameOnly() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("새로운이름")
                .build();

            User updatedUser = UserFixture.createDefaultUser();
            updatedUser.update(request);

            when(userService.updateUser(eq("test@example.com"), any(UserUpdateRequest.class))).thenReturn(updatedUser);

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("새로운이름"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"));

            verify(userService, times(1)).updateUser(eq("test@example.com"), any(UserUpdateRequest.class));
        }

        @Test
        @Order(3)
        @DisplayName("성공 - avatarUrl만 수정")
        @WithMockUser(username = "test@example.com")
        void success_UpdateAvatarUrlOnly() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                .avatarUrl("https://example.com/new-avatar.jpg")
                .build();

            User updatedUser = UserFixture.createDefaultUser();
            updatedUser.update(request);

            when(userService.updateUser(eq("test@example.com"), any(UserUpdateRequest.class))).thenReturn(updatedUser);

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("테스트유저"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/new-avatar.jpg"));

            verify(userService, times(1)).updateUser(eq("test@example.com"), any(UserUpdateRequest.class));
        }

        @Test
        @Order(4)
        @DisplayName("성공 - avatarUrl을 null로 설정하여 제거")
        @WithMockUser(username = "test@example.com")
        void success_RemoveAvatarUrl() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                .avatarUrl(null)
                .build();

            User updatedUser = UserFixture.createDefaultUser();
            updatedUser.update(request);

            when(userService.updateUser(eq("test@example.com"), any(UserUpdateRequest.class))).thenReturn(updatedUser);

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk());

            verify(userService, times(1)).updateUser(eq("test@example.com"), any(UserUpdateRequest.class));
        }

        // ========== 실패 케이스 - 유효성 검증 ==========
        @Test
        @Order(5)
        @DisplayName("실패 - displayName이 너무 긴 경우")
        @WithMockUser(username = "test@example.com")
        void fail_DisplayNameTooLong() throws Exception {
            // given
            String longDisplayName = "a".repeat(101); // 101자
            UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName(longDisplayName)
                .build();

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @Order(6)
        @DisplayName("실패 - avatarUrl이 너무 긴 경우")
        @WithMockUser(username = "test@example.com")
        void fail_AvatarUrlTooLong() throws Exception {
            // given
            String longUrl = "https://example.com/" + "a".repeat(500); // 500자 이상
            UserUpdateRequest request = UserUpdateRequest.builder()
                .avatarUrl(longUrl)
                .build();

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @Order(7)
        @DisplayName("실패 - displayName이 빈 문자열인 경우")
        @WithMockUser(username = "test@example.com")
        void fail_EmptyDisplayName() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("")
                .build();

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isBadRequest());

            verify(userService, never()).updateUser(any(), any());
        }

        // ========== 실패 케이스 - 인증 오류 ==========
        @Test
        @Order(8)
        @DisplayName("실패 - 인증되지 않은 사용자")
        void fail_Unauthenticated() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("새로운이름")
                .build();

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isUnauthorized());

            verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @Order(9)
        @DisplayName("실패 - 존재하지 않는 사용자")
        @WithMockUser(username = "nonexistent@example.com")
        void fail_UserNotFound() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder()
                .displayName("새로운이름")
                .build();

            when(userService.updateUser(eq("nonexistent@example.com"), any(UserUpdateRequest.class)))
                .thenThrow(new UserException(UserErrorCode.NOT_EXIST));

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U_0001"))
                .andExpect(jsonPath("$.errorMessage").value("존재하지 않는 유저입니다"));

            verify(userService, times(1)).updateUser(eq("nonexistent@example.com"), any(UserUpdateRequest.class));
        }

        @Test
        @Order(10)
        @DisplayName("성공 - 빈 요청 바디로 호출 시 아무것도 수정하지 않음")
        @WithMockUser(username = "test@example.com")
        void success_EmptyRequestBody() throws Exception {
            // given
            UserUpdateRequest request = UserUpdateRequest.builder().build();

            User user = UserFixture.createDefaultUser();
            when(userService.updateUser(eq("test@example.com"), any(UserUpdateRequest.class))).thenReturn(user);

            // when & then
            mockMvc.perform(put(UPDATE_PROFILE_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("테스트유저"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.jpg"));

            verify(userService, times(1)).updateUser(eq("test@example.com"), any(UserUpdateRequest.class));
        }
    }
}
