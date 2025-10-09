# SightingService - createSighting 메서드 처리 흐름

## 개요
`createSighting` 메서드는 사용자가 업로드한 이미지를 분석하여 동물 목격 정보(Sighting)를 생성하는 핵심 메서드입니다.

**위치**: `src/main/java/com/example/demo/domain/service/SightingService.java:222`

**응답 시간**: 약 3-7초 (YOLO 대기 시간 포함)

---

## 처리 흐름 다이어그램

```
사용자 이미지 업로드
        ↓
[1] EXIF 메타데이터 추출 (GPS, 촬영시간, 카메라정보)
        ↓
[2] EXIF 제거 이미지 생성 (보안 처리)
        ↓
[3] S3 업로드 (원본 + EXIF 제거 버전)
        ↓
[4] Vision API 동물 인식 (신뢰도 0.5 이상, 최대 5개)
        ↓
[5] 동물 미감지 검증 (없으면 예외 발생)
        ↓
[6] FastAPI YOLO 추론 요청 (비동기)
        ↓
[7] Media 엔티티 생성 및 저장
        ↓
[8] AiDetection 엔티티 저장 (Vision API 결과)
        ↓
[9] YOLO 결과 대기 및 최적 탐지 선택
        ├─ YOLO 신뢰도 >= 90% → YOLO 결과 사용
        └─ 그 외 → Vision API 결과 사용
        ↓
[10] Sighting 엔티티 생성 및 저장 (EXIF 데이터 반영)
        ↓
[11] Species 처리 이벤트 발행 (비동기)
        ↓
[12] 사용자에게 즉시 응답 반환
        ↓
백그라운드에서 Species 처리 (이벤트 리스너)
```

---

## 단계별 상세 설명

### 1. EXIF 메타데이터 추출
```java
ExifMetadata exifMetadata = exifService.extractMetadata(imageFile);
```
- **목적**: 이미지에서 GPS 좌표, 촬영 시간, 카메라 정보 추출
- **추출 정보**:
  - GPS 위치 (위도/경도)
  - 촬영 시간
  - 카메라 제조사 및 모델명
  - 이미지 크기 (width, height)

### 2. EXIF 제거 이미지 생성
```java
byte[] sanitizedImageBytes = exifRemovalService.removeExifMetadata(imageFile);
```
- **목적**: 개인정보 보호를 위해 EXIF 데이터를 제거한 이미지 생성
- **보안**: 공개 URL에는 EXIF 제거 버전만 노출

### 3. S3 업로드 (2개 버전)
```java
S3Service.ImageUploadResult uploadResult = s3Service.uploadBothVersions(imageFile, sanitizedImageBytes);
```
- **원본 이미지** (`originalUrl`): EXIF 포함 (소유자만 접근)
- **공개 이미지** (`sanitizedUrl`): EXIF 제거 (모든 사용자 접근)

### 4. Vision API 동물 인식
```java
List<AnimalDetection> detections = animalVisionService.detectAnimals(imageFile, 0.5f, 5);
```
- **신뢰도 임계값**: 0.5 (50%) 이상
- **최대 결과 개수**: 5개
- **반환 정보**:
  - label (동물명)
  - confidence (신뢰도)
  - scientificName (학명)
  - description (설명)

### 5. 동물 미감지 검증
```java
if (detections.isEmpty()) {
    throw new SightingException(SightingErrorCode.NO_ANIMAL_DETECTED);
}
```
- **예외 발생**: Vision API가 동물을 인식하지 못한 경우 즉시 종료

### 6. FastAPI YOLO 추론 요청 (비동기)
```java
String yoloRequestId = requestYoloInference(uploadResult.getSanitizedUrl());
```
- **목적**: 더 정확한 동물 분류를 위해 YOLO 모델 활용
- **비동기 처리**: 요청 후 ID만 받고 계속 진행
- **실패 처리**: YOLO 실패 시 Vision API 결과만 사용

### 7. Media 엔티티 생성 및 저장
```java
Media media = createMediaEntity(user, imageFile, uploadResult, exifMetadata);
```
- **저장 정보**:
  - storagePath: 원본 이미지 URL
  - mimeType, bytes, width, height
  - extraInfo (JSONB):
    - sanitizedUrl (공개 URL)
    - cameraMake, cameraModel
    - capturedAt (촬영 시간)
    - gpsLatitude, gpsLongitude

### 8. AiDetection 엔티티 저장 (Vision API)
```java
saveAiDetections(media, detections, "VISION");
```
- **목적**: Vision API가 탐지한 모든 결과를 저장
- **저장 정보**:
  - label, score (신뢰도)
  - extraInfo:
    - source: "VISION"
    - scientificName
    - description

### 9. YOLO 결과 대기 및 최적 탐지 선택
```java
AnimalDetection topDetection = selectBestDetection(detections, yoloRequestId, media);
```

**처리 로직**:

1. **YOLO 결과 대기** (최대 5초, 200ms 간격으로 폴링)
   ```java
   YoloCallbackRequest yoloResult = waitForYoloResult(yoloRequestId, 5000);
   ```

2. **YOLO 결과 변환 및 저장**
   ```java
   List<AnimalDetection> yoloDetections = convertYoloToAnimalDetections(yoloResult);
   saveAiDetections(media, yoloDetections, "YOLO");
   ```

3. **최적 탐지 결과 선택**
   - **YOLO 신뢰도 >= 90%** → YOLO 결과 사용
   - **그 외** → Vision API 결과 사용

