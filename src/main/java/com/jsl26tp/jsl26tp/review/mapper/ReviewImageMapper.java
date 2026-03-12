package com.jsl26tp.jsl26tp.review.mapper;

import com.jsl26tp.jsl26tp.review.domain.ReviewImage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ReviewImageMapper {

    // 리뷰의 이미지 목록
    List<ReviewImage> findByReviewId(@Param("reviewId") Long reviewId);

    // 이미지 등록
    void insertImage(ReviewImage image);

    // 이미지 삭제
    void deleteByReviewId(@Param("reviewId") Long reviewId);
}
