package com.jsl26tp.jsl26tp.admin.mapper;

import com.jsl26tp.jsl26tp.admin.domain.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 管理者専用 MyBatis Mapper
 *
 * 관리자 페이지의 모든 SQL 매핑 인터페이스
 * - 메서드명 = AdminMapper.xml의 <select>/<update> id와 일치
 * - 파라미터 2개 이상일 때 @Param 필수
 * - resultType은 com.jsl26tp.jsl26tp.admin.domain.* 패키지
 */
@Mapper
public interface AdminMapper {

    // =====================================================================
    // 회원 관리 (FR-SCR004-1: 회원 정지, FR-SCR004-2: 계정 삭제)
    // =====================================================================

    /**
     * 회원 목록 조회 (키워드 검색 + 상태 필터 + 페이징)
     * - keyword: username / nickname / email LIKE 검색
     * - status: ACTIVE / SUSPENDED / DELETED 필터
     * - 리뷰수, 신고받은수 서브쿼리 포함
     */
    List<AdminUser> findUserList(
            @Param("keyword") String keyword,
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 회원 총 건수 (페이징 계산용) — findUserList와 동일 WHERE 조건 */
    int countUsers(
            @Param("keyword") String keyword,
            @Param("status") String status
    );

    /** 회원 상세 조회 (리뷰수 + 신고받은수 포함) */
    AdminUser findUserById(@Param("id") Long id);

    /**
     * 회원 상태 변경 (정지 / 해제 / 삭제)
     * - 정지: status="SUSPENDED", suspendUntil=해제일시 (영구면 null)
     * - 해제: status="ACTIVE", suspendUntil=null
     * - 삭제: status="DELETED", suspendUntil=null
     */
    int updateUserStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("suspendUntil") String suspendUntil
    );

    // =====================================================================
    // 신고 관리 (FR-SCR004-3: 신고 목록 조회 및 처리 상태 변경)
    // =====================================================================

    /**
     * 신고 목록 조회 (상태 + 대상타입 필터 + PENDING 우선 정렬)
     * - reports + 신고자(users) + 리뷰(reviews) + 화장실(toilets) JOIN
     * - PENDING 상태가 항상 맨 위로 정렬됨
     */
    List<AdminReport> findReportList(
            @Param("status") String status,
            @Param("targetType") String targetType,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 신고 총 건수 */
    int countReports(
            @Param("status") String status,
            @Param("targetType") String targetType
    );

    /** 신고 상세 조회 (신고자 + 대상 콘텐츠 정보 포함) */
    AdminReport findReportById(@Param("id") Long id);

    /**
     * 신고 상태 변경 + 관리자 메모 + 처리일시 기록
     * - 승인: status="PROCESSED", processed_at=NOW()
     * - 기각: status="DISMISSED", processed_at=NOW()
     */
    int updateReportStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("adminNote") String adminNote
    );

    /** 리뷰 비표시 처리 — 신고 승인 시 대상 리뷰에 조치 (status → "HIDDEN") */
    int hideReview(@Param("reviewId") Long reviewId);

    /** 화장실 반려 — 신고 승인 시 대상 화장실에 조치 (status → "REJECTED") */
    int rejectToiletByReport(@Param("toiletId") Long toiletId);

    // =====================================================================
    // 화장실 승인 (FR-SCR004-4: 사용자 제보 화장실 승인/반려)
    // =====================================================================

    /**
     * 화장실 목록 조회 (PENDING 우선 정렬)
     * ※ deleted_at IS NULL 필수 (소프트 삭제 대상 테이블)
     */
    List<AdminToilet> findToiletList(
            @Param("status") String status,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 화장실 총 건수 (deleted_at IS NULL 필터 포함) */
    int countToilets(@Param("status") String status);

    /** 화장실 상세 조회 (deleted_at IS NULL 필터 포함) */
    AdminToilet findToiletById(@Param("id") Long id);

    /** 화장실 승인 (PENDING → APPROVED) — 이미 처리된 건은 0 반환 */
    int approveToilet(@Param("id") Long id);

    /** 화장실 반려 (PENDING → REJECTED) — 이미 처리된 건은 0 반환 */
    int rejectToilet(@Param("id") Long id);

    // =====================================================================
    // 문의 관리 (FR-SCR004-5: 문의사항 답변)
    // =====================================================================

    /**
     * 문의 목록 조회 (WAITING 우선 정렬)
     * ※ deleted_at IS NULL 필수 (소프트 삭제 대상 테이블)
     * - keyword: 제목 / 내용 LIKE 검색
     */
    List<AdminInquiry> findInquiryList(
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    /** 문의 총 건수 (deleted_at IS NULL 필터 포함) */
    int countInquiries(
            @Param("status") String status,
            @Param("keyword") String keyword
    );

    /** 문의 상세 조회 (작성자 + 답변 관리자 정보 포함) */
    AdminInquiry findInquiryById(@Param("id") Long id);

    /**
     * 문의 답변 등록
     * → SET answer=답변, admin_id=관리자ID, status="ANSWERED", answered_at=NOW()
     */
    int answerInquiry(
            @Param("id") Long id,
            @Param("answer") String answer,
            @Param("adminId") Long adminId
    );
}