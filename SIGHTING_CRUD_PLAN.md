# Sighting CRUD API 구현 계획

## 📋 프로젝트 현황 분석

### 기존 구현된 기능
- ✅ **Sighting 생성** (`POST /api/v1/sightings`)
  - 이미지 업로드 및 Vision API 동물 인식
  - EXIF 메타데이터 추출 및 저장
  - S3 업로드 (원본 + EXIF 제거 버전)
  - Species 비동기 처리

- ✅ **Sighting 목록 조회** (`GET /api/v1/sightings/nearby`)
  - 반경 기반 검색 (좌표/중심 ID 기준)
  - Public/Private 권한 처리
  - 페이징 없음 (반경 기반 필터링만)

### 구현해야 할 기능
- ⬜ **Sighting 전체 목록 조회** (`GET /api/v1/sightings`)
- ⬜ **Sighting 상세 조회** (`GET /api/v1/sightings/{sightingId}`)
- ⬜ **Sighting 수정** (`PATCH /api/v1/sightings/{sightingId}`)
- ⬜ **Sighting 삭제** (`DELETE /api/v1/sightings/{sightingId}`)

---

## 🎯 구현 계획

### 1. Sighting 전체 목록 조회 API

#### 1.1 엔드포인트
```
GET /api/v1/sightings
```

#### 1.2 요구사항
- **권한 검증**
  - Public: 모든 사용자 조회 가능
  - Private: 소유자만 포함 (로그인 사용자의 private 데이터만 추가)
  - 비로그인 사용자: Public만 조회 가능

- **페이징**
  - Offset-based pagination
  - `page` (기본값: 0)
  - `size` (기본값: 20, 최대: 100)

- **정렬**
  - `sortBy` (기본값: `created_at`)
    - `created_at`: 생성 시간순
    - `occurred_at`: 목격 시간순
  - `sortOrder` (기본값: `desc`)
    - `asc`: 오름차순
    - `desc`: 내림차순

- **검색**
  - `keyword` (선택): 학명 또는 한국어 이름으로 검색
    - `species.scientific_name` (학명) ILIKE 검색
    - `species.common_name_ko` (한국어 이름) ILIKE 검색
  - 검색어가 없으면 전체 목록 반환

- **응답 데이터**
  - Sighting 기본 정보 (id, title, description, occurred_at, created_at)
  - Species 정보 (common_name_ko, common_name_en, scientific_name, status)
  - Media 썸네일 URL (sanitizedUrl)
  - User 정보 (display_name)
  - 페이징 메타데이터 (totalElements, totalPages, currentPage, size)

#### 1.3 구현 파일
- **Controller**: `SightingQueryController.java`
  - `GET /api/v1/sightings` 엔드포인트 추가

- **Service**: `SightingQueryService.java`
  - `findAllSightings(UUID viewerId, String keyword, Pageable pageable)` 메서드 추가

- **Repository**: `SightingRepository.java`
  - `findAllWithSearch(UUID viewerId, String keyword, Pageable pageable)` Native Query 추가
  - Species, Media, User JOIN
  - Public/Private 권한 필터링
  - ILIKE 검색 (PostgreSQL)

- **DTO**:
  - `SightingListResponse.java` (응답 DTO - Page 래퍼)
  - `SightingListItemDto.java` (목록 아이템 DTO)
  - `SightingListRow.java` (Repository Projection)

#### 1.4 쿼리 예시
```sql
-- keyword가 있을 때
SELECT s.*, sp.common_name_ko, sp.scientific_name, ...
FROM app.sightings s
LEFT JOIN app.species sp ON sp.id = s.species_id
LEFT JOIN app.media m ON m.id = s.media_id
JOIN app.users u ON u.id = s.user_id
WHERE (
    s.visibility = 'public'
    OR (:viewerId IS NOT NULL AND s.user_id = :viewerId AND s.visibility = 'private')
)
AND (
    :keyword IS NULL
    OR sp.scientific_name ILIKE '%' || :keyword || '%'
    OR sp.common_name_ko ILIKE '%' || :keyword || '%'
)
ORDER BY s.created_at DESC
LIMIT :size OFFSET :offset
```

---

### 2. Sighting 상세 조회 API

