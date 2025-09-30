package com.example.demo.domain.web.dto;

public class DeleteMediaResponse {
    private final boolean success;
    private final int deletedSightings;
    private final int deletedMedia;
    private final String message;

    public DeleteMediaResponse(boolean success, int deletedSightings, int deletedMedia, String message) {
        this.success = success;
        this.deletedSightings = deletedSightings;
        this.deletedMedia = deletedMedia;
        this.message = message;
    }

    public boolean isSuccess() { return success; }
    public int getDeletedSightings() { return deletedSightings; }
    public int getDeletedMedia() { return deletedMedia; }
    public String getMessage() { return message; }

    /** 기본 메시지 자동 생성 버전 (기존 코드 호환) */
    public static DeleteMediaResponse of(int sCount, int mCount) {
        String msg = String.format("미디어 삭제 완료 (미디어 %d건 삭제)", mCount);
        return new DeleteMediaResponse(true, sCount, mCount, msg);
    }

    /** 커스텀 메시지 지정 버전 */
    public static DeleteMediaResponse ofWithMessage(int sCount, int mCount, String message) {
        return new DeleteMediaResponse(true, sCount, mCount, message);
    }
}
