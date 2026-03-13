package com.jsl26tp.jsl26tp.inquiry.service;

import com.jsl26tp.jsl26tp.common.BusinessException;
import com.jsl26tp.jsl26tp.common.ErrorCode;
import com.jsl26tp.jsl26tp.inquiry.domain.Inquiry;
import com.jsl26tp.jsl26tp.inquiry.mapper.InquiryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

/**
 * Inquiry Service (User-facing)
 * - Handles inquiry create / read / delete
 * - Admin answer handling is in AdminService
 */
@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryMapper inquiryMapper;

    // Get my inquiry list
    public List<Inquiry> findByWriterId(Long userId) {
        return inquiryMapper.findByWriterId(userId);
    }

    // Create inquiry (validates title and content)
    @Transactional
    public void createInquiry(Inquiry inquiry) {

        if (inquiry.getTitle() == null || inquiry.getTitle().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        if (inquiry.getContent() == null || inquiry.getContent().trim().isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }

        inquiryMapper.insertInquiry(inquiry);
    }

    // Get inquiry detail
    public Inquiry getInquiryDetail(Long id) {
        Inquiry inquiry = inquiryMapper.findById(id);
        if (inquiry == null) {
            throw new BusinessException(ErrorCode.INQUIRY_NOT_FOUND);
        }
        return inquiry;
    }

    // Soft delete inquiry (owner only)
    @Transactional
    public void deleteInquiry(Long id, Long writerId) {
        Inquiry inquiry = inquiryMapper.findById(id);

        if (inquiry == null) {
            throw new BusinessException(ErrorCode.INQUIRY_NOT_FOUND);
        }

        if (!inquiry.getWriterId().equals(writerId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        inquiryMapper.deleteInquiry(id);
    }
}
