package com.jsl26tp.jsl26tp.admin.controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.common.PageResponse;
import com.jsl26tp.jsl26tp.admin.domain.*;
import com.jsl26tp.jsl26tp.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 * 管理者 Controller (FR-SCR004)
 *
 * 공통코드 규칙:
 * - 페이지 이동: String 반환 (Thymeleaf 템플릿 경로)
 * - API: @ResponseBody + ApiResponse<T> 반환
 * - API URL은 /api/ 접두사
 * - 비즈니스 로직은 AdminService에 위임 (Controller에서 Mapper 직접 호출 금지)
 *
 * 접근 제어: SecurityConfig에서 /admin/**, /api/admin/** → ROLE_ADMIN만 허용
 */
@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // =====================================================================
    // 페이지 이동 (Thymeleaf 렌더링)
    // templates/admin/ 하위에 HTML 파일 필요
    // =====================================================================

    /** 관리자 로그인 페이지 → templates/admin/login.html */
    @GetMapping("/admin/login")
    public String loginPage() {
        return "admin/login";
    }

    /** 대시보드 메인 → templates/admin/dashboard.html */
    @GetMapping("/admin/dashboard")
    public String dashboard() {
        return "admin/dashboard";
    }

    /** 회원 관리 페이지 → templates/admin/users.html */
    @GetMapping("/admin/users")
    public String usersPage() {
        return "admin/users";
    }

    /** 신고 관리 페이지 → templates/admin/reports.html */
    @GetMapping("/admin/reports")
    public String reportsPage() {
        return "admin/reports";
    }

    /** 화장실 승인 페이지 → templates/admin/toilets.html */
    @GetMapping("/admin/toilets")
    public String toiletsPage() {
        return "admin/toilets";
    }

    /** 문의 관리 페이지 → templates/admin/inquiries.html */
    @GetMapping("/admin/inquiries")
    public String inquiriesPage() {
        return "admin/inquiries";
    }

    // =====================================================================
    // 1. 회원 관리 API (FR-SCR004-1: 회원 정지, FR-SCR004-2: 계정 삭제)
    // =====================================================================

    /**
     * 회원 목록 조회
     * @param keyword 검색어 (username/nickname/email LIKE)
     * @param status  상태 필터 (ACTIVE/SUSPENDED/DELETED)
     * @param page    페이지 번호 (0-based)
     * @return ApiResponse<PageResponse> — 공통코드 통일 응답
     */
    @GetMapping("/api/admin/users")
    @ResponseBody
    public ApiResponse<PageResponse<AdminUser>> getUserList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(adminService.getUserList(keyword, status, page));
    }

    /** 회원 상세 조회 */
    @GetMapping("/api/admin/users/{id}")
    @ResponseBody
    public ApiResponse<AdminUser> getUserDetail(@PathVariable Long id) {
        return ApiResponse.ok(adminService.getUserById(id));
    }

    /**
     * 회원 정지 (FR-SCR004-1)
     * @param days 정지 일수 (0=영구, 양수=기간)
     */
    @PostMapping("/api/admin/users/{id}/suspend")
    @ResponseBody
    public ApiResponse<Void> suspendUser(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int days) {
        adminService.suspendUser(id, days);
        return ApiResponse.ok();
    }

    /** 회원 정지 해제 → status="ACTIVE", suspend_until=NULL */
    @PostMapping("/api/admin/users/{id}/unsuspend")
    @ResponseBody
    public ApiResponse<Void> unsuspendUser(@PathVariable Long id) {
        adminService.unsuspendUser(id);
        return ApiResponse.ok();
    }

    /** 회원 삭제 (FR-SCR004-2) → status="DELETED" 소프트 삭제 */
    @PostMapping("/api/admin/users/{id}/delete")
    @ResponseBody
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ApiResponse.ok();
    }

    // =====================================================================
    // 2. 신고 관리 API (FR-SCR004-3)
    // =====================================================================

    /** 신고 목록 조회 (상태+대상타입 필터, PENDING 우선) */
    @GetMapping("/api/admin/reports")
    @ResponseBody
    public ApiResponse<PageResponse<AdminReport>> getReportList(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String targetType,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(adminService.getReportList(status, targetType, page));
    }

    /**
     * 신고 승인 → 대상 콘텐츠 조치 + reports.status="PROCESSED"
     * - REVIEW 대상 → reviews.status="HIDDEN"
     * - TOILET 대상 → toilets.status="REJECTED"
     */
    @PostMapping("/api/admin/reports/{id}/resolve")
    @ResponseBody
    public ApiResponse<Void> resolveReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String adminNote) {
        adminService.resolveReport(id, adminNote);
        return ApiResponse.ok();
    }

    /** 신고 기각 → 대상 변경 없이 reports.status="DISMISSED" */
    @PostMapping("/api/admin/reports/{id}/dismiss")
    @ResponseBody
    public ApiResponse<Void> dismissReport(
            @PathVariable Long id,
            @RequestParam(defaultValue = "") String adminNote) {
        adminService.dismissReport(id, adminNote);
        return ApiResponse.ok();
    }

    // =====================================================================
    // 3. 화장실 승인 API (FR-SCR004-4)
    // =====================================================================

    /** 화장실 목록 조회 (PENDING 우선, deleted_at IS NULL) */
    @GetMapping("/api/admin/toilets")
    @ResponseBody
    public ApiResponse<PageResponse<AdminToilet>> getToiletList(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(adminService.getToiletList(status, page));
    }

    /** 화장실 승인 → toilets.status = "APPROVED" (지도에 표시됨) */
    @PostMapping("/api/admin/toilets/{id}/approve")
    @ResponseBody
    public ApiResponse<Void> approveToilet(@PathVariable Long id) {
        adminService.approveToilet(id);
        return ApiResponse.ok();
    }

    /** 화장실 반려 → toilets.status = "REJECTED" (지도에 미표시) */
    @PostMapping("/api/admin/toilets/{id}/reject")
    @ResponseBody
    public ApiResponse<Void> rejectToilet(@PathVariable Long id) {
        adminService.rejectToilet(id);
        return ApiResponse.ok();
    }

    // =====================================================================
    // 4. 문의 관리 API (FR-SCR004-5)
    // =====================================================================

    /** 문의 목록 조회 (WAITING 우선, deleted_at IS NULL) */
    @GetMapping("/api/admin/inquiries")
    @ResponseBody
    public ApiResponse<PageResponse<AdminInquiry>> getInquiryList(
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page) {
        return ApiResponse.ok(adminService.getInquiryList(status, keyword, page));
    }

    /**
     * 문의 답변 등록
     * → SET answer=답변, admin_id=관리자ID, status="ANSWERED", answered_at=NOW()
     */
    @PostMapping("/api/admin/inquiries/{id}/answer")
    @ResponseBody
    public ApiResponse<Void> answerInquiry(
            @PathVariable Long id,
            @RequestParam String answer,
            @RequestParam Long adminId) {
        adminService.answerInquiry(id, answer, adminId);
        return ApiResponse.ok();
    }
}
