package com.rebirth.my.mypage;

import java.io.File;
import java.io.IOException;
// ... (나머지 import 생략)
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // 2. AuthenticationPrincipal import
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.rebirth.my.auth.CustomUserDetails; // 1. CustomUserDetails import
import com.rebirth.my.domain.User;
import com.rebirth.my.service.ProfileService;

@Controller
@RequestMapping("/mypage/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @Autowired
    private MypageService mypageService;

    // 🌟 수정: UploadConfig와 동일하게 프로젝트 루트의 uploads/ 폴더 사용
    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String UPLOAD_PATH = PROJECT_DIR + "/uploads/";
    private static final String WEB_PATH = "/uploads/";

    private Long getUserId(Authentication auth) {
        if (auth == null)
            return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        } else if (principal instanceof com.rebirth.my.auth.CustomOAuth2User) {
            return ((com.rebirth.my.auth.CustomOAuth2User) principal).getId();
        }
        return null;
    }

    // ====================================================================
    // 1. 새 프로필 사진 업로드 및 히스토리 기록
    // ====================================================================
    // ====================================================================
    // 1. 새 프로필 사진 업로드 및 히스토리 기록
    // ====================================================================
    @PostMapping("/upload")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadProfileImage(
            @RequestParam("profileImage") MultipartFile file,
            Authentication authentication // <--- 변경: Authentication 객체 사용
    ) {
        Map<String, Object> response = new HashMap<>();

        if (file.isEmpty()) {
            response.put("success", false);
            response.put("message", "업로드할 파일이 없습니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String savedFileName = UUID.randomUUID().toString() + extension;

            // 1) 물리적 파일 저장 (프로젝트 루트/uploads/ 에 저장)
            saveFile(file, savedFileName);

            // 2) DB 업데이트 및 히스토리 관리 (Service 호출)
            // 🌟 수정: 이제 imagePath는 /uploads/가 포함된 웹 경로로 전달됨
            String webUrl = WEB_PATH + savedFileName;
            profileService.uploadAndManageProfileImage(userId, webUrl);

            User updatedUser = profileService.findUserById(userId);

            // 2. 새로운 Principal 객체 생성 (세션 갱신)
            if (updatedUser.getMemImg() != null) {
                // Ensure the session user gets the correct web path
                if (!updatedUser.getMemImg().startsWith(WEB_PATH) && updatedUser.getMemImg().contains("uploads")) {
                    // If for some reason it's a full path, sanitize it (defensive coding)
                    updatedUser.setMemImg(webUrl);
                }
            }

            // 4. 세션 갱신 (Principal 타입 유지)
            Object currentPrincipal = authentication.getPrincipal();
            Object newPrincipal = null;

            if (currentPrincipal instanceof CustomUserDetails) {
                newPrincipal = new CustomUserDetails(updatedUser);
            } else if (currentPrincipal instanceof com.rebirth.my.auth.CustomOAuth2User) {
                com.rebirth.my.auth.CustomOAuth2User oldOAuth2User = (com.rebirth.my.auth.CustomOAuth2User) currentPrincipal;
                newPrincipal = new com.rebirth.my.auth.CustomOAuth2User(updatedUser, oldOAuth2User.getAttributes(),
                        oldOAuth2User.getNameAttributeKey());
            }

            if (newPrincipal != null) {
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        newPrincipal, authentication.getCredentials(),
                        (newPrincipal instanceof org.springframework.security.core.userdetails.UserDetails)
                                ? ((org.springframework.security.core.userdetails.UserDetails) newPrincipal)
                                        .getAuthorities()
                                : ((com.rebirth.my.auth.CustomOAuth2User) newPrincipal).getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }

            // 🌟🌟🌟 Security Context 갱신 완료 🌟🌟🌟

            // 5. 프론트엔드 응답에도 웹 URL을 담아 보냅니다. (JS 즉시 갱신용)
            String webUrlForResponse = WEB_PATH + savedFileName;
            response.put("success", true);
            response.put("message", "프로필 이미지가 성공적으로 변경되었습니다.");
            response.put("newPath", webUrl); // Use the explicitly constructed web URL
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (IOException e) {
            System.err.println("!!! [Controller I/O Error] 파일 저장 중 오류 발생:");
            System.err.println("!!! Error Message: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "파일 저장 중 시스템 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            System.err.println("!!! [Controller Service Error] DB 처리 중 오류 발생:");
            System.err.println("!!! Error Message: " + e.getMessage());
            e.printStackTrace();

            response.put("success", false);
            response.put("message", "프로필 변경 중 알 수 없는 오류가 발생했습니다.");
            return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ====================================================================
    // 2. 최근 5개 이미지 기록 조회
    // ====================================================================
    @GetMapping("/history")
    @ResponseBody
    public ResponseEntity<List<String>> getProfileHistory(
            Authentication authentication) {
        try {
            Long userId = getUserId(authentication);
            if (userId == null)
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

            List<String> historyPaths = profileService.getRecentImageHistory(userId);
            return new ResponseEntity<>(historyPaths, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ====================================================================
    // 3. 히스토리 이미지로 프로필 복원 (원클릭 변경)
    // ====================================================================
    @PostMapping("/restore")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> restoreProfileImage(
            @RequestBody Map<String, String> request,
            Authentication authentication) {
        Map<String, Object> response = new HashMap<>();
        String imagePath = request.get("imagePath");

        if (imagePath == null || imagePath.isEmpty()) {
            response.put("success", false);
            response.put("message", "복원할 이미지 경로가 필요합니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        try {
            Long userId = getUserId(authentication);
            if (userId == null) {
                response.put("success", false);
                response.put("message", "로그인이 필요합니다.");
                return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
            }

            profileService.restoreProfileImage(userId, imagePath);

            User updatedUser = profileService.findUserById(userId);
            if (updatedUser.getMemImg() != null && updatedUser.getMemImg().startsWith(UPLOAD_PATH)) {
                String webUrl = updatedUser.getMemImg().replace(UPLOAD_PATH, WEB_PATH);
                updatedUser.setMemImg(webUrl);
            }

            // 세션 갱신
            Object currentPrincipal = authentication.getPrincipal();
            Object newPrincipal = null;

            if (currentPrincipal instanceof CustomUserDetails) {
                newPrincipal = new CustomUserDetails(updatedUser);
            } else if (currentPrincipal instanceof com.rebirth.my.auth.CustomOAuth2User) {
                com.rebirth.my.auth.CustomOAuth2User oldOAuth2User = (com.rebirth.my.auth.CustomOAuth2User) currentPrincipal;
                newPrincipal = new com.rebirth.my.auth.CustomOAuth2User(updatedUser, oldOAuth2User.getAttributes(),
                        oldOAuth2User.getNameAttributeKey());
            }

            if (newPrincipal != null) {
                Authentication newAuth = new UsernamePasswordAuthenticationToken(
                        newPrincipal, authentication.getCredentials(),
                        (newPrincipal instanceof org.springframework.security.core.userdetails.UserDetails)
                                ? ((org.springframework.security.core.userdetails.UserDetails) newPrincipal)
                                        .getAuthorities()
                                : ((com.rebirth.my.auth.CustomOAuth2User) newPrincipal).getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(newAuth);
            }

            response.put("success", true);
            response.put("message", "프로필 이미지가 히스토리 이미지로 복원되었습니다.");
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (Exception e) {
            // ... 오류 처리
        }
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @GetMapping("/edit")
    public String viewEditProfile(Model model, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            return "redirect:/auth/login";

        Long userId = getUserId(authentication);
        if (userId == null)
            return "redirect:/auth/login";

        // 사용자 이름/ID 획득
        String username = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            username = ((CustomUserDetails) principal).getUsername();
        } else if (principal instanceof com.rebirth.my.auth.CustomOAuth2User) {
            username = ((com.rebirth.my.auth.CustomOAuth2User) principal).getUser().getEmail();
        }

        MypageVo profileVo = mypageService.getUserInfo(username);
        model.addAttribute("profile", profileVo);

        User user = profileService.findUserById(userId);
        model.addAttribute("user", user);

        return "mypage/edit_profile";
    }

    /**
     * 파일 저장 헬퍼 메서드: UploadConfig와 일치하는 경로에 저장
     */
    private void saveFile(MultipartFile file, String savedFileName) throws IOException {
        File uploadDir = new File(UPLOAD_PATH);
        if (!uploadDir.exists())
            uploadDir.mkdirs();

        File destFile = new File(uploadDir, savedFileName);
        file.transferTo(destFile);
    }
}
