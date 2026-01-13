package com.rebirth.my.notice;

import com.rebirth.my.auth.CustomOAuth2User;
import com.rebirth.my.auth.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/community")
public class CommunityController {

    @Autowired
    private NoticeService noticeService;

    private String getCurrentUserId(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return null;
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return String.valueOf(((CustomUserDetails) principal).getId());
        } else if (principal instanceof CustomOAuth2User) {
            return String.valueOf(((CustomOAuth2User) principal).getId());
        }

        return null;
    }

    private String getCurrentUserName(Authentication auth) {
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return "Anonymous";
        }

        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getName();
        } else if (principal instanceof CustomOAuth2User) {
            return ((CustomOAuth2User) principal).getRealName();
        }

        return auth.getName();
    }

    @GetMapping({ "", "/" })
    public String list(
            @RequestParam(defaultValue = "NOTICE") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String keyword,
            Model model) {

        List<NoticeVo> posts = noticeService.getNoticeList();

        // Filter by category (Temporary, better to do in SQL)
        posts = posts.stream()
                .filter(p -> category.equals(p.getCategory()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("posts", posts);
        model.addAttribute("currentCategory", category);
        model.addAttribute("keyword", keyword);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUserId = getCurrentUserId(auth);

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUserId", currentUserId);

        // Mock Page object
        model.addAttribute("page", new MockPage(page, 1));

        // Pinned post logic removed
        // if (!posts.isEmpty()) {
        // model.addAttribute("pinnedPost", posts.get(0));
        // }

        return "notice/list";
    }

    @GetMapping("/{id}")
    public String detail(@org.springframework.web.bind.annotation.PathVariable int id, Model model) {
        NoticeVo post = noticeService.getNoticeDetail(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUserId = getCurrentUserId(auth);

        // Secret Post Access Check
        if ("Y".equals(post.getIsSecret())) {
            boolean canView = isAdmin || (currentUserId != null && currentUserId.equals(post.getUserId()));
            if (!canView) {
                // Return alert and redirect using a simple script in a direct response or a
                // dedicated view
                model.addAttribute("msg", "비밀글입니다. 작성자와 관리자만 볼 수 있습니다.");
                model.addAttribute("url", "/community?category=" + post.getCategory());
                return "common/alert_redirect"; // You might need to create this view or handle it differently
            }
        }

        // Increase view count only if access is allowed
        noticeService.increaseViewCount(id);
        // Re-fetch to show updated view count (Optional, but good for consistency)
        // post.setViewCount(post.getViewCount() + 1); // Or just update the object
        // manually to save a query

        model.addAttribute("post", post);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("currentUserId", currentUserId);

        return "notice/detail";
    }

    @GetMapping("/new")
    public String writeForm(@RequestParam(required = false) String category, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        // Only Admin can write NOTICE
        if ("NOTICE".equals(category) && !isAdmin) {
            return "redirect:/community";
        }

        model.addAttribute("category", category);
        return "notice/write";
    }

    @org.springframework.web.bind.annotation.PostMapping("/save")
    public String save(NoticeVo vo) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if ("NOTICE".equals(vo.getCategory()) && !isAdmin) {
            return "redirect:/community";
        }

        String userId = getCurrentUserId(auth);
        if (userId != null) {
            vo.setUserId(userId);
            vo.setWriter(getCurrentUserName(auth));
        }

        // Handling null isSecret
        if (vo.getIsSecret() == null) {
            vo.setIsSecret("N");
        }

        noticeService.writeNotice(vo);
        return "redirect:/community?category=" + vo.getCategory();
    }

    @GetMapping("/{id}/edit")
    public String editForm(@org.springframework.web.bind.annotation.PathVariable int id, Model model) {
        NoticeVo post = noticeService.getNoticeDetail(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUserId = getCurrentUserId(auth);

        // Permission Check
        boolean canEdit = isAdmin || (currentUserId != null && currentUserId.equals(post.getUserId())
                && "QNA".equals(post.getCategory()));
        if (!canEdit) {
            return "redirect:/community/" + id;
        }

        model.addAttribute("post", post);
        model.addAttribute("category", post.getCategory());
        return "notice/write";
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/update")
    public String update(@org.springframework.web.bind.annotation.PathVariable int id, NoticeVo vo) {
        NoticeVo original = noticeService.getNoticeDetail(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUserId = getCurrentUserId(auth);

        boolean canEdit = isAdmin || (currentUserId != null && currentUserId.equals(original.getUserId())
                && "QNA".equals(original.getCategory()));
        if (!canEdit) {
            return "redirect:/community/" + id;
        }

        vo.setId(id);
        noticeService.updateNotice(vo);
        return "redirect:/community/" + id;
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/delete")
    public String delete(@org.springframework.web.bind.annotation.PathVariable int id) {
        NoticeVo original = noticeService.getNoticeDetail(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUserId = getCurrentUserId(auth);

        boolean canDelete = isAdmin || (currentUserId != null && currentUserId.equals(original.getUserId())
                && "QNA".equals(original.getCategory()));
        if (!canDelete) {
            return "redirect:/community/" + id;
        }

        noticeService.deleteNotice(id);
        return "redirect:/community?category=" + original.getCategory();
    }

    @org.springframework.web.bind.annotation.PostMapping("/{id}/answer")
    public String saveAnswer(@org.springframework.web.bind.annotation.PathVariable int id,
            @RequestParam String answer) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null
                && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {
            return "redirect:/community/" + id;
        }

        noticeService.registAnswer(id, answer);
        return "redirect:/community/" + id;
    }

    public static class MockPage {
        public int number;
        public int totalPages;

        public MockPage(int number, int totalPages) {
            this.number = number;
            this.totalPages = totalPages;
        }

        public boolean hasPrevious() {
            return number > 0;
        }

        public boolean hasNext() {
            return number < totalPages - 1;
        }
    }
}
