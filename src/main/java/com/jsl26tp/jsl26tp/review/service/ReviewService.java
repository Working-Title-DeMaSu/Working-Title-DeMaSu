package com.jsl26tp.jsl26tp.review.service;

import com.jsl26tp.jsl26tp.common.BusinessException;
import com.jsl26tp.jsl26tp.common.ErrorCode;
import com.jsl26tp.jsl26tp.review.domain.Review;
import com.jsl26tp.jsl26tp.review.domain.ReviewImage;
import com.jsl26tp.jsl26tp.review.mapper.ReviewImageMapper;
import com.jsl26tp.jsl26tp.review.mapper.ReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private final ReviewMapper reviewMapper;
    private final ReviewImageMapper reviewImageMapper;

    @Value("${file.path.review}")
    private String reviewPath;

    /*
     * 리뷰 게시
     */

    public void writeReview(Review review, List<MultipartFile> files) {

        
        // 본문 저장
        reviewMapper.insertReview(review);


        // 파일 처리
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    saveFileToReview(review, file); //
                }
            }
        }
    }

    /*
     * 파일 저장
     */
    public void saveFileToReview(Review review, MultipartFile file) {
        try {
            
            // 폴더 생성
            File uploadDir = new File(reviewPath);

            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            String originalName = file.getOriginalFilename();
            String uniqueName = UUID.randomUUID() + "_" + originalName;

            Path savePath = Paths.get(reviewPath, uniqueName);
            Files.copy(file.getInputStream(), savePath);

            ReviewImage image = new ReviewImage();
            image.setReviewId(review.getId());
            image.setImageUrl("/upload/review/" + uniqueName);

            reviewImageMapper.insertImage(image);

        }catch(IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    /*
     * 리뷰 상세 조회
     */

    public Review getReviewDetail(Long id) {
        // 리뷰 정보 + 작성자 닉네임 가져오기
        Review review = reviewMapper.findById(id);

        // 예외 처리: 만약 해당 리뷰가 존재하지 않거나 삭제된 상태라면
        if (review == null || "HIDDEN".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND); //
        }

        // 해당 리뷰 이미지 리스트 가져와서 객체에 담기
        List<ReviewImage> images = reviewImageMapper.findByReviewId(id);
        review.setImages(images);

        return review;
    }

    /*
     * 리뷰 수정
     */

    public void updateReview(Review review, List<MultipartFile> files) {
        // 본문 수정
        reviewMapper.updateReview(review);

        // 새로운 이미지 파일이 넘어 온 경우
        if (files != null && !files.isEmpty() && !files.get(0).isEmpty()) {

            // 기존 이미지 정보 삭제
            reviewImageMapper.deleteByReviewId(review.getId());

            // 새로운 이미지 업데이트
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    saveFileToReview(review, file);
                }
            }
        }
    }

    /*
     * 리뷰 삭제 (소프트 삭제)
     */
    public void deleteReview(Long id) {

        // 조회
        Review review = reviewMapper.findById(id);

        // 예외 처리: 만약 해당 리뷰가 존재하지 않거나 삭제된 상태라면
        if (review == null || "HIDDEN".equals(review.getStatus())) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_FOUND); //
        }

        reviewMapper.deleteReview(id);
    }

    /*
     * 특정 화장실의 리뷰 목록 조회
     */

    public List<Review> getReviewsByToilet(Long toiletId) {
        // 리뷰 본문 + 유저 정보 목록 가져오기
        List<Review> reviews = reviewMapper.findByToiletId(toiletId);

        // 각 리뷰마다 달린 이미지 미리보기
        for (Review review : reviews) {
            List<ReviewImage> images = reviewImageMapper.findByReviewId(review.getId());
            review.setImages(images);
        }
        return reviews;
    }

    /*
     * 화장실별 평균 청결도 점수
     */

    public Double getAverageScore(Long toiletId) {
        Double avgScore = reviewMapper.getAvgScore(toiletId);
        return avgScore != null ? avgScore : 0.0;
    }

    /*
     * 화장실별 총 리뷰 개수
     */

    public Integer getReviewCount(Long toiletId) {
        Integer count = reviewMapper.getReviewCount(toiletId);
        return count != null ? count : 0;
    }

    /*
     * 내가 쓴 리뷰 목록 조회
     */

    public List<Review> getMyReviews(Long userId) {
        return reviewMapper.findByUserId(userId);
    }


}