#### 2.1 엔드포인트
```
GET /api/v1/sightings/{sightingId}
```

#### 1.2 요구사항
- **권한 검증**
  - Public: 모든 사용자 조회 가능
  - Private: 소유자 또는 ADMIN만 조회 가능
  - 비로그인 사용자: Public만 조회 가능

- **응답 데이터**
  - Sighting 기본 정보 (title, description, occurred_at, etc.)
  - 연관 Media 정보 (sanitizedUrl, storage_path, EXIF 데이터)
  - 연관 Species 정보 (common_name_ko, common_name_en, scientific_name, status)
  - 연관 User 정보 (display_name)
  - AI Detection 정보 (label, confidence, scientific_name)
  - GPS 정보 (GeoJSON 형식)

#### 1.3 구현 파일
- **Controller**: `SightingQueryController.java`
  - `GET /api/v1/sightings/{sightingId}` 엔드포인트 추가

- **Service**: `SightingQueryService.java`
  - `findSightingDetail(UUID sightingId, UUID viewerId)` 메서드 추가

- **Repository**: `SightingRepository.java`
  - `findDetailById(UUID sightingId, UUID viewerId)` Native Query 추가
  - Species, Media, User, AiDetection JOIN
  - Public/Private 권한 필터링

- **DTO**:
  - `SightingDetailResponse.java` (응답 DTO)
  - `SightingDetailRow.java` (Repository Projection)

---

### 3. Sighting 수정 API

#### 3.1 엔드포인트
```
PATCH /api/v1/sightings/{sightingId}
```

#### 3.2 요구사항
- **권한 검증**
  - 소유자 또는 ADMIN만 수정 가능

- **수정 가능 필드**
  - `title` (제목)
  - `description` (설명)
  - `visibility` (PUBLIC/PRIVATE)
  - `occurred_at` (목격 시간)
  - `address_text` (주소 텍스트)
  - ~~`geom` (GPS 좌표)~~ → 보안상 수정 불가 (EXIF에서만 추출)
  - ~~`species_id`~~ → AI 감지 결과로만 결정 (수동 변경 불가)

- **검증**
  - Visibility: PUBLIC 또는 PRIVATE만 허용
  - occurred_at: 미래 시간 불가
  - title/description: 길이 제한 (optional)

#### 3.3 구현 파일
- **Controller**: `SightingController.java` (기존 파일)
  - `PATCH /api/v1/sightings/{sightingId}` 엔드포인트 추가

- **Service**: `SightingService.java` (기존 파일)
  - `updateSighting(UUID sightingId, UUID actorId, boolean isAdmin, SightingUpdateRequest request)` 메서드 추가

- **Repository**: `SightingRepository.java`
  - JPA `findById()` 사용
  - 권한 검증 로직은 Service에서 처리

- **DTO**:
  - `SightingUpdateRequest.java` (요청 DTO)
  - `SightingUpdateResponse.java` (응답 DTO)

---

### 4. Sighting 삭제 API

#### 4.1 엔드포인트
```
DELETE /api/v1/sightings/{sightingId}
```

#### 4.2 요구사항
- **권한 검증**
  - 소유자 또는 ADMIN만 삭제 가능

- **삭제 순서**
  1. Sighting 삭제
  2. ~~Media 삭제~~ → Media는 별도 API로 삭제 (`DELETE /api/v1/media/{mediaId}`)
  3. ~~Species 삭제~~ → Species는 다른 Sighting에서 참조 가능 (삭제 안 함)

- **Soft Delete vs Hard Delete**
  - **Hard Delete** 채택 (DB에서 완전 삭제)
  - Soft Delete는 추후 요구사항 발생 시 추가 (deleted_at 컬럼 필요)

#### 4.3 구현 파일
- **Controller**: `SightingController.java`
  - `DELETE /api/v1/sightings/{sightingId}` 엔드포인트 추가

- **Service**: `SightingService.java`
  - `deleteSighting(UUID sightingId, UUID actorId, boolean isAdmin)` 메서드 추가

- **Repository**: `SightingRepository.java`
  - JPA `deleteById()` 사용
  - 권한 검증 로직은 Service에서 처리

- **DTO**:
  - `SightingDeleteResponse.java` (응답 DTO)

---

