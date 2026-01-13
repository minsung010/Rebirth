package com.rebirth.my.donation;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import com.rebirth.my.auth.CustomOAuth2User;
import com.rebirth.my.auth.CustomUserDetails;
import com.rebirth.my.domain.MarketVo;
import com.rebirth.my.domain.User;
import com.rebirth.my.mapper.UserMapper;

@Controller
@RequestMapping("/donation")
public class DonationController {

    @Autowired
    private DonationService donationService;

    @Autowired
    private UserMapper userMapper;

    @Value("${kakao.maps.appkey}")
    private String kakaoMapsAppKey;

    // 1. 기부 안내 페이지
    @GetMapping("/guide")
    public String guide(Model model, Principal principal) {
        // API Key 전달
        model.addAttribute("kakaoMapsAppKey", kakaoMapsAppKey);

        Long userId = getUserIdFromPrincipal(principal);
        if (userId != null) {
            User user = userMapper.getUserById(userId);
            if (user != null && user.getAddress() != null && !user.getAddress().isEmpty()) {
                // 주소 전처리 (지번/도로명 주소 앞부분만 사용하면 더 정확할 수 있으나 일단 전체 사용)
                model.addAttribute("userAddress", user.getAddress());
            }
        }
        return "donation/guide";
    }

    // 2. 기부 기록하기 페이지 (내 옷장 패치)
    @GetMapping("/record")
    public String recordForm(Model model, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return "redirect:/auth/login";
        }

        List<MarketVo> myClothes = donationService.getClosetItems(userId);
        model.addAttribute("myClothes", myClothes);

        return "donation/record";
    }

    // 3. 기부 처리 (상태 변경)
    @PostMapping("/complete")
    public String completeDonation(@RequestParam List<Long> itemIds,
            @RequestParam String disposalMethod,
            @RequestPart(value = "receiptImage", required = false) MultipartFile receiptImage,
            Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return "redirect:/auth/login";
        }

        donationService.processDonation(itemIds, userId, disposalMethod, receiptImage);

        return "redirect:/wardrobe"; // 완료 후 옷장으로 이동 (또는 완료 페이지)
    }

    // Helper method
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null)
            return null;
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            var token = (UsernamePasswordAuthenticationToken) principal;
            if (token.getPrincipal() instanceof CustomUserDetails) {
                return ((CustomUserDetails) token.getPrincipal()).getId();
            }
        } else if (principal instanceof OAuth2AuthenticationToken) {
            var token = (OAuth2AuthenticationToken) principal;
            if (token.getPrincipal() instanceof CustomOAuth2User) {
                return ((CustomOAuth2User) token.getPrincipal()).getId();
            }
        }
        return null;
    }
}
