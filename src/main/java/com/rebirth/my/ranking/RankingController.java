package com.rebirth.my.ranking;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.rebirth.my.auth.CustomOAuth2User;
import com.rebirth.my.auth.CustomUserDetails;
import com.rebirth.my.mypage.MypageVo;

@Controller
@RequestMapping("/ranking")
public class RankingController {

    @Autowired
    private RankingService rankingService;

    @GetMapping("")
    public String rankingList(Model model,
            @RequestParam(value = "type", defaultValue = "eco") String type,
            @RequestParam(value = "page", defaultValue = "1") int page) {

        // 1. 현재 로그인한 사용자 정보 조회
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long userId = getCurrentUserId(auth);

        if (userId != null) {
            MypageVo myRanking = rankingService.getMyRankingInfo(userId);
            model.addAttribute("myRanking", myRanking); // 상단 '나의 현황' 용
            model.addAttribute("isLoggedIn", true);
        } else {
            model.addAttribute("isLoggedIn", false);
        }

        // 2. 전체 랭킹 리스트 조회
        int pageSize = 10;
        List<RankingVo> rankings = rankingService.getRankingList(type, page, pageSize);
        int totalUsers = rankingService.getTotalUserCount();
        int totalPages = (int) Math.ceil((double) totalUsers / pageSize);

        model.addAttribute("rankings", rankings);
        model.addAttribute("currentType", type);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);

        return "ranking/list"; // templates/ranking/list.html
    }

    private Long getCurrentUserId(Authentication auth) {
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
}
