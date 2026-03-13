package com.jsl26tp.jsl26tp.admin.domain;

import lombok.Getter;
import java.util.List;

/**
 * Admin 전용 페이징 응답 클래스
 *
 * 공통 PageResponse<T>는 common/ 소속이라 수정 불가.
 * dashboard.html의 JS가 기대하는 필드명에 맞춰 admin 전용으로 별도 정의.
 *
 * HTML (dashboard.html) 매핑:
 *   res.data.content    → items 대신 content 필드명 사용
 *   res.data.number     → page  대신 number 필드명 사용 (0-based)
 *   res.data.totalPages → 공통과 동일
 */
@Getter
public class AdminPageResponse<T> {

    /** 현재 페이지 데이터 목록 (HTML: res.data.content) */
    private final List<T> content;

    /** 현재 페이지 번호 0-based (HTML: res.data.number) */
    private final int number;

    /** 페이지당 표시 건수 */
    private final int size;

    /** 전체 데이터 건수 */
    private final long totalCount;

    /** 전체 페이지 수 (HTML: res.data.totalPages) */
    private final int totalPages;

    public AdminPageResponse(List<T> content, int number, int size, long totalCount, int totalPages) {
        this.content    = content;
        this.number     = number;
        this.size       = size;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
    }
}
