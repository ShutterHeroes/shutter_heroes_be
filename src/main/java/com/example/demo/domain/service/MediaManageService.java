package com.example.demo.domain.service;

import com.example.demo.domain.repository.MediaRepository;
import com.example.demo.domain.web.dto.DeleteMediaResponse;
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

    /** visibility 변경: 본인 또는 관리자 */
    @Transactional
    public int changeVisibility(UUID mediaId, UUID actorId, boolean admin, String visibilityRaw) {
        String visibility = normalizeVisibility(visibilityRaw);

        if (!admin) {
            // 본인 소유 확인
            if (!isOwner(mediaId, actorId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this media");
            }
            return mediaRepository.updateSightingVisibilityForOwner(mediaId, actorId, visibility);
        } else {
            // 관리자: 전체 sighting 일괄 변경
            return mediaRepository.updateSightingVisibilityAsAdmin(mediaId, visibility);
        }
    }

    /** 삭제: 본인 또는 관리자 (sightings → media 순서) */
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
            // 이미 지워졌거나 없음
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found");
        }
        return DeleteMediaResponse.of(sDel, mDel);
    }
}
