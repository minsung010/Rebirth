package com.rebirth.my.mypage;

import java.security.Principal; // 로그인 사용자 정보 획득을 위해 추가

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model; // Model 객체 추가
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.rebirth.my.auth.CustomUserDetails;
import com.rebirth.my.domain.User;

@Controller
@RequestMapping("/mypage")
public class MypageController {

    @org.springframework.beans.factory.annotation.Autowired
    private com.rebirth.my.mapper.UserMapper userMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private com.rebirth.my.mapper.UserProfileMapper userProfileMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired
    private com.rebirth.my.mapper.BadgeMapper badgeMapper;

    @org.springframework.beans.factory.annotation.Autowired
    private com.rebirth.my.mapper.EcoTodoMapper ecoTodoMapper;

    private Long getCurrentUserId(org.springframework.security.core.Authentication auth) {
        if (auth == null || !auth.isAuthenticated())
            return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof com.rebirth.my.auth.CustomUserDetails) {
            return ((com.rebirth.my.auth.CustomUserDetails) principal).getId();
        } else if (principal instanceof com.rebirth.my.auth.CustomOAuth2User) {
            return ((com.rebirth.my.auth.CustomOAuth2User) principal).getId();
        }
        return null;
    }

    @Autowired
    private MypageService mypageService;

    @GetMapping("")
    public String mypageMain(Model model, Principal principal) {
        // 1. Security Context에서 Authentication 가져오기
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        // 2. 인증된 사용자 ID (PK) 추출
        Long userId = getCurrentUserId(auth);
        if (userId == null) {
            return "redirect:/auth/login";
        }

        // 3. Service Layer: Get User Info for Header (MypageVo) using PK
        MypageVo userInfo = mypageService.getUserInfo(userId);
        model.addAttribute("user", userInfo);

        // 4. Legacy/Mapper Layer: Get Tasks and Badges
        // dbUserId 변수는 이미 구했으므로 재활용
        Long dbUserId = userId;

        if (dbUserId != null) {
            // Profile (Optional, if HTML still needs 'profile' object distinct from 'user')
            com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(dbUserId).orElse(null);
            model.addAttribute("profile", profile);

            // Eco Todo Tasks
            // java.util.List<com.rebirth.my.domain.EcoTodoTask> tasks =
            // ecoTodoMapper.findAllActiveTasks();
            java.util.List<com.rebirth.my.domain.EcoTodoTask> tasks = mypageService.getDailyMissions();
            java.util.List<com.rebirth.my.domain.UserTodoCheck> checks = ecoTodoMapper.findUserChecks(dbUserId,
                    java.time.LocalDate.now());

            java.util.Set<Long> checkedTaskIds = checks.stream()
                    .map(com.rebirth.my.domain.UserTodoCheck::getTaskId)
                    .collect(java.util.stream.Collectors.toSet());

            for (com.rebirth.my.domain.EcoTodoTask task : tasks) {
                task.setChecked(checkedTaskIds.contains(task.getId()));
            }
            model.addAttribute("ecoTasks", tasks);

            model.addAttribute("ecoTasks", tasks);
        } else {
            // Fallback for unauthenticated or test users if needed, or just leave empty
            // The HTML handles null checks for profile, but tasks/badges might need empty
            // lists
            model.addAttribute("totalBadgesCount", 0);
        }

        // 5. Decoration Ownership Info
        java.util.List<String> ownedItems = mypageService.getOwnedItemCodes(userId);
        model.addAttribute("ownedItems", ownedItems);

        return "mypage/main";
    }

    @GetMapping("/check-password")
    public String checkPasswordForm() {
        return "mypage/check_password";
    }

    @PostMapping("/check-password")
    public String checkPassword(@RequestParam("password") String password,
            Authentication auth,
            jakarta.servlet.http.HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long userId = getCurrentUserId(auth);
        if (userId == null)
            return "redirect:/auth/login";

        com.rebirth.my.domain.User user = userMapper.getUserById(userId);

        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            session.setAttribute("passwordVerified", true);

            // [Self-Healing] 비밀번호 확인 시점에도 이미지 동기화 시도
            syncSessionImageToDb(auth, user);

            return "redirect:/mypage/edit";
        } else {
            redirectAttributes.addFlashAttribute("error", "비밀번호가 일치하지 않습니다.");
            return "redirect:/mypage/check-password";
        }
    }

    @GetMapping("/edit")
    public String editForm(org.springframework.ui.Model model, jakarta.servlet.http.HttpSession session) {
        // Check if password verified
        if (session.getAttribute("passwordVerified") == null) {
            return "redirect:/mypage/check-password";
        }

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        Long userId = getCurrentUserId(auth);
        if (userId == null)
            return "redirect:/auth/login";

        // ID로 명확하게 조회
        com.rebirth.my.domain.User user = userMapper.getUserById(userId);
        com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(userId).orElse(null);

        // 연동된 소셜 계정 목록 조회
        java.util.List<com.rebirth.my.domain.OAuthAccount> accounts = oAuthAccountMapper.findByUserId(userId);
        java.util.List<String> linkedProviders = accounts.stream()
                .map(com.rebirth.my.domain.OAuthAccount::getProvider)
                .map(String::toUpperCase)
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        model.addAttribute("linkedProviders", linkedProviders);
        return "mypage/edit_profile";
    }

    // Helper method to sync session image to DB if DB misses it
    private void syncSessionImageToDb(Authentication auth, com.rebirth.my.domain.User dbUser) {
        try {
            Object principal = auth.getPrincipal();
            String sessionImg = null;
            if (principal instanceof com.rebirth.my.auth.CustomUserDetails) {
                sessionImg = ((com.rebirth.my.auth.CustomUserDetails) principal).getUser().getMemImg();
            } else if (principal instanceof com.rebirth.my.auth.CustomOAuth2User) {
                sessionImg = ((com.rebirth.my.auth.CustomOAuth2User) principal).getUser().getMemImg();
            }

            if (sessionImg != null && !sessionImg.isEmpty()) {
                boolean dbUpdated = false;
                if (dbUser.getMemImg() == null || dbUser.getMemImg().isEmpty()) {
                    dbUser.setMemImg(sessionImg);
                    userMapper.update(dbUser);
                    dbUpdated = true;
                }

                // Profile sync as well
                if (dbUpdated) {
                    com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(dbUser.getId()).orElse(null);
                    if (profile != null && (profile.getAvatarUrl() == null || profile.getAvatarUrl().isEmpty())) {
                        profile.setAvatarUrl(sessionImg);
                        userProfileMapper.update(profile);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore sync errors
        }
    }

    @Autowired
    private com.rebirth.my.mapper.OAuthAccountMapper oAuthAccountMapper;

    @org.springframework.web.bind.annotation.PostMapping("/update")
    public String updateProfile(
            @org.springframework.web.bind.annotation.RequestParam("nickname") String nickname,
            @org.springframework.web.bind.annotation.RequestParam("loginId") String loginId,
            @org.springframework.web.bind.annotation.RequestParam(value = "email", required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(value = "isEmailVerified", defaultValue = "false") boolean isEmailVerified,
            @org.springframework.web.bind.annotation.RequestParam(value = "password", required = false) String password,
            @org.springframework.web.bind.annotation.RequestParam(value = "birthDate", required = false) @org.springframework.format.annotation.DateTimeFormat(pattern = "yyyy-MM-dd") java.time.LocalDate birthDate,
            @org.springframework.web.bind.annotation.RequestParam(value = "profileImage", required = false) org.springframework.web.multipart.MultipartFile profileImage,
            RedirectAttributes redirectAttributes,
            jakarta.servlet.http.HttpSession session) {

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        Long userId = getCurrentUserId(auth);
        if (userId == null)
            return "redirect:/auth/login";

        // Update User Profile (Nickname, Avatar)
        com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(userId).orElse(null);
        if (profile != null) {
            profile.setNickname(nickname);

            // Handle Profile Image Upload
            if (profileImage != null && !profileImage.isEmpty()) {
                try {
                    // UploadConfig와 동일한 경로 설정
                    String projectDir = System.getProperty("user.dir");
                    String uploadDir = projectDir + "/uploads/";

                    java.io.File directory = new java.io.File(uploadDir);
                    if (!directory.exists())
                        directory.mkdirs();

                    String originalFileName = profileImage.getOriginalFilename();
                    String extension = "";
                    if (originalFileName != null && originalFileName.contains(".")) {
                        extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                    }

                    String newFileName = java.util.UUID.randomUUID().toString() + extension;

                    // Physical Save (Single Location)
                    java.io.File destFile = new java.io.File(uploadDir + newFileName);
                    profileImage.transferTo(destFile);

                    // Web URL
                    String webUrl = "/uploads/" + newFileName;
                    profile.setAvatarUrl(webUrl);

                    // 🌟 Call ProfileService for History Management
                    // MypageController에서 직접 업데이트하는 대신 Service를 통해 히스토리도 남기도록 개선
                    // 하지만 여기서는 이미 updateProfile이 호출된 상태이므로, Service의 uploadAndManageProfileImage
                    // 로직을 일부 차용하거나
                    // 단순히 여기서 매퍼 업데이트 + 히스토리 추가를 할 수 있음.
                    // 일관성을 위해 Service 호출이 가장 좋음.
                    profileService.uploadAndManageProfileImage(userId, webUrl);

                    // profileService에서 DB 업데이트를 하므로 아래 userProfileMapper.update(profile)는 중복일 수
                    // 있으나,
                    // uploadAndManageProfileImage는 memImg(User)와 avatarUrl(UserProfile) 둘 다 업데이트함.
                    // 따라서 여기서 profile 객체만 setAvatarUrl 하고 아래에서 update(profile) 하는 것은 괜찮음.
                    // 다만, Service 호출 시 User 테이블도 업데이트되므로 동기화 됨.

                    System.out.println("Uploaded to: " + destFile.getAbsolutePath());
                } catch (java.io.IOException e) {
                    e.printStackTrace();
                }
            }

            // userProfileMapper.update(profile); // Service에서 처리하므로 주석 처리 또는 재확인 필요
            // Service가 UserProfile도 업데이트하므로 여기서는 닉네임 변경 등 다른 필드 변경이 있을 때만 의미가 있음.
            // 닉네임 변경이 있으므로 update 호출 유지 (단, avatarUrl은 이미 최신화됨)
            userProfileMapper.update(profile);
        }

        // Update User (LoginId, Password, BirthDate)
        com.rebirth.my.domain.User user = null;
        if (auth.getPrincipal() instanceof com.rebirth.my.auth.CustomUserDetails) {
            user = userMapper
                    .findByEmailOrLoginId(((com.rebirth.my.auth.CustomUserDetails) auth.getPrincipal()).getUsername())
                    .orElse(null);
        } else if (auth.getPrincipal() instanceof com.rebirth.my.auth.CustomOAuth2User) {
            String pEmail = ((com.rebirth.my.auth.CustomOAuth2User) auth.getPrincipal()).getUser().getEmail();

            // 만약 소셜 로그인 유저인데 이메일이 없는 경우(null), ID로 조회해야 함
            if (pEmail == null) {
                user = userMapper.getUserById(userId);
            } else {
                user = userMapper.findByEmail(pEmail).orElse(null);
            }
        }

        if (user != null) {
            boolean isUpdated = false;

            // Update Login ID if changed
            if (loginId != null && !loginId.isEmpty() && !loginId.equals(user.getLoginId())) {
                // Duplicate Check
                if (userMapper.findByLoginId(loginId).isPresent()) {
                    redirectAttributes.addFlashAttribute("errorMessage", "이미 존재하는 아이디입니다.");
                    return "redirect:/mypage/edit";
                }
                user.setLoginId(loginId);
                isUpdated = true;
            }

            if (password != null && !password.isEmpty()) {
                user.setPassword(passwordEncoder.encode(password));
                isUpdated = true;
            }
            if (birthDate != null) {
                user.setBirthDate(birthDate.atStartOfDay());
                isUpdated = true;
            }

            // Sync Avatar URL to User table if updated in Profile
            if (profile != null && profile.getAvatarUrl() != null) {
                user.setMemImg(profile.getAvatarUrl());
                isUpdated = true;
            }

            // ============================================
            // 🚀 계정 통합 및 이메일 변경 로직
            // ============================================
            if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
                if (isEmailVerified) {
                    // 이메일이 검증되었다면, 해당 이메일을 사용하는 기존 유저가 있는지 확인
                    java.util.Optional<com.rebirth.my.domain.User> targetUserOpt = userMapper.findByEmail(email);

                    if (targetUserOpt.isPresent()) {
                        // A. 기존 계정 존재 -> 통합 (Merge)
                        com.rebirth.my.domain.User targetUser = targetUserOpt.get();
                        System.out.println("Processing Account Merge: " + user.getId() + " -> " + targetUser.getId());

                        // 1. 소셜 계정 이동 (Current -> Target)
                        oAuthAccountMapper.updateUserId(user.getId(), targetUser.getId());

                        // 2. 현재 임시 계정 삭제
                        userMapper.deleteById(user.getId());

                        // 3. 로그인 컨텍스트 전환 (Target 계정으로 로그인)
                        updateSecurityContext(targetUser);

                        redirectAttributes.addFlashAttribute("successMessage", "기존 계정과 성공적으로 통합되었습니다.");
                        return "redirect:/mypage";

                    } else {
                        // B. 기존 계정 없음 -> 단순 이메일 변경
                        user.setEmail(email);
                        user.setEmailVerifStatus("VERIFIED");
                        isUpdated = true;
                    }
                }
            }

            if (isUpdated) {
                userMapper.update(user);
                // Refresh Security Context
                updateSecurityContext(user);
            }
        }

        return "redirect:/mypage";
    }

    @Autowired
    private com.rebirth.my.service.ProfileService profileService;

    // 🌟 수정: 프로젝트 내부 경로를 사용하도록 변경 -> UploadConfig와 일치하는 프로젝트 루트/uploads
    private static final String PROJECT_DIR = System.getProperty("user.dir");
    private static final String UPLOAD_PATH = PROJECT_DIR + "/uploads/";
    private static final String WEB_PATH = "/uploads/";

    @PostMapping("/updateProfileImage")
    public String updateProfileImage(
            @RequestParam("profileImageFile") MultipartFile file,
            @RequestParam(value = "userId", required = false) String userId, // userId가 폼에 있다면 받음
            RedirectAttributes redirectAttributes) {

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "업로드할 파일을 선택해주세요.");
            return "redirect:/mypage";
        }

        try {
            Long dbUserId = null;
            if (userId != null) {
                User user = userMapper.findByEmailOrLoginId(userId).orElse(null);
                if (user != null) {
                    dbUserId = user.getId();
                }
            }

            // 만약 form에서 userId가 안넘어왔다면 Securiyt Context에서 추출 시도
            if (dbUserId == null) {
                org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                        .getContext().getAuthentication();
                dbUserId = getCurrentUserId(auth);
            }

            if (dbUserId != null) {
                String originalFileName = file.getOriginalFilename();
                String extension = "";
                if (originalFileName != null && originalFileName.contains(".")) {
                    extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                }
                String savedFileName = java.util.UUID.randomUUID().toString() + extension;

                // 🌟 Physical Save
                java.io.File uploadDir = new java.io.File(UPLOAD_PATH);
                if (!uploadDir.exists())
                    uploadDir.mkdirs();
                java.io.File destFile = new java.io.File(uploadDir, savedFileName);
                file.transferTo(destFile);

                // 🌟 Logic Integration: Call ProfileService
                String webUrl = WEB_PATH + savedFileName;
                profileService.uploadAndManageProfileImage(dbUserId, webUrl);

                // Refresh Security Context
                User user = profileService.findUserById(dbUserId); // Reload full user
                if (user != null) {
                    updateSecurityContext(user);
                }

                // 성공 메시지 전달
                redirectAttributes.addFlashAttribute("successMessage", "프로필 사진이 성공적으로 변경되었습니다.");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "사용자 정보를 찾을 수 없습니다.");
            }

        } catch (Exception e) {
            // 파일 처리 중 오류 발생 (예: I/O 오류, 파일 크기 초과 등)
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "파일 업로드 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 마이페이지로 다시 리다이렉트
        return "redirect:/mypage";
    }

    @org.springframework.web.bind.annotation.PostMapping("/mission/toggle")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> toggleMission(
            @org.springframework.web.bind.annotation.RequestParam("taskId") Long taskId,
            @org.springframework.web.bind.annotation.RequestParam("checked") boolean checked) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        Long userId = getCurrentUserId(auth);

        if (userId == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }

        try {
            java.time.LocalDate today = java.time.LocalDate.now();

            // 1. Find Task to get points
            // Since we don't have findById in EcoTodoMapper, we'll iterate the static list
            // or assume points.
            // Ideally, fetch from DB. Let's use the static list in Service for now as it's
            // faster than adding Mapper method.
            com.rebirth.my.domain.EcoTodoTask task = mypageService.getTaskById(taskId);
            int points = (task != null) ? task.getDefaultPoints() : 10; // Default 10 if not found

            if (checked) {
                // Insert Check
                com.rebirth.my.domain.UserTodoCheck check = new com.rebirth.my.domain.UserTodoCheck();
                check.setUserId(userId);
                check.setTaskId(taskId);
                check.setCheckDate(today);
                check.setPointsEarned(points);
                ecoTodoMapper.insertCheck(check);

                // Update User Points
                com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(userId).orElse(null);
                if (profile != null) {
                    profile.setEcoPoints(profile.getEcoPoints() + points);
                    userProfileMapper.update(profile);
                }
            } else {
                // Delete Check
                ecoTodoMapper.deleteCheck(userId, taskId, today);

                // Deduct User Points
                com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(userId).orElse(null);
                if (profile != null) {
                    profile.setEcoPoints(Math.max(0, profile.getEcoPoints() - points));
                    userProfileMapper.update(profile);
                }
            }

            response.put("success", true);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
        }

        return response;
    }

    @org.springframework.web.bind.annotation.PostMapping("/shop/buy")
    @org.springframework.web.bind.annotation.ResponseBody
    public java.util.Map<String, Object> buyDecoration(
            @org.springframework.web.bind.annotation.RequestParam("itemCode") String itemCode,
            @org.springframework.web.bind.annotation.RequestParam("price") int price) {

        java.util.Map<String, Object> response = new java.util.HashMap<>();

        // 1. Validate User
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        Long userId = getCurrentUserId(auth);

        if (userId == null) {
            response.put("success", false);
            response.put("message", "로그인이 필요합니다.");
            return response;
        }

        try {
            // 2. Reset Case
            if (itemCode == null || itemCode.isEmpty()) {
                com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(userId).orElse(null);
                if (profile != null) {
                    profile.setActiveDecoration(null);
                    userProfileMapper.update(profile);

                    // Refresh Session
                    User updatedUser = userMapper.getUserById(userId);
                    updateSecurityContext(updatedUser);

                    response.put("success", true);
                    response.put("newPoints", profile.getEcoPoints());
                    response.put("message", "프로필 장식 초기화가 완료되었습니다.");
                    return response;
                }
            }

            // 3. Load Profile
            com.rebirth.my.domain.UserProfile profile = userProfileMapper.findById(userId).orElse(null);
            if (profile == null) {
                response.put("success", false);
                response.put("message", "프로필 정보를 찾을 수 없습니다.");
                return response;
            }

            // 4. Check Ownership
            java.util.List<String> ownedItems = mypageService.getOwnedItemCodes(userId);
            boolean alreadyOwned = ownedItems.contains(itemCode);

            if (alreadyOwned) {
                // Apply without deduction
                profile.setActiveDecoration(itemCode);
                userProfileMapper.update(profile);

                // Refresh Session
                User updatedUser = userMapper.getUserById(userId);
                updateSecurityContext(updatedUser);

                response.put("success", true);
                response.put("newPoints", profile.getEcoPoints());
                response.put("message", "소유 중인 아이템을 장착했습니다!");
                return response;
            }

            // 5. New Purchase - Check Points
            if (profile.getEcoPoints() < price) {
                response.put("success", false);
                response.put("message", "포인트가 부족합니다.");
                return response;
            }

            // 6. Deduct Points & Apply Decoration
            profile.setEcoPoints(profile.getEcoPoints() - price);
            profile.setActiveDecoration(itemCode);

            // 7. Save Ownership & Profile
            userProfileMapper.update(profile);
            mypageService.addOwnedDecoration(userId, itemCode);

            // Refresh Session
            User updatedUserForPurchase = userMapper.getUserById(userId);
            updateSecurityContext(updatedUserForPurchase);

            response.put("success", true);
            response.put("newPoints", profile.getEcoPoints());
            response.put("message", "아이템을 성공적으로 구매하고 적용했습니다!");

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", "처리 중 오류 발생: " + e.getMessage());
        }

        return response;
    }

    private void updateSecurityContext(User user) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null)
            return;

        Object currentPrincipal = auth.getPrincipal();
        Object newPrincipal = null;

        if (currentPrincipal instanceof CustomUserDetails) {
            newPrincipal = new CustomUserDetails(user);
        } else if (currentPrincipal instanceof com.rebirth.my.auth.CustomOAuth2User) {
            com.rebirth.my.auth.CustomOAuth2User oldOAuth2User = (com.rebirth.my.auth.CustomOAuth2User) currentPrincipal;
            newPrincipal = new com.rebirth.my.auth.CustomOAuth2User(user, oldOAuth2User.getAttributes(),
                    oldOAuth2User.getNameAttributeKey());
        }

        if (newPrincipal != null) {
            Authentication newAuth = new UsernamePasswordAuthenticationToken(
                    newPrincipal, auth.getCredentials(),
                    (newPrincipal instanceof org.springframework.security.core.userdetails.UserDetails)
                            ? ((org.springframework.security.core.userdetails.UserDetails) newPrincipal)
                                    .getAuthorities()
                            : ((com.rebirth.my.auth.CustomOAuth2User) newPrincipal).getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(newAuth);
        }
    }
}
