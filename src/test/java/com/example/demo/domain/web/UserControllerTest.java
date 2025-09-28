package com.example.demo.domain.web;

import com.example.demo.domain.dto.request.UserRegisterRequest;
import com.example.demo.domain.dto.response.UserRegisterResponse;
import com.example.demo.domain.fixture.UserFixture;
import com.example.demo.domain.service.UserRegisterService;
import com.example.demo.domain.service.UserSearchService;
import com.example.demo.exceptions.errorcode.UserErrorCode;
import com.example.demo.exceptions.exception.UserException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRegisterService userRegisterService;

    @MockitoBean
    private UserSearchService userSearchService;

    @Test
    @DisplayName("회원가입 성공 - 모든 필드 입력")
    void registerSuccess_AllFields() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createDefaultRegisterRequest();
        UserRegisterResponse response = UserFixture.createDefaultRegisterResponse();

        when(userRegisterService.register(any(UserRegisterRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
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
    @DisplayName("회원가입 성공 - 필수 필드만 입력")
    void registerSuccess_OnlyRequiredFields() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createMinimalRegisterRequest();
        UserRegisterResponse response = UserFixture.createMinimalRegisterResponse();

        when(userRegisterService.register(any(UserRegisterRequest.class))).thenReturn(response);

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
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

    @Test
    @DisplayName("회원가입 실패 - 이메일 누락")
    void registerFail_EmailMissing() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createRequestWithoutEmail();

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(userRegisterService, never()).register(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 누락")
    void registerFail_PasswordMissing() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createRequestWithoutPassword();

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(userRegisterService, never()).register(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 잘못된 이메일 형식")
    void registerFail_InvalidEmailFormat() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createRequestWithInvalidEmail();

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(userRegisterService, never()).register(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 비밀번호 길이 부족")
    void registerFail_PasswordTooShort() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createRequestWithShortPassword();

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(userRegisterService, never()).register(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 중복된 이메일")
    void registerFail_DuplicateEmail() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createRequestWithDuplicateEmail();

        when(userRegisterService.register(any(UserRegisterRequest.class)))
            .thenThrow(new UserException(UserErrorCode.DUPLICATE_EMAIL));

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("U_0003"))
            .andExpect(jsonPath("$.errorMessage").value("이미 존재하는 이메일입니다"));

        verify(userRegisterService, times(1)).register(any(UserRegisterRequest.class));
    }

    @Test
    @DisplayName("회원가입 실패 - 빈 이메일")
    void registerFail_EmptyEmail() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createRequestWithEmptyEmail();

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(userRegisterService, never()).register(any());
    }

    @Test
    @DisplayName("회원가입 실패 - 빈 비밀번호")
    void registerFail_EmptyPassword() throws Exception {
        // given
        UserRegisterRequest request = UserFixture.createRequestWithEmptyPassword();

        // when & then
        mockMvc.perform(post("/api/v1/users/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(userRegisterService, never()).register(any());
    }

    @Test
    @DisplayName("이메일 존재 확인 - 존재하는 이메일")
    void checkEmailExists_ExistingEmail() throws Exception {
        // given
        String email = "existing@example.com";
        when(userSearchService.existsByEmail(email)).thenReturn(true);

        // when & then
        mockMvc.perform(get("/api/v1/users/exists")
                .param("email", email))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exists").value(true));

        verify(userSearchService, times(1)).existsByEmail(email);
    }

    @Test
    @DisplayName("이메일 존재 확인 - 존재하지 않는 이메일")
    void checkEmailExists_NonExistingEmail() throws Exception {
        // given
        String email = "nonexisting@example.com";
        when(userSearchService.existsByEmail(email)).thenReturn(false);

        // when & then
        mockMvc.perform(get("/api/v1/users/exists")
                .param("email", email))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.exists").value(false));

        verify(userSearchService, times(1)).existsByEmail(email);
    }

    @Test
    @DisplayName("이메일 존재 확인 실패 - 이메일 파라미터 누락")
    void checkEmailExists_MissingEmailParameter() throws Exception {
        // when & then
        mockMvc.perform(get("/api/v1/users/exists"))
            .andDo(print())
            .andExpect(status().isBadRequest());

        verify(userSearchService, never()).existsByEmail(any());
    }
}
