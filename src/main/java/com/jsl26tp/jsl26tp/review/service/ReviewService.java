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
}
