package com.jsl26tp.jsl26tp.auth.controller;

import com.jsl26tp.jsl26tp.auth.domain.User;
import com.jsl26tp.jsl26tp.auth.service.UserService;
import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.inquiry.service.InquiryService;
import com.jsl26tp.jsl26tp.review.service.ReviewService;
import com.jsl26tp.jsl26tp.toilet.service.RecentViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/mypage")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RecentViewService recentViewService;
    private final ReviewService reviewService;
    private final InquiryService inquiryService;

    // 1. 마이페이지 메인 (GET /mypage)
    @GetMapping
    public String index(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // Security에서 가져온 username으로 유저 정보 조회
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        return "mypage/index"; // templates/mypage/index.html
    }

    // 2. 프로필 수정 폼 (GET /mypage/edit)
    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("user", user);
        return "mypage/edit";
    }

    // 3. 프로필 수정 처리 (POST /mypage/edit)
    @PostMapping("/edit")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails, User updateData) {
        User user = userService.findByUsername(userDetails.getUsername());
        updateData.setId(user.getId());
        userService.updateUser(updateData);
        return "redirect:/mypage";
    }

    // 4. 비밀번호 변경 폼 (GET /mypage/password)
    @GetMapping("/password")
    public String passwordForm() {
        return "mypage/password";
    }

    // 5. 비밀번호 변경 처리 (POST /mypage/password)
    @PostMapping("/password")
    @ResponseBody
    public ApiResponse<Void> updatePassword(@AuthenticationPrincipal UserDetails userDetails,
                                            @RequestParam String currentPassword,
                                            @RequestParam String newPassword) {
        User user = userService.findByUsername(userDetails.getUsername());
        userService.updatePassword(user.getId(), currentPassword, newPassword);
        return ApiResponse.ok(null);
    }

    // 6. 최근 본 화장실 (GET /mypage/recent)
    @GetMapping("/recent")
    public String recent(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        // 1. 현재 로그인한 유저 정보 가져오기
        User user = userService.findByUsername(userDetails.getUsername());

        // 2. 유저 ID로 최근 본 화장실 목록 조회해서 모델에 담기
        model.addAttribute("recentToilets", recentViewService.findByUserId(user.getId()));

        return "mypage/recent"; // templates/mypage/recent.html
    }

    // 7. 내가 쓴 리뷰 (GET /mypage/reviews)
    @GetMapping("/reviews")
    public String myReviews(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("reviews", reviewService.findByUserId(user.getId()));
        return "mypage/reviews";
    }

    // 8. 내 문의 내역 (GET /mypage/inquiries)
    @GetMapping("/inquiries")
    public String myInquiries(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("inquiries", inquiryService.findByWriterId(user.getId()));
        return "mypage/inquiries";
    }
}