package com.jsl26tp.jsl26tp.toilet.Controller;

import com.jsl26tp.jsl26tp.common.ApiResponse;
import com.jsl26tp.jsl26tp.config.CustomUserDetails;
import com.jsl26tp.jsl26tp.toilet.service.ToiletService;
import com.jsl26tp.jsl26tp.toilet.domain.Toilet;
import com.jsl26tp.jsl26tp.toilet.domain.ToiletTag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ToiletController {

    private final ToiletService toiletService;

    // ========== 페이지 렌더링 ==========

    // 화장실 추가 폼 (GET /toilet/add)
    @GetMapping("/toilet/add")
    public String addForm() {
        return "toilet/add";
    }

    // 화장실 추가 처리 (POST /toilet/add)
    @PostMapping("/toilet/add")
    public String addToilet(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam String name,
            @RequestParam String address,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false, defaultValue = "0") Integer is24hours,
            @RequestParam(required = false) String openTime,
            @RequestParam(required = false) String closeTime,
            @RequestParam(required = false, defaultValue = "0") Integer isWheelchair,
            @RequestParam(required = false, defaultValue = "0") Integer hasPaper,
            @RequestParam(required = false, defaultValue = "0") Integer hasSoap,
            @RequestParam(required = false, defaultValue = "0") Integer hasSanitary,
            @RequestParam(required = false, defaultValue = "0") Integer hasEmergency,
            @RequestParam(required = false, defaultValue = "WESTERN") String toiletType,
            @RequestParam(required = false) List<String> tags) {

        Toilet toilet = new Toilet();
        toilet.setName(name);
        toilet.setAddress(address);
        toilet.setLatitude(latitude);
        toilet.setLongitude(longitude);
        toilet.setIs24hours(is24hours);
        toilet.setIsWheelchair(isWheelchair);
        toilet.setHasPaper(hasPaper);
        toilet.setHasSoap(hasSoap);
        toilet.setHasSanitary(hasSanitary);
        toilet.setHasEmergency(hasEmergency);
        toilet.setToiletType(toiletType);

        // 영업시간 조합 (is24hours가 0일 때만)
        if (is24hours == 0 && openTime != null && !openTime.isBlank()
                && closeTime != null && !closeTime.isBlank()) {
            toilet.setOpenHours(openTime + "-" + closeTime);
        }

        toiletService.insertToiletWithTags(toilet, userDetails.getId(), tags);

        return "redirect:/toilet/add?success=true";
    }

    // ========== API ==========

    //주변 화장실 검색 (위도, 경도, 반경)
    @GetMapping("/api/toilets")
    @ResponseBody
    public ApiResponse<List<Toilet>> getNearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "500") int radius) {
        List<Toilet> toilets = toiletService.findNearby(lat, lng, radius);
        return ApiResponse.ok(toilets);
    }

    //필터 검색 (24시간, 장애인, 기저귀, 비상벨 등)
    @GetMapping("/api/toilets/filter")
    @ResponseBody
    public ApiResponse<List<Toilet>> filter(
            @RequestParam double lat, @RequestParam double lng,
            @RequestParam(defaultValue = "500") int radius,
            @RequestParam(required = false) Integer is24hours,
            @RequestParam(required = false) Integer isWheelchair,
            @RequestParam(required = false) Integer hasDiaper,
            @RequestParam(required = false) Integer hasEmergency,
            @RequestParam(required = false) Integer hasPaper,
            @RequestParam(required = false) Integer hasSanitary) {

        List<Toilet> toilets = toiletService.findByFilter(lat, lng, radius,
                                                    is24hours, isWheelchair, hasDiaper,
                                                    hasEmergency, hasPaper, hasSanitary);
        return ApiResponse.ok(toilets);
    }

    //키워드 검색 (이름, 주소)
    @GetMapping("/api/toilets/search")
    @ResponseBody
    public ApiResponse<List<Toilet>> search(@RequestParam String keyword) {
       List<Toilet> toilets = toiletService.searchByKeyword(keyword);
       return ApiResponse.ok(toilets);
    }

    //화장실 상세 조회 (평균점수 + 리뷰수 포함)
    @GetMapping("/api/toilets/{id}")
    @ResponseBody
    public ApiResponse<Toilet> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails user) {

        Long userId = (user != null) ? user.getId() : null;
        Toilet toilet = toiletService.getDetail(id, userId);
        return ApiResponse.ok(toilet);
    }

    //태그 조회
    @GetMapping("/api/toilets/{id}/tag")
    @ResponseBody
    public ApiResponse<List<ToiletTag>> getTags(@PathVariable Long id) {
        List<ToiletTag> tags = toiletService.getTags(id);
        return ApiResponse.ok(tags);
    }

    //새 화장실 등록 (사용자 제보)
    @PostMapping("/api/toilets")
    @ResponseBody
    public ApiResponse<Void> newToilet(
            @RequestBody Toilet toilet,
            @AuthenticationPrincipal CustomUserDetails user) {

        toiletService.insertToilet(toilet, user.getId());
        return ApiResponse.ok();
    }

    //정보 수정 제안
    @PostMapping("/api/toilets/{id}/edit-request")
    @ResponseBody
    public ApiResponse<Void> editRequest(
                @PathVariable Long id,
                @RequestBody Map<String, String> body,
                @AuthenticationPrincipal CustomUserDetails user){

        String category = body.get("category");
        String content = body.get("content");
        toiletService.submitEditRequest(id, user.getId(), content);
        return ApiResponse.ok();
    }
}
