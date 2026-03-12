# 신고(Report) API 완성 플랜

## 현재 상태
- ✅ `Report.java` (도메인) — 있음
- ✅ `ReportMapper.java` (매퍼 인터페이스) — 있음
- ✅ `ReportMapper.xml` (SQL) — 있음
- ❌ `ReportService.java` — **없음 (생성 필요)**
- ❌ `ReportController.java` — **없음 (생성 필요)**

## 생성할 파일 (2개)

### 1. ReportService.java
**위치**: `report/service/ReportService.java`

**패턴**: 기존 ReviewService, ToiletService와 동일
- `@Service` + `@RequiredArgsConstructor`
- `BusinessException` + `ErrorCode` 사용
- `@Transactional` 적용

**메서드**:
| 메서드 | 설명 |
|--------|------|
| `createReport(Report)` | 신고 등록 (중복 체크 포함) |
| `getMyReports(reporterId)` | 내 신고 목록 조회 |
| `getReportDetail(id)` | 신고 상세 조회 |

**비즈니스 로직**:
- 중복 신고 방지: 같은 유저가 같은 대상을 이미 신고했으면 `DUPLICATE_REPORT` 에러
- targetType 유효성: "REVIEW" 또는 "TOILET"만 허용
- reason 빈값 체크

### 2. ReportController.java
**위치**: `report/controller/ReportController.java`

**패턴**: 기존 ToiletController 스타일 (`@RestController`)
- `@AuthenticationPrincipal CustomUserDetails` 로 로그인 유저 확인
- `ApiResponse<T>` 공통 응답

**엔드포인트**:
| Method | URL | 설명 |
|--------|-----|------|
| `POST /api/reports` | 신고 등록 | 로그인 필요 |
| `GET /api/reports/my` | 내 신고 목록 | 로그인 필요 |

## 기존 파일 수정 (2개 — 메서드 추가만, 기존 코드 수정 없음)

### 3. ReportMapper.java — 메서드 2개 추가
- `countByReporterAndTarget(reporterId, targetType, targetId)` — 중복 신고 체크용
- `findByReporterId(reporterId)` — 내 신고 목록용

### 4. ReportMapper.xml — SQL 2개 추가
- `countByReporterAndTarget` SELECT 쿼리
- `findByReporterId` SELECT 쿼리

## 안 건드리는 것
- ❌ 프론트엔드 (HTML/CSS/JS) 일절 수정 안 함
- ❌ 기존 Mapper 메서드/SQL 수정 없음 (추가만)
- ❌ SecurityConfig 수정 불필요 (`.anyRequest().authenticated()`에 의해 자동으로 로그인 필요)
