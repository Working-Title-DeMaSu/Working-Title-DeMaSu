package com.jsl26tp.jsl26tp.toilet.Controller;

import com.jsl26tp.jsl26tp.config.CustomUserDetails;
import com.jsl26tp.jsl26tp.toilet.domain.Toilet;
import com.jsl26tp.jsl26tp.toilet.service.ToiletService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ToiletPageController {

    private final ToiletService toiletService;

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
}
