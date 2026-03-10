package com.jsl26tp.jsl26tp.toilet.Controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.config.CustomUserDetails;
import com.jsl26tp.jsl26tp.toilet.service.ToiletService;
import com.jsl26tp.jsl26tp.toilet.domain.Toilet;
import com.jsl26tp.jsl26tp.toilet.domain.ToiletTag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/toilets")
@RequiredArgsConstructor
public class ToiletController {

    private final ToiletService toiletService;

    //주변 화장실 검색 (위도, 경도, 반경)
    @GetMapping
    public ApiResponse<List<Toilet>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "500") int radius) {
        List<Toilet> toilets = toiletService.findNearby(lat, lng, radius);
        return ApiResponse.ok(toilets);
    }

    //필터 검색 (24시간, 장애인, 기저귀, 비상벨 등)
    @GetMapping("/filter")
    public ApiResponse<List<Toilet>> filter(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(defaultValue = "500") int radius,
            @RequestParam(required = false) Integer is24hours,
            @RequestParam(required = false) Integer isWeelchair,
            @RequestParam(required = false) Integer hasDiaper,
            @RequestParam(required = false) Integer hasEmergency,
            @RequestParam(required = false) Integer hasPaper,
            @RequestParam(required = false) Integer hasSanitary) {

        List<Toilet> toilets = toiletService.findByFilter(lat, lng, radius,
                                                    is24hours, isWeelchair, hasDiaper,
                                                    hasEmergency, hasPaper, hasSanitary);
        return ApiResponse.ok(toilets);

    }

    //키워드 검색 (이름, 주소)
    //Get /api/toilets/search?keyword=강남
    @GetMapping("/search")
    public ApiResponse<List<Toilet>> search(@RequestParam String keyword) {
       List<Toilet> toilets = toiletService.searchByKeyword(keyword);
       return ApiResponse.ok(toilets);
    }

    //화장실 상세 조회 (평균점수 + 리뷰수 포함)
    @GetMapping("/{id}")
    public ApiResponse<Toilet> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        //로그인 안 했으면 user = null -> userId = null
        Long userId = (user != null) ? user.getId() : null;
        Toilet toilet = toiletService.getDetail(id, userId);
        return ApiResponse.ok(toilet);
    }

    //태그 조회
    @GetMapping("/{id}/tag")
    public ApiResponse<List<ToiletTag>> getTags(@PathVariable Long id) {
        List<ToiletTag> tags = toiletService.getTags(id);
        return ApiResponse.ok(tags);
    }

    //새 화장실 등록 (사용자 제보)
    @PostMapping
    public ApiResponse<Void> newToilet(
            @RequestBody Toilet toilet,
            @AuthenticationPrincipal CustomUserDetails user) {

        toiletService.insertToilet(toilet, user.getId());
        return ApiResponse.ok();
    }


    //정보 수정 제안
    @GetMapping("/{id}/edit-request")
    public ApiResponse<Void> editRequest(
                @PathVariable Long id,
                @RequestBody Map<String, String> body,
                @AuthenticationPrincipal CustomUserDetails user){

        String content = body.get("content");
        toiletService.submitEditRequest(id, user.getId(), content);
        return ApiResponse.ok();
    }
}
