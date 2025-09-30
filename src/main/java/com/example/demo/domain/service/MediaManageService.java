package com.example.demo.domain.service;

import com.example.demo.domain.repository.MediaRepository;
import com.example.demo.domain.web.dto.DeleteMediaResponse;
import com.example.demo.domain.web.dto.UpdateVisibilityResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaManageService {

    private final MediaRepository mediaRepository;

    private boolean isOwner(UUID mediaId, UUID userId) {
        UUID owner = mediaRepository.findOwnerIdByMediaId(mediaId);
        return owner != null && owner.equals(userId);
    }

    private String normalizeVisibility(String v) {
        if (v == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visibility is required");
        String norm = v.trim().toLowerCase(Locale.ROOT);
        if (!norm.equals("public") && !norm.equals("private")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "visibility must be 'public' or 'private'");
        }
        return norm;
    }

    /** visibility 변경: 본인 또는 관리자 → 메시지 포함 응답 */
    @Transactional
    public UpdateVisibilityResponse changeVisibility(UUID mediaId, UUID actorId, boolean admin, String visibilityRaw) {
        String visibility = normalizeVisibility(visibilityRaw);

        int updated;
        if (!admin) {
            if (!isOwner(mediaId, actorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this media");
            }
            updated = mediaRepository.updateSightingVisibilityForOwner(mediaId, actorId, visibility);
        } else {
            updated = mediaRepository.updateSightingVisibilityAsAdmin(mediaId, visibility);
        }

        String msg = String.format("가시성을 '%s'(으)로 변경했습니다. 적용된 미디어: %d건", visibility, updated);
        return UpdateVisibilityResponse.of(updated, visibility, msg);
    }

    /** 삭제: 본인 또는 관리자 (sightings → media 순서) → 메시지 포함 응답 */
    @Transactional
    public DeleteMediaResponse deleteMedia(UUID mediaId, UUID actorId, boolean admin) {
        int sDel, mDel;

        if (!admin) {
            if (!isOwner(mediaId, actorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this media");
            }
            sDel = mediaRepository.deleteSightingsByMediaForOwner(mediaId, actorId);
            mDel = mediaRepository.deleteMediaByIdForOwner(mediaId, actorId);
        } else {
            sDel = mediaRepository.deleteSightingsByMediaAsAdmin(mediaId);
            mDel = mediaRepository.deleteMediaByIdAsAdmin(mediaId);
        }

        if (mDel == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        String msg = String.format("미디어(%s) 삭제 완료 (미디어 %d건 삭제)", mediaId, mDel);
        return DeleteMediaResponse.ofWithMessage(sDel, mDel, msg);
    }
}
