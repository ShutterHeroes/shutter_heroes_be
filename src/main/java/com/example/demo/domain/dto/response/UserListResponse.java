package com.example.demo.domain.dto.response;

import com.example.demo.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserListResponse {

    private List<UserPublicInfoResponse> users;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
    private boolean hasNext;
    private boolean hasPrevious;

    /**
     * Spring Data Page 객체로부터 UserListResponse 생성
     *
     * @param userPage Spring Data Page 객체
     * @return UserListResponse
     */
    public static UserListResponse from(Page<User> userPage) {
        // 공개 정보만 포함하도록 변환
        Page<UserPublicInfoResponse> infoResponsePage = userPage.map(UserPublicInfoResponse::from);

        return UserListResponse.builder()
            .users(infoResponsePage.getContent())
            .currentPage(infoResponsePage.getNumber())
            .totalPages(infoResponsePage.getTotalPages())
            .totalElements(infoResponsePage.getTotalElements())
            .size(infoResponsePage.getSize())
            .hasNext(infoResponsePage.hasNext())
            .hasPrevious(infoResponsePage.hasPrevious())
            .build();
    }
}
