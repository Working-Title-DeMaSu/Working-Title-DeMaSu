package com.jsl26tp.jsl26tp.toilet.service;

import com.jsl26tp.jsl26tp.toilet.domain.RecentView;
import com.jsl26tp.jsl26tp.toilet.mapper.RecentViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentViewService {

    private final RecentViewMapper recentViewMapper;

    // 최근 본 화장실 목록 조회
    public List<RecentView> findByUserId(Long userId) {
        return recentViewMapper.findByUserId(userId);
    }

    // 조회 기록 전체 삭제
    public void deleteByUserId(Long userId) {
        recentViewMapper.deleteByUserId(userId);
    }
}