package com.jsl26tp.jsl26tp.toilet.service;

import com.jsl26tp.jsl26tp.common.BusinessException;
import com.jsl26tp.jsl26tp.common.ErrorCode;
import com.jsl26tp.jsl26tp.toilet.domain.Toilet;
import com.jsl26tp.jsl26tp.toilet.domain.ToiletEditRequest;
import com.jsl26tp.jsl26tp.toilet.domain.ToiletTag;
import com.jsl26tp.jsl26tp.toilet.mapper.RecentViewMapper;
import com.jsl26tp.jsl26tp.toilet.mapper.ToiletEditRequestMapper;
import com.jsl26tp.jsl26tp.toilet.mapper.ToiletMapper;
import com.jsl26tp.jsl26tp.toilet.mapper.ToiletTagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor // ← final 필드를 자동으로 생성자 주입
public class ToiletService {

    private final ToiletMapper toiletMapper;
    private final RecentViewMapper recentViewMapper;
    private final ToiletTagMapper toiletTagMapper;
    private final ToiletEditRequestMapper toiletEditRequestMapper;

    //주변 화장실 검색 (위도, 경도, 반경)
    public List<Toilet> findNearby(double lat, double lng, int radius) {
        return toiletMapper.findNearby(lat, lng, radius);
    }

    //필터 검색 (24시간, 장애인, 기저귀, 비상벨 등)
    public List<Toilet> findByFilter(double lat, double lng, int radius,
                                     Integer is24hours, Integer isWheelchair,
                                     Integer hasDiaper, Integer hasEmergency,
                                     Integer hasPaper, Integer hasSanitary) {

        Map<String, Object> params = new HashMap<>();
        params.put("lat", lat);
        params.put("lng", lng);
        params.put("radius", radius);
        params.put("is24hours", is24hours);
        params.put("isWheelchair", isWheelchair);
        params.put("hasDiaper", hasDiaper);
        params.put("hasEmergency", hasEmergency);
        params.put("hasPaper", hasPaper);
        params.put("hasSanitary", hasSanitary);
        return toiletMapper.findByFilter(params);
    }

    //키워드 검색 (이름, 주소)
    public List<Toilet> searchByKeyword(String keyword) {
        return toiletMapper.findByKeyword(keyword);
    }

    //화장실 상세 조회 (시설정보 포함)
    //     → 평균점수, 리뷰수도 한번에 가져옴
    //     → 로그인 유저면 최근조회 자동 저장
    public Toilet getDetail(Long id, Long userId) {

        //화장실 존재 여부
        Toilet toilet = toiletMapper.findDetailById(id);

        if(toilet == null) {
            throw new BusinessException(ErrorCode.TOILET_NOT_FOUND);
        }

       //로그인 유저면 최근조회 자동 저장
        if(userId != null) {
            recentViewMapper.upsertView(userId, id);
        }

        return toilet;
    }

    //태그 조회
    public List<ToiletTag> getTags(Long toiletId) {

        //화장실 존재 여부
        Toilet toilet = toiletMapper.findById(toiletId);
        if(toilet == null) {
            throw new BusinessException(ErrorCode.TOILET_NOT_FOUND);
        }

        return toiletTagMapper.findByToiletId(toiletId);
    }

    //새 화장실 등록 (사용자 제보)
    public void insertToilet(Toilet toilet, Long userId) {
            toilet.setSource("USER");
            toilet.setStatus("PENDING");
            toilet.setByUserId(userId);
            toiletMapper.insertToilet(toilet);
    }

    //새 화장실 등록 + 태그 한번에 (페이지 폼용)
    public void insertToiletWithTags(Toilet toilet, Long userId, List<String> tagNames) {
        toilet.setSource("USER");
        toilet.setStatus("PENDING");
        toilet.setByUserId(userId);
        toiletMapper.insertToilet(toilet); // useGeneratedKeys로 toilet.id 자동 설정됨

        // 태그 등록
        if (tagNames != null) {
            for (String tagName : tagNames) {
                if (tagName != null && !tagName.isBlank()) {
                    ToiletTag tag = new ToiletTag();
                    tag.setToiletId(toilet.getId());
                    tag.setTagName(tagName);
                    toiletTagMapper.insertTag(tag);
                }
            }
        }
    }

    //정보 수정 제안
    public void submitEditRequest(Long toiletId, Long userId, String content) {

            //화장실 존재 여부
            Toilet toilet = toiletMapper.findById(toiletId);
            if(toilet == null) {
                throw new BusinessException(ErrorCode.TOILET_NOT_FOUND);
            }

            ToiletEditRequest request = new ToiletEditRequest();
            request.setToiletId(toiletId);
            request.setUserId(userId);
            request.setContent(content);
            toiletEditRequestMapper.insertRequest(request);
    }
}
