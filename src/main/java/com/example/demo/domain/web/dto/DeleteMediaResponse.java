package com.example.demo.domain.web.dto;

public class DeleteMediaResponse {
    private final boolean success;
    private final int deletedSightings;
    private final int deletedMedia;

    public DeleteMediaResponse(boolean success, int deletedSightings, int deletedMedia) {
        this.success = success;
        this.deletedSightings = deletedSightings;
        this.deletedMedia = deletedMedia;
    }

    public boolean isSuccess() { return success; }
    public int getDeletedSightings() { return deletedSightings; }
    public int getDeletedMedia() { return deletedMedia; }

    public static DeleteMediaResponse of(int sCount, int mCount) {
        return new DeleteMediaResponse(true, sCount, mCount);
    }
}
