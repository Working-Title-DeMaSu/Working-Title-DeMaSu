package com.jsl26tp.jsl26tp.auth.controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.common.BusinessException;
import com.jsl26tp.jsl26tp.auth.domain.User;
import com.jsl26tp.jsl26tp.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @Value("${naver.maps.client-id}")
    private String naverMapsClientId;

    // 로그인 페이지 (모달 방식이므로 메인으로 리다이렉트, error/logout 파라미터 전달)
    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error,
                        @RequestParam(value = "logout", required = false) String logout,
                        Model model) {
        // index.html에서 Naver Maps API 로드에 필요
        model.addAttribute("naverMapsClientId", naverMapsClientId);
        if (error != null) {
            model.addAttribute("loginError", true);
        }
        if (logout != null) {
            model.addAttribute("logoutSuccess", true);
        }
        return "index";
    }

    // 회원가입 페이지 (새 창으로 이동)
    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    // 회원가입 처리
    @PostMapping("/register")
    public String register(@ModelAttribute User user,
                           @RequestParam("passwordConfirm") String passwordConfirm,
                           RedirectAttributes redirectAttributes,
                           Model model) {

        // 비밀번호 확인 일치 여부
        if (!user.getPassword().equals(passwordConfirm)) {
            model.addAttribute("error", "パスワードが一致しません。");
            model.addAttribute("user", user);
            return "auth/register";
        }

        // 회원가입 실행 (검증은 UserService에서 BusinessException으로 처리)
        try {
            userService.register(user);
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("user", user);
            return "auth/register";
        }

        redirectAttributes.addFlashAttribute("registerSuccess", true);
        return "redirect:/login";
    }

    // ===== Ajax API (ApiResponse 통일) =====

    // username 중복 체크 API
    @GetMapping("/api/check-username")
    @ResponseBody
    public ApiResponse<Boolean> checkUsername(@RequestParam String username) {
        boolean available = !userService.isUsernameTaken(username);
        return ApiResponse.ok(available);
    }

    // 닉네임 중복 체크 API
    @GetMapping("/api/check-nickname")
    @ResponseBody
    public ApiResponse<Boolean> checkNickname(@RequestParam String nickname) {
        boolean available = !userService.isNicknameTaken(nickname);
        return ApiResponse.ok(available);
    }

    // 이메일 중복 체크 API
    @GetMapping("/api/check-email")
    @ResponseBody
    public ApiResponse<Boolean> checkEmail(@RequestParam String email) {
        boolean available = !userService.isEmailTaken(email);
        return ApiResponse.ok(available);
    }
}
