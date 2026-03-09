package com.jsl26tp.jsl26tp.review.controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.review.domain.Review;
import com.jsl26tp.jsl26tp.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /*
     * 리뷰 작성
     * http://localhost:8090/api/reviews/write
     */
    @PostMapping("/write")
    public ApiResponse<String> writeReview(
            @ModelAttribute Review review,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        reviewService.writeReview(review, files);

        return ApiResponse.ok("OK");
    }

    /*
     * 리뷰 상세 조회
     * http://localhost:8090/api/reviews/{id}
     */
    @GetMapping("/{id}")
    public ApiResponse<Review> getReviewDetail(@PathVariable("id") Long id) {

        // 서비스 호출 (리뷰 정보 + 작성자 닉네임 + 이미지 리스트가 담긴 객체 반환)
        Review review = reviewService.getReviewDetail(id);

        return ApiResponse.ok(review);
    }

    /*
     * 리뷰 수정
     * http://localhost:8090/api/reviews/update
     */
    @PostMapping("/update")
    public ApiResponse<String> updateReview(
            @ModelAttribute Review review,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        reviewService.updateReview(review, files);

        return ApiResponse.ok("OK");
    }

    /*
     * 리뷰 삭제
     * http://localhost:8090/api/reviews/{id}/delete
     */
    @PostMapping("/{id}/delete")
    public ApiResponse<String> deleteReview(@PathVariable("id") Long id) {

        reviewService.deleteReview(id);

        return ApiResponse.ok("OK");
    }

    /*
     * 특정 화장실의 리뷰 목록 조회
     * /api/reviews/toilet/{toiletId}
     */
    @GetMapping("/toilet/{toiletId}")
    public ApiResponse<List<Review>> getReviewsByToilet(@PathVariable("toiletId") Long toiletId) {
        return ApiResponse.ok(reviewService.getReviewsByToilet(toiletId));
    }

}
