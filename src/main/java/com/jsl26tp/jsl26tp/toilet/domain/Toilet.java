package com.jsl26tp.jsl26tp.toilet.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Toilet {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private String openHours;
    private Integer is24hours;
    private Integer isWheelchair;
    private Integer hasPaper;
    private Integer hasSoap;
    private Integer hasSanitary;
    private Integer hasDiaper;
    private String toiletType;       // WESTERN, EASTERN
    private Integer hasEmergency;
    private String phone;
    private Integer hasCctv;
    private Integer maleToiletCount;
    private Integer maleUrinalCount;
    private Integer femaleToiletCount;
    private String source;           // PUBLIC_API, USER
    private String status;           // APPROVED, PENDING, REJECTED
    private Long byUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // DB 컬럼 아닌 추가 필드 (검색 결과용)
    private Double distance;         // 현재 위치와의 거리
    private Double avgScore;         // 평균 청결도 점수
    private Integer reviewCount;     // 리뷰 수
}