## 📂 파일 구조

```
src/main/java/com/example/demo/domain/
├── web/
│   ├── SightingController.java          (수정, 삭제 추가)
│   ├── SightingQueryController.java     (목록 조회, 상세 조회 추가)
│   └── dto/
│       ├── SightingListResponse.java    (NEW - 페이징 래퍼)
│       ├── SightingListItemDto.java     (NEW - 목록 아이템)
│       ├── SightingDetailResponse.java  (NEW - 상세 조회)
│       ├── SightingUpdateRequest.java   (NEW - 수정 요청)
│       ├── SightingUpdateResponse.java  (NEW - 수정 응답)
│       └── SightingDeleteResponse.java  (NEW - 삭제 응답)
│
├── service/
│   ├── SightingService.java            (수정, 삭제 메서드 추가)
│   └── SightingQueryService.java       (목록 조회, 상세 조회 메서드 추가)
│
└── repository/
    ├── SightingRepository.java         (목록 조회, 상세 조회 Native Query 추가)
    └── projection/
        ├── SightingListRow.java        (NEW - 목록 Projection)
        └── SightingDetailRow.java      (NEW - 상세 Projection)
```

---

## 🔐 권한 검증 로직

### 공통 권한 체크 메서드 (Service 계층)
```java
private void validateOwnershipOrAdmin(UUID sightingId, UUID actorId, boolean isAdmin) {
    if (isAdmin) return;

    Sighting sighting = sightingRepository.findById(sightingId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Sighting not found"));

    if (!sighting.getUser().getId().equals(actorId)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not the owner of this sighting");
    }
}
```

---

## 🚀 구현 순서

### Phase 1: Sighting 전체 목록 조회
1. `SightingListRow.java` Projection 생성
2. `SightingListItemDto.java` DTO 생성
3. `SightingListResponse.java` 페이징 래퍼 DTO 생성
4. `SightingRepository.findAllWithSearch()` Native Query 구현
5. `SightingRepository.countAllWithSearch()` Count Query 구현
6. `SightingQueryService.findAllSightings()` 메서드 구현
7. `SightingQueryController` GET `/api/v1/sightings` 엔드포인트 추가
8. Swagger 문서화

### Phase 2: Sighting 상세 조회
1. `SightingDetailRow.java` Projection 생성
2. `SightingDetailResponse.java` DTO 생성
3. `SightingRepository.findDetailById()` Native Query 구현
4. `SightingQueryService.findSightingDetail()` 메서드 구현
5. `SightingQueryController` GET `/api/v1/sightings/{id}` 엔드포인트 추가
6. Swagger 문서화

### Phase 3: Sighting 수정
1. `SightingUpdateRequest.java` DTO 생성 (Validation 포함)
2. `SightingUpdateResponse.java` DTO 생성
3. `SightingService.updateSighting()` 메서드 구현
4. `SightingController` PATCH `/api/v1/sightings/{id}` 엔드포인트 추가
5. 권한 검증 로직 추가
6. Swagger 문서화

### Phase 4: Sighting 삭제
1. `SightingDeleteResponse.java` DTO 생성
2. `SightingService.deleteSighting()` 메서드 구현
3. `SightingController` DELETE `/api/v1/sightings/{id}` 엔드포인트 추가
4. 권한 검증 로직 추가
5. Swagger 문서화

---

## 📝 참고사항

### 기존 코드 패턴 준수
- **Media 관리**: `MediaManageController`, `MediaManageService` 참고
  - 권한 검증: `isOwner()`, `isAdmin()` 패턴
  - 응답 DTO에 메시지 포함

- **User 관리**: `UserController`, `UserService` 참고
  - `@AuthenticationPrincipal UserPrincipal` 사용
  - JWT 인증 기반

- **예외 처리**: `ResponseStatusException` 사용
  - `HttpStatus.NOT_FOUND`: 리소스 없음
  - `HttpStatus.FORBIDDEN`: 권한 없음
  - `HttpStatus.BAD_REQUEST`: 잘못된 요청

