package com.rebirth.my.wardrobe;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.rebirth.my.auth.CustomOAuth2User;
import com.rebirth.my.auth.CustomUserDetails;

@Controller
@RequestMapping("/wardrobe")
public class WardrobeController {

    @Autowired
    private WardrobeDao wardrobeDao;

    /**
     * Spring Security에서 로그인 사용자 ID 가져오기
     */
    private Long getLoginUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            return null;

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getId();
        }
        return null;
    }

    @GetMapping("")
    public String wardrobeMain(Model model) {
        Long userId = getLoginUserId();

        if (userId != null) {
            String userIdStr = String.valueOf(userId);
            List<WardrobeVo> clothesList = wardrobeDao.selectClothesList(userIdStr);
            model.addAttribute("clothesList", clothesList);
            model.addAttribute("totalCount", clothesList.size());
        } else {
            model.addAttribute("clothesList", List.of());
            model.addAttribute("totalCount", 0);
        }

        return "wardrobe/main";
    }
}
