package com.jsl26tp.jsl26tp.inquiry.controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.config.CustomUserDetails;
import com.jsl26tp.jsl26tp.inquiry.domain.Inquiry;
import com.jsl26tp.jsl26tp.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Inquiry Controller (User-facing)
 * - User inquiry CRUD API
 * - Admin answer handling is in AdminController
 */
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    // Create inquiry - POST /api/inquiries
    @PostMapping
    public ApiResponse<Void> createInquiry(
            @RequestBody Inquiry inquiry,
            @AuthenticationPrincipal CustomUserDetails user) {

        inquiry.setWriterId(user.getId());
        inquiryService.createInquiry(inquiry);
        return ApiResponse.ok();
    }

    // Get my inquiries - GET /api/inquiries/my
    @GetMapping("/my")
    public ApiResponse<List<Inquiry>> getMyInquiries(
            @AuthenticationPrincipal CustomUserDetails user) {

        List<Inquiry> inquiries = inquiryService.findByWriterId(user.getId());
        return ApiResponse.ok(inquiries);
    }

    // Get inquiry detail - GET /api/inquiries/{id}
    @GetMapping("/{id}")
    public ApiResponse<Inquiry> getInquiryDetail(@PathVariable Long id) {

        Inquiry inquiry = inquiryService.getInquiryDetail(id);
        return ApiResponse.ok(inquiry);
    }

    // Delete inquiry (soft delete, owner only) - DELETE /api/inquiries/{id}
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInquiry(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        inquiryService.deleteInquiry(id, user.getId());
        return ApiResponse.ok();
    }
}