### 데이터베이스 스키마
- **Schema**: `app.sightings`
- **주요 컬럼**:
  - `visibility`: `app.visibility` ENUM (PUBLIC, PRIVATE)
  - `detected_by`: `app.detected_by` ENUM (AI, USER)
  - `geom`: PostGIS `geometry(Point,4326)`
  - `species_id`: FK → `app.species`
  - `media_id`: FK → `app.media`
  - `user_id`: FK → `app.users`

### Swagger 문서화
- `@Tag(name = "Sighting", description = "동물 목격 정보 API")`
- `@Operation(summary = "...", description = "...")`
- `@ApiResponse` 코드별 설명 추가
- 요청/응답 예시 포함

---

## ✅ 테스트 계획

### 단위 테스트
- [ ] `SightingQueryServiceTest.findAllSightings()`
- [ ] `SightingQueryServiceTest.findAllSightings_withKeyword()`
- [ ] `SightingQueryServiceTest.findSightingDetail()`
- [ ] `SightingServiceTest.updateSighting()`
- [ ] `SightingServiceTest.deleteSighting()`

### 통합 테스트
- [ ] `SightingControllerTest.getAllSightings()`
- [ ] `SightingControllerTest.getAllSightings_withPagination()`
- [ ] `SightingControllerTest.getAllSightings_withKeywordSearch()`
- [ ] `SightingControllerTest.getSightingDetail()`
- [ ] `SightingControllerTest.updateSighting()`
- [ ] `SightingControllerTest.deleteSighting()`

### 권한 테스트
- [ ] Public Sighting → 비로그인 사용자 조회 가능
- [ ] Private Sighting → 소유자만 조회 가능
- [ ] Private Sighting → ADMIN 조회 가능
- [ ] 수정/삭제 → 타인 시도 시 403 Forbidden
- [ ] 수정/삭제 → ADMIN은 모든 리소스 접근 가능

---

## 📌 추가 고려사항

### 1. 고급 검색 필터
현재는 학명과 한국어 이름 검색만 지원합니다.
추후 다음 필터 추가 고려:
- 영어 이름 검색 (`common_name_en`)
- 제목/설명 검색 (`title`, `description`)
- 사용자 필터 (`user_id`, `display_name`)
- 날짜 범위 검색 (`occurred_at_from`, `occurred_at_to`)
- 검증 상태 필터 (`is_verified`)
- Species 상태 필터 (`species.status`)

### 2. Soft Delete
추후 요구사항 발생 시:
- `deleted_at` 컬럼 추가
- `@SQLDelete`, `@Where` 어노테이션 사용
- 삭제된 데이터 복구 API

### 3. 이미지 변경 기능
- 현재는 Sighting 생성 시에만 이미지 업로드 가능
- 수정 API에서 이미지 교체 기능 추가 고려
  - 기존 Media 삭제 + 새 Media 생성
  - 또는 별도 API로 분리 (`PATCH /api/v1/sightings/{id}/image`)

---

## 🎨 API 명세 요약

| Method | Endpoint | Description | Auth | 권한 |
|--------|----------|-------------|------|------|
| **GET** | `/api/v1/sightings` | Sighting 목록 조회 (페이징, 검색) | Optional | Public: 모두, Private: 소유자만 |
| **GET** | `/api/v1/sightings/{id}` | Sighting 상세 조회 | Optional | Public: 모두, Private: 소유자/ADMIN |
| **PATCH** | `/api/v1/sightings/{id}` | Sighting 수정 | Required | 소유자 또는 ADMIN |
| **DELETE** | `/api/v1/sightings/{id}` | Sighting 삭제 | Required | 소유자 또는 ADMIN |

### 쿼리 파라미터 (목록 조회)
- `page`: 페이지 번호 (기본값: 0)
- `size`: 페이지 크기 (기본값: 20, 최대: 100)
- `sortBy`: 정렬 필드 (`created_at`, `occurred_at`, 기본값: `created_at`)
- `sortOrder`: 정렬 방향 (`asc`, `desc`, 기본값: `desc`)
- `keyword`: 검색어 (학명 또는 한국어 이름, ILIKE 검색)

---

**작성일**: 2025-10-02
**작성자**: Claude Code
**버전**: 1.1
**변경 이력**:
- v1.0: 초기 계획 (상세 조회, 수정, 삭제)
- v1.1: 전체 목록 조회 API 추가 (페이징, 학명/한국어 이름 검색)