### 10. Sighting 엔티티 생성 및 저장
```java
Sighting sighting = createSightingEntity(user, media, exifMetadata, title, description, topDetection.getLabel(), topDetection.getConfidence());
```

**저장 정보**:
- user, media (관계 설정)
- title (미입력 시 동물명 사용)
- description
- detectedBy: `AI`
- aiConfidence: 선택된 탐지 결과의 신뢰도
- visibility: `PUBLIC`
- isVerified: `false`
- **EXIF 데이터 반영**:
  - geom: GPS 좌표 (있는 경우)
  - occurredAt: 촬영 시간 (있는 경우)

### 11. Species 처리 이벤트 발행 (비동기)
```java
if (topDetection.getScientificName() != null && !topDetection.getScientificName().isEmpty()) {
    publishSpeciesProcessingEvent(sighting, topDetection);
}
```

**이벤트 정보**:
- sightingId
- label (동물명)
- scientificName (학명)
- confidence (신뢰도)

**백그라운드 처리**:
- 이벤트 리스너가 Species 테이블 조회/생성
- Sighting의 species_id 업데이트
- 위키피디아 API 호출 등 추가 정보 수집

### 12. 사용자에게 즉시 응답 반환
```java
return SightingCreateResult.of(sighting, media, detections, true);
```

**응답 데이터**:
- sighting: 생성된 Sighting 엔티티
- media: 생성된 Media 엔티티
- detections: Vision API 탐지 결과 리스트
- speciesProcessing: `true` (Species 처리 진행 중)

---

## 주요 특징

### 1. 빠른 응답 시간 (3-7초)
- Species 처리는 비동기로 백그라운드에서 진행
- 사용자는 즉시 Sighting 생성 결과 확인 가능

### 2. 이중 AI 모델 활용
- **Vision API**: 빠른 응답, 학명 제공
- **YOLO**: 높은 정확도 (90% 이상 시 우선 사용)

### 3. 개인정보 보호
- 원본 이미지: 소유자만 접근 가능
- 공개 이미지: EXIF 제거 후 모든 사용자 접근 가능

### 4. EXIF 메타데이터 활용
- GPS 좌표 → 목격 위치 자동 설정
- 촬영 시간 → 목격 시간 자동 설정
- 카메라 정보 → Media 메타데이터에 저장

### 5. AI 탐지 결과 이력 관리
- Vision API와 YOLO의 모든 탐지 결과를 `AiDetection` 테이블에 저장
- source 필드로 구분 ("VISION" / "YOLO")

---

## 에러 처리

### 동물 미감지
```java
throw new SightingException(SightingErrorCode.NO_ANIMAL_DETECTED);
```
- Vision API가 동물을 인식하지 못한 경우

### YOLO 실패
- 로그만 남기고 Vision API 결과로 계속 진행
- 트랜잭션 롤백 없음

### Species 처리 실패
- 백그라운드에서 처리되므로 사용자 응답에 영향 없음
- 이벤트 리스너에서 별도 에러 처리

---

## 관련 엔티티

### Sighting
- 동물 목격 기록
- EXIF GPS → `geom` (PostGIS POINT)
- EXIF 촬영시간 → `occurred_at`

### Media
- 이미지 파일 정보
- `storage_path`: 원본 URL (EXIF 포함)
- `extra_info` JSONB:
  - `sanitizedUrl`: 공개 URL (EXIF 제거)
  - EXIF 메타데이터 (카메라, GPS, 촬영시간)

### AiDetection
- AI 탐지 결과 이력
- Vision API와 YOLO 결과 모두 저장
- `extra_info` JSONB:
  - `source`: "VISION" 또는 "YOLO"
  - `scientificName`
  - `description`

### Species (비동기 처리)
- 동물 종 정보
- 이벤트 리스너가 백그라운드에서 생성/조회
- Sighting과 다대일 관계

---

## 성능 최적화

### 1. YOLO 타임아웃 (5초)
- YOLO 응답이 느릴 경우 Vision API 결과로 즉시 진행
- 폴링 간격: 200ms

### 2. 비동기 Species 처리
- Wikipedia API 호출 등 시간이 걸리는 작업을 백그라운드로 분리
- 사용자 응답 시간 단축

### 3. 트랜잭션 범위
- `@Transactional`: Sighting, Media, AiDetection 저장까지만
- Species 처리는 별도 트랜잭션

---

## 참고사항

### YOLO와 Vision API 비교

| 항목 | Vision API | YOLO |
|------|------------|------|
| 속도 | 빠름 (1-2초) | 보통 (3-5초) |
| 정확도 | 보통 | 높음 (특화 모델) |
| 학명 제공 | O | X |
| Bounding Box | X | O |
| 사용 조건 | 항상 | 신뢰도 >= 90% |

### EXIF 데이터 흐름

```
이미지 업로드
    ↓
EXIF 추출 (ExifService)
    ↓
┌─────────────────┬─────────────────┐
│  원본 이미지     │  EXIF 제거 이미지 │
│  (EXIF 포함)    │  (보안 처리)      │
└─────────────────┴─────────────────┘
    ↓                   ↓
 S3 저장             S3 저장
    ↓                   ↓
storage_path      sanitizedUrl
(소유자만 접근)    (공개 접근)
    ↓
Media.extra_info (JSONB)
    ↓
Sighting (geom, occurred_at)
```
