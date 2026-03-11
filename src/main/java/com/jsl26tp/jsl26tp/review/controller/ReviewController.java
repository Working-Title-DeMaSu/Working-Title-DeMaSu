package com.jsl26tp.jsl26tp.review.controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.review.domain.Review;
import com.jsl26tp.jsl26tp.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/review")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /*
     * 리뷰 페이지 이동
     */

    @GetMapping("/write")
    public String writePage() {
        return "review/write";
    }


    /*
     * 리뷰 작성
     * http://localhost:8090/review/api/write
     */
    @PostMapping("/api/write")
    @ResponseBody
    public ApiResponse<String> writeReview(
            @ModelAttribute Review review,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        reviewService.writeReview(review, files);

        return ApiResponse.ok("OK");
    }

    /*
     * 리뷰 상세 조회
     * http://localhost:8090/review/api/{id}
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public ApiResponse<Review> getReviewDetail(@PathVariable("id") Long id) {

        // 서비스 호출 (리뷰 정보 + 작성자 닉네임 + 이미지 리스트가 담긴 객체 반환)
        Review review = reviewService.getReviewDetail(id);

        return ApiResponse.ok(review);
    }

    /*
     * 리뷰 수정
     * http://localhost:8090/review/api/update
     */
    @PostMapping("/api/update")
    @ResponseBody
    public ApiResponse<String> updateReview(
            @ModelAttribute Review review,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) {

        reviewService.updateReview(review, files);

        return ApiResponse.ok("OK");
    }

    /*
     * 리뷰 삭제
     * http://localhost:8090/review/api/{id}/delete
     */
    @PostMapping("/api/{id}/delete")
    @ResponseBody
    public ApiResponse<String> deleteReview(@PathVariable("id") Long id) {

        reviewService.deleteReview(id);

        return ApiResponse.ok("OK");
    }

    /*
     * 특정 화장실의 리뷰 목록 조회
     * /api/review/api/toilet/{toiletId}
     */
    @GetMapping("/api/toilet/{toiletId}")
    @ResponseBody
    public ApiResponse<List<Review>> getReviewsByToilet(@PathVariable("toiletId") Long toiletId) {
        return ApiResponse.ok(reviewService.getReviewsByToilet(toiletId));
    }

    /*
     * 화장실별 평균 청결도 점수
     * /api/review/api/toilet/{toiletId}/avg
     */
    @GetMapping("/api/toilet/{toiletId}/avg")
    @ResponseBody
    public ApiResponse<Double> getAvgScore(@PathVariable("toiletId") Long toiletId) {
        return ApiResponse.ok(reviewService.getAverageScore(toiletId));
    }

    /*
     * 화장실별 총 리뷰 개수
     * /api/review/api/toilet/{toiletId}/cnt
     */
    @GetMapping("/api/toilet/{toiletId}/cnt")
    @ResponseBody
    public ApiResponse<Integer> getReviewCount(@PathVariable("toiletId") Long toiletId) {
        return ApiResponse.ok(reviewService.getReviewCount(toiletId));
    }

    /*
     * 내가 쓴 리뷰 목록 조회
     * /api/review/api/mypage/{userId}
     */
    @GetMapping("/api/mypage/{userId}")
    @ResponseBody
    public ApiResponse<List<Review>> getMyReviews(@PathVariable("userId") Long userId) {
        return ApiResponse.ok(reviewService.getMyReviews(userId));
    }

}
