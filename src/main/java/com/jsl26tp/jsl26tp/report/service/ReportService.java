package com.jsl26tp.jsl26tp.report.service;

import com.jsl26tp.jsl26tp.common.BusinessException;
import com.jsl26tp.jsl26tp.common.ErrorCode;
import com.jsl26tp.jsl26tp.report.domain.Report;
import com.jsl26tp.jsl26tp.report.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通報 Service (ユーザー向け)
 *
 * 유저가 리뷰/화장실을 신고하는 비즈니스 로직 담당
 * - 관리자 신고 처리는 AdminService에서 담당
 * - 예외는 BusinessException → GlobalExceptionHandler가 ApiResponse.error()로 변환
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    /**
     * 신고 등록
     *
     * 검증:
     * 1. targetType이 "REVIEW" 또는 "TOILET"인지 확인
     * 2. reason이 비어있지 않은지 확인
     * 3. 같은 유저가 같은 대상을 이미 신고했는지 중복 체크
     *
     * @param report reporterId, targetType, targetId, reason 필수
     * @throws BusinessException BAD_REQUEST — targetType/reason 유효하지 않을 때
     * @throws BusinessException DUPLICATE_REPORT — 이미 신고한 대상일 때
     */
    @Transactional
    public void createReport(Report report) {

        // targetType 유효성 검증
        if (!"REVIEW".equals(report.getTargetType()) && !"TOILET".equals(report.getTargetType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // reason 빈값 체크
        if (report.getReason() == null || report.getReason().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        // 중복 신고 체크
        int count = reportMapper.countByReporterAndTarget(
                report.getReporterId(),
                report.getTargetType(),
                report.getTargetId()
        );
        if (count > 0) {
            throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
        }

        reportMapper.insertReport(report);
    }

    /**
     * 내 신고 목록 조회 (최신순)
     */
    public List<Report> getMyReports(Long reporterId) {
        return reportMapper.findByReporterId(reporterId);
    }

    /**
     * 신고 상세 조회
     *
     * @throws BusinessException REPORT_NOT_FOUND — 신고가 존재하지 않을 때
     */
    public Report getReportDetail(Long id) {
        Report report = reportMapper.findById(id);
        if (report == null) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }
        return report;
    }
}
