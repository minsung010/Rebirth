package com.rebirth.my.market;

import com.rebirth.my.domain.MarketVo;
import com.rebirth.my.domain.User;
import com.rebirth.my.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/market")
public class MarketController {

    @Autowired
    private MarketService marketService;

    @Autowired
    private UserMapper userMapper;

    @Value("${kakao.maps.appkey}")
    private String kakaoMapsAppKey;

    // 판매 목록 페이지 (지도 UI)
    @GetMapping("/list")
    public String list(Model model, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        List<MarketVo> items = marketService.getAllItems(userId);
        model.addAttribute("items", items);
        model.addAttribute("kakaoMapsAppKey", kakaoMapsAppKey);
        model.addAttribute("userId", userId);

        // 사용자 주소 정보 전달 (지도 초기 중심점용) + 채팅용 사용자 이름
        if (userId != null) {
            User user = userMapper.getUserById(userId);
            if (user != null) {
                if (user.getAddress() != null) {
                    model.addAttribute("userAddress", user.getAddress());
                }
                // 채팅용 사용자 이름 전달
                model.addAttribute("currentUsername", user.getName());
            }
        }

        return "market/list";
    }

    // 판매 등록 페이지
    @GetMapping("/register")
    public String registerForm(@RequestParam(required = false) String clothesId, Model model, Principal principal) {
        // 카카오맵 API Key 전달
        model.addAttribute("kakaoMapsAppKey", kakaoMapsAppKey);

        // 옷장에서 판매하기로 넘어온 경우 옷 정보 조회
        if (clothesId != null && !clothesId.isEmpty()) {
            Long userId = getUserIdFromPrincipal(principal);
            if (userId != null) {
                com.rebirth.my.wardrobe.WardrobeVo clothesInfo = marketService.getClothesInfo(clothesId,
                        String.valueOf(userId));
                if (clothesInfo != null) {
                    model.addAttribute("clothesInfo", clothesInfo);
                }
            }
        }
        return "market/register";
    }

    // 판매 등록 처리
    @PostMapping("/register")
    public String register(MarketVo vo, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);

        if (userId == null) {
            return "redirect:/auth/login";
        }

        vo.setUserId(userId);
        marketService.registerItem(vo);
        return "redirect:/market/list";
    }

    // 상품 상세 페이지
    @GetMapping("/detail/{id}")
    public String detail(@PathVariable("id") Long id, Model model, Principal principal) {
        Long currentUserId = getUserIdFromPrincipal(principal);

        // userId를 넘겨서 찜 여부까지 조회
        MarketVo item = marketService.getItemDetail(id, currentUserId);
        model.addAttribute("item", item);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("kakaoMapsAppKey", kakaoMapsAppKey);

        return "market/detail";
    }

    // 찜하기 토글 (AJAX)
    @PostMapping("/wish/{id}")
    @ResponseBody
    public boolean toggleWish(@PathVariable("id") Long id, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return false;
        }
        return marketService.toggleWish(id, userId);
    }

    // 판매 삭제 (취소) 처리
    @PostMapping("/delete/{id}")
    public String deleteItem(@PathVariable("id") Long id, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            marketService.deleteItem(id, userId);
        } catch (RuntimeException e) {
            // 권한 없음 등의 에러 처리
            return "redirect:/market/list";
        }

        return "redirect:/market/list";
    }

    // 상품 수정 페이지
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable("id") Long id, Model model, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        MarketVo item = marketService.getItemDetail(id, userId);

        if (item == null || !item.getUserId().equals(userId)) {
            return "redirect:/market/list"; // 권한 없음
        }

        model.addAttribute("item", item);
        model.addAttribute("kakaoMapsAppKey", kakaoMapsAppKey);
        return "market/register";
    }

    // 상품 수정 처리
    @PostMapping("/edit")
    public String edit(MarketVo vo, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return "redirect:/auth/login";
        }
        vo.setUserId(userId);
        marketService.updateItem(vo);
        return "redirect:/market/detail/" + vo.getId();
    }

    // 판매 완료 처리
    @PostMapping("/complete/{id}")
    public String completeSale(@PathVariable("id") Long id, Principal principal) {
        Long userId = getUserIdFromPrincipal(principal);
        if (userId == null) {
            return "redirect:/auth/login";
        }

        try {
            marketService.completeSale(id, userId);
        } catch (RuntimeException e) {
            // 권한 없음 등의 에러 처리
            return "redirect:/market/detail/" + id + "?error=true";
        }

        return "redirect:/market/list";
    }

    // Helper method to extract User ID from Principal
    private Long getUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            return null;
        }

        if (principal instanceof org.springframework.security.authentication.UsernamePasswordAuthenticationToken) {
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken token = (org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal;
            if (token.getPrincipal() instanceof com.rebirth.my.auth.CustomUserDetails) {
                return ((com.rebirth.my.auth.CustomUserDetails) token.getPrincipal()).getId();
            }
        } else if (principal instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) {
            org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken token = (org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken) principal;
            if (token.getPrincipal() instanceof com.rebirth.my.auth.CustomOAuth2User) {
                return ((com.rebirth.my.auth.CustomOAuth2User) token.getPrincipal()).getId();
            }
        }
        return null;
    }
}
