package com.jsl26tp.jsl26tp.report.mapper;

import com.jsl26tp.jsl26tp.report.domain.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ReportMapper {

    // 신고 등록
    void insertReport(Report report);

    // 신고 목록 (관리자용)
    List<Report> findAllReports();

    // 대기 중인 신고 목록
    List<Report> findPendingReports();

    // 신고 상세 조회
    Report findById(@Param("id") Long id);

    // 신고 상태 변경 (처리/기각)
    void updateStatus(@Param("id") Long id,
                      @Param("status") String status,
                      @Param("adminNote") String adminNote);

    // 특정 사용자의 신고 횟수 (다수 신고 접수 확인용)
    Integer countByTargetUser(@Param("targetType") String targetType,
                              @Param("targetId") Long targetId);

    // 중복 신고 체크 (같은 유저가 같은 대상을 이미 신고했는지)
    int countByReporterAndTarget(@Param("reporterId") Long reporterId,
                                 @Param("targetType") String targetType,
                                 @Param("targetId") Long targetId);

    // 내 신고 목록 조회
    List<Report> findByReporterId(@Param("reporterId") Long reporterId);
}
