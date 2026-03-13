package com.jsl26tp.jsl26tp.admin.service;

import com.jsl26tp.jsl26tp.common.BusinessException;
import com.jsl26tp.jsl26tp.common.ErrorCode;
import com.jsl26tp.jsl26tp.admin.domain.*;
import com.jsl26tp.jsl26tp.admin.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理者 Service
 *
 * 관리자 페이지의 모든 비즈니스 로직 처리
 * - 예외는 BusinessException으로 던짐 → GlobalExceptionHandler가 ApiResponse.error()로 변환
 * - Mapper 호출은 Service에서만 (Controller에서 Mapper 직접 호출 금지)
 * - 상태 변경 메서드는 @Transactional 적용 (실패 시 롤백)
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminMapper adminMapper;

    /** 페이지당 표시 건수 (모든 목록 공통) */
    private static final int PAGE_SIZE = 20;

    // =====================================================================
    // 1. 회원 관리 (FR-SCR004-1: 회원 정지, FR-SCR004-2: 계정 삭제)
    // =====================================================================

    /**
     * 회원 목록 조회 (페이징)
     * @param keyword 검색어 (username/nickname/email LIKE)
     * @param status  상태 필터 (ACTIVE/SUSPENDED/DELETED, 빈 문자열이면 전체)
     * @param page    현재 페이지 번호 (0-based)
     * @return AdminPageResponse — dashboard.html JS 필드명(content, number)에 맞춘 admin 전용 응답
     */
    public AdminPageResponse<AdminUser> getUserList(String keyword, String status, int page) {
        int offset = page * PAGE_SIZE;
        List<AdminUser> items = adminMapper.findUserList(keyword, status, offset, PAGE_SIZE);
        int totalCount = adminMapper.countUsers(keyword, status);
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        // AdminPageResponse: content(=items), number(=page), totalPages → HTML의 res.data.* 와 일치
        return new AdminPageResponse<>(items, page, PAGE_SIZE, totalCount, totalPages);
    }

    /**
     * 회원 상세 조회
     * @throws BusinessException USER_NOT_FOUND — 회원이 존재하지 않을 때
     */
    public AdminUser getUserById(Long id) {
        AdminUser user = adminMapper.findUserById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 회원 정지 (FR-SCR004-1)
     * @param id   대상 회원 ID
     * @param days 정지 일수 (0이면 영구 정지, 양수면 기간 정지)
     * @throws BusinessException ACCESS_DENIED — 관리자 계정 정지 시도 시
     */
    @Transactional
    public void suspendUser(Long id, int days) {
        AdminUser user = getUserById(id);

        // 관리자 계정은 정지 불가
        if ("ROLE_ADMIN".equals(user.getRole())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // days=0 → 영구 정지 (suspendUntil=null), days>0 → 기간 정지
        String suspendUntil = (days > 0)
                ? LocalDateTime.now().plusDays(days).toString()
                : null;

        adminMapper.updateUserStatus(id, "SUSPENDED", suspendUntil);
    }

    /**
     * 회원 정지 해제
     * → status="ACTIVE", suspend_until=NULL
     */
    @Transactional
    public void unsuspendUser(Long id) {
        getUserById(id); // 존재 확인 (없으면 USER_NOT_FOUND)
        adminMapper.updateUserStatus(id, "ACTIVE", null);
    }

    /**
     * 회원 삭제 (FR-SCR004-2)
     * ※ users 테이블은 deleted_at 없음 → status="DELETED"로 소프트 삭제
     */
    @Transactional
    public void deleteUser(Long id) {
        getUserById(id); // 존재 확인
        adminMapper.updateUserStatus(id, "DELETED", null);
    }

    // =====================================================================
    // 2. 신고 관리 (FR-SCR004-3)
    // =====================================================================

    /** 신고 목록 조회 (페이징) */
    public AdminPageResponse<AdminReport> getReportList(String status, String targetType, int page) {
        int offset = page * PAGE_SIZE;
        List<AdminReport> items = adminMapper.findReportList(status, targetType, offset, PAGE_SIZE);
        int totalCount = adminMapper.countReports(status, targetType);
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        return new AdminPageResponse<>(items, page, PAGE_SIZE, totalCount, totalPages);
    }

    /**
     * 신고 상세 조회
     * @throws BusinessException BAD_REQUEST — 신고가 존재하지 않을 때
     */
    public AdminReport getReportById(Long id) {
        AdminReport report = adminMapper.findReportById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return report;
    }

    /**
     * 신고 승인 — 대상 콘텐츠에 조치 후 PROCESSED 처리
     *
     * 처리 흐름:
     * 1. 신고 조회 → PENDING 상태인지 확인
     * 2. target_type에 따라 대상 조치:
     *    - "REVIEW" → reviews.status = "HIDDEN" (비표시)
     *    - "TOILET" → toilets.status = "REJECTED" (반려)
     * 3. reports.status = "PROCESSED", processed_at = NOW()
     *
     * @Transactional: 대상 조치 + 신고 상태 변경을 하나의 트랜잭션으로 (실패 시 전체 롤백)
     */
    @Transactional
    public void resolveReport(Long id, String adminNote) {
        AdminReport report = getReportById(id);

        // 이미 처리된 신고는 재처리 불가
        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 대상 콘텐츠에 조치
        if ("REVIEW".equals(report.getTargetType())) {
            adminMapper.hideReview(report.getTargetId());        // 리뷰 비표시
        } else if ("TOILET".equals(report.getTargetType())) {
            adminMapper.rejectToiletByReport(report.getTargetId()); // 화장실 반려
        }

        // 신고를 PROCESSED로 변경 (DB 설계서 기준)
        adminMapper.updateReportStatus(id, "PROCESSED", adminNote);
    }

    /**
     * 신고 기각 — 대상 콘텐츠는 변경하지 않고 DISMISSED 처리
     */
    @Transactional
    public void dismissReport(Long id, String adminNote) {
        AdminReport report = getReportById(id);

        if (!"PENDING".equals(report.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 대상 콘텐츠 변경 없이 신고만 기각
        adminMapper.updateReportStatus(id, "DISMISSED", adminNote);
    }

    // =====================================================================
    // 3. 화장실 승인 (FR-SCR004-4)
    // =====================================================================

    /** 화장실 목록 조회 (페이징) */
    public AdminPageResponse<AdminToilet> getToiletList(String status, int page) {
        int offset = page * PAGE_SIZE;
        List<AdminToilet> items = adminMapper.findToiletList(status, offset, PAGE_SIZE);
        int totalCount = adminMapper.countToilets(status);
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        return new AdminPageResponse<>(items, page, PAGE_SIZE, totalCount, totalPages);
    }

    /**
     * 화장실 상세 조회
     * @throws BusinessException TOILET_NOT_FOUND — 화장실이 존재하지 않을 때
     */
    public AdminToilet getToiletById(Long id) {
        AdminToilet toilet = adminMapper.findToiletById(id);
        if (toilet == null) {
            throw new BusinessException(ErrorCode.TOILET_NOT_FOUND);
        }
        return toilet;
    }

    /**
     * 화장실 승인 (PENDING → APPROVED)
     * - SQL에 AND status='PENDING' 조건이 있어서 이미 처리된 건은 영향행 0
     * - 영향행 0이면 BAD_REQUEST 예외
     */
    @Transactional
    public void approveToilet(Long id) {
        if (adminMapper.approveToilet(id) == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    /**
     * 화장실 반려 (PENDING → REJECTED)
     */
    @Transactional
    public void rejectToilet(Long id) {
        if (adminMapper.rejectToilet(id) == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
    }

    // =====================================================================
    // 4. 문의 관리 (FR-SCR004-5)
    // =====================================================================

    /** 문의 목록 조회 (페이징) */
    public AdminPageResponse<AdminInquiry> getInquiryList(String status, String keyword, int page) {
        int offset = page * PAGE_SIZE;
        List<AdminInquiry> items = adminMapper.findInquiryList(status, keyword, offset, PAGE_SIZE);
        int totalCount = adminMapper.countInquiries(status, keyword);
        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
        return new AdminPageResponse<>(items, page, PAGE_SIZE, totalCount, totalPages);
    }

    /**
     * 문의 상세 조회
     * @throws BusinessException BAD_REQUEST — 문의가 존재하지 않을 때
     */
    public AdminInquiry getInquiryById(Long id) {
        AdminInquiry inquiry = adminMapper.findInquiryById(id);
        if (inquiry == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        return inquiry;
    }

    /**
     * 문의 답변 등록
     * → SET answer=답변, admin_id=관리자ID, status="ANSWERED", answered_at=NOW()
     */
    @Transactional
    public void answerInquiry(Long id, String answer, Long adminId) {
        getInquiryById(id); // 존재 확인
        adminMapper.answerInquiry(id, answer, adminId);
    }
}