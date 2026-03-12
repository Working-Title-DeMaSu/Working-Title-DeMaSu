package com.jsl26tp.jsl26tp.report.controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.config.CustomUserDetails;
import com.jsl26tp.jsl26tp.report.domain.Report;
import com.jsl26tp.jsl26tp.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 通報 Controller (ユーザー向け)
 *
 * 유저가 리뷰/화장실을 신고하는 API
 * - 관리자 신고 처리는 AdminController에서 담당
 * - 모든 엔드포인트 로그인 필요 (SecurityConfig → anyRequest().authenticated())
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 신고 등록
     * POST /api/reports
     *
     * @param report { targetType: "REVIEW"/"TOILET", targetId: 대상ID, reason: "신고사유" }
     * @param user   로그인 유저 (reporterId 자동 세팅)
     */
    @PostMapping
    public ApiResponse<Void> createReport(
            @RequestBody Report report,
            @AuthenticationPrincipal CustomUserDetails user) {

        report.setReporterId(user.getId());
        reportService.createReport(report);
        return ApiResponse.ok();
    }

    /**
     * 내 신고 목록 조회
     * GET /api/reports/my
     */
    @GetMapping("/my")
    public ApiResponse<List<Report>> getMyReports(
            @AuthenticationPrincipal CustomUserDetails user) {

        List<Report> reports = reportService.getMyReports(user.getId());
        return ApiResponse.ok(reports);
    }
}
