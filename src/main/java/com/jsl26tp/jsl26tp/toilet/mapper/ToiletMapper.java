package com.jsl26tp.jsl26tp.toilet.mapper;

import com.jsl26tp.jsl26tp.toilet.domain.Toilet;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

@Mapper
public interface ToiletMapper {

    // 주변 화장실 검색 (위도, 경도, 반경)
    List<Toilet> findNearby(@Param("lat") double lat,
                            @Param("lng") double lng,
                            @Param("radius") int radius);

    // 필터 검색 (24시간, 장애인, 기저귀, 비상벨 등)
    List<Toilet> findByFilter(Map<String, Object> params);

    // 키워드 검색 (이름, 주소)
    List<Toilet> findByKeyword(@Param("keyword") String keyword);

    // 화장실 기본 조회 (존재 여부만 체크)
    Toilet findById(@Param("id") Long id);

    // 화장실 상세 조회 (평균점수 + 리뷰수 포함)
    Toilet findDetailById(@Param("id") Long id);

    // 새 화장실 등록 (사용자 제보)
    void insertToilet(Toilet toilet);

    // 화장실 정보 수정 (관리자)
    void updateToilet(Toilet toilet);

    // 화장실 삭제 (소프트 삭제)
    void deleteToilet(@Param("id") Long id);

    // 승인 대기 화장실 목록 (관리자용)
    List<Toilet> findPendingToilets();

    // 화장실 상태 변경 (승인/거절)
    void updateStatus(@Param("id") Long id, @Param("status") String status);

    // 전체 화장실 목록 (관리자용)
    List<Toilet> findAllToilets();
}
