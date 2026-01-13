package com.rebirth.my.mypage;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.Locale;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.rebirth.my.auth.CustomUserDetails;
import com.rebirth.my.auth.CustomOAuth2User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.rebirth.my.domain.Badge;
import com.rebirth.my.domain.User;
import com.rebirth.my.domain.UserBadge;
import com.rebirth.my.domain.UserProfile;
import com.rebirth.my.mapper.BadgeMapper;
import com.rebirth.my.mapper.UserMapper;
import com.rebirth.my.mapper.UserProfileMapper;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MypageService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Autowired
    private BadgeMapper badgeMapper;

    @Autowired
    private MypageDao mypageDao;

    /**
     * 사용자 ID(Email)를 이용해 마이페이지 정보를 조회하고, 출력에 필요한 값을 포맷합니다.
     * 
     * @param email 로그인된 사용자 이메일 (Principal.getName())
     * @return 포맷팅된 문자열을 포함하는 MypageVo (사용자 정보)
     */
    /**
     * 사용자 PK(ID)를 이용해 마이페이지 정보를 조회합니다. (권장)
     * 소셜 로그인 등에서 Principal Name이 이메일 형식이 아닐 수 있으므로 PK 조회가 안전합니다.
     */
    public MypageVo getUserInfo(Long userId) {
        MypageVo vo = new MypageVo();

        // 1. User 기본 정보 조회 (PK 사용)
        User user = userMapper.getUserById(userId);
        if (user == null) {
            return new MypageVo();
        }

        // 2. UserProfile 상세 정보 조회
        UserProfile profile = userProfileMapper.findById(user.getId()).orElse(null);

        // 3. VO 매핑
        vo.setUserId(user.getEmail()); // 화면에 보여줄 ID (이메일 사용)
        vo.setUserName(user.getName()); // 실명
        vo.setStatus(user.getStatus()); // 계정 상태
        vo.setWithdrawalAt(user.getWithdrawalAt()); // 탈퇴 예정일

        if (profile != null) {

            // 2. 🚨 핵심: 헤더와 동일하게 User 테이블의 memImg를 최우선으로 참조 🚨
            String latestPath = user.getMemImg();

            // 3. User 테이블에 경로가 없을 때만 Profile 테이블을 봅니다.
            if (latestPath == null || latestPath.isEmpty()) {
                latestPath = profile.getAvatarUrl();
            }

            // 4. [추가] DB에 이미지가 없지만 세션(헤더)에는 있는 경우 동기화 (Self-Healing)
            if (latestPath == null || latestPath.isEmpty()) {
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() != null) {
                        Object principal = auth.getPrincipal();
                        User sessionUser = null;

                        if (principal instanceof CustomUserDetails) {
                            sessionUser = ((CustomUserDetails) principal).getUser();
                        } else if (principal instanceof CustomOAuth2User) {
                            sessionUser = ((CustomOAuth2User) principal).getUser();
                        }

                        // 세션 유저가 현재 조회 대상 유저와 동일인이고, 이미지를 가지고 있다면 사용
                        if (sessionUser != null && sessionUser.getId().equals(user.getId())
                                && sessionUser.getMemImg() != null && !sessionUser.getMemImg().isEmpty()) {

                            latestPath = sessionUser.getMemImg();
                            // DB 업데이트 (자가 치유)
                            user.setMemImg(latestPath);
                            userMapper.update(user);
                            if (profile.getAvatarUrl() == null || profile.getAvatarUrl().isEmpty()) {
                                profile.setAvatarUrl(latestPath);
                                userProfileMapper.update(profile);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to sync session image: " + e.getMessage());
                }
            }

            // 5. 경로 변환 로직 (웹 URL로 변환)
            if (latestPath != null && latestPath.startsWith("C:/profile_upload/")) {
                vo.setAvatarUrl(latestPath.replace("C:/profile_upload/", "/profile-images/"));
            } else {
                vo.setAvatarUrl(latestPath);
            }

            vo.setUserName(profile.getNickname()); // 닉네임이 있으면 닉네임 우선 사용
            vo.setEcoPoint(profile.getEcoPoints() != null ? profile.getEcoPoints() : 0);
            vo.setGender(profile.getGender());
            vo.setActiveDecoration(profile.getActiveDecoration()); // 프로필 꾸미기 매핑
            vo.setLastLoginAt(profile.getLastLoginAt());

            // 세션에서 '이전 로그인 시간' 가져오기 (보안 강화 기능)
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpSession session = attr.getRequest().getSession(false);
                if (session != null) {
                    java.time.LocalDateTime prev = (java.time.LocalDateTime) session.getAttribute("previousLoginAt");
                    vo.setPreviousLoginAt(prev);
                }
            }

            // Null Safety Check
            double waterL = profile.getTotalWaterSavedL() != null ? profile.getTotalWaterSavedL() : 0.0;
            double carbonKg = profile.getTotalCarbonSavedKg() != null ? profile.getTotalCarbonSavedKg() : 0.0;

            vo.setTotalWater(waterL);
            vo.setTotalCarbon(carbonKg);

            // [에너지 절약량 계산]
            // 공식: 물절약량(m³) * 1 + 탄소감축량(kg) / 0.424
            // 1 m³ = 1000L 이므로 (L / 1000) 적용
            double energyValue = (waterL / 1000.0) * 1.0 + (carbonKg / 0.424);
            vo.setTotalEnergy(energyValue);

            // 뱃지 정보 조회 및 매핑 (추후 중복 코드 리팩토링 필요)
            List<Badge> allBadges = badgeMapper.findAll();
            List<UserBadge> userBadges = badgeMapper.findUserBadges(user.getId());

            vo.setBadgeCount(userBadges.size());
            vo.setTotalBadges(allBadges.size());

            java.util.Set<Long> acquiredIds = userBadges.stream()
                    .map(UserBadge::getBadgeId)
                    .collect(Collectors.toSet());

            for (Badge b : allBadges) {
                b.setAcquired(acquiredIds.contains(b.getId()));
            }
            vo.setBadges(allBadges);

            // 6. 통합 활동 내역 조회 추가
            List<ActivityVo> history = mypageDao.selectActivityHistory(userId);
            vo.setActivityHistory(history);

            // 7. 나의 랭킹 정보 조회 추가
            MypageVo rankingInfo = mypageDao.selectMyRanking(userId);
            if (rankingInfo != null) {
                vo.setEcoPointRank(rankingInfo.getEcoPointRank());
                vo.setDonationRank(rankingInfo.getDonationRank());
                vo.setSalesRank(rankingInfo.getSalesRank());
                vo.setTotalUsers(rankingInfo.getTotalUsers());
                vo.setDonationCount(rankingInfo.getDonationCount());
                vo.setSalesCount(rankingInfo.getSalesCount());
            }

            // 8. 관심상품 목록 조회 추가
            List<WishlistItemVo> wishlist = mypageDao.selectWishlist(userId);
            vo.setWishlist(wishlist);
        } else {
            // 프로필이 없는 경우 기본값 설정 - DiceBear 등
            vo.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getName());
            vo.setEcoPoint(0);
            vo.setTotalWater(0.0);
            vo.setTotalCarbon(0.0);
            vo.setTotalEnergy(0.0);
        }

        // 4. 포맷팅
        vo.setFormattedTotalWater(formatNumber(vo.getTotalWater(), "#,##0.0"));
        vo.setFormattedTotalCarbon(formatNumber(vo.getTotalCarbon(), "#,##0.0"));
        vo.setFormattedTotalEnergy(formatNumber(vo.getTotalEnergy(), "#,##0"));

        return vo;
    }

    /**
     * 사용자 ID(Email)를 이용해 마이페이지 정보를 조회하고, 출력에 필요한 값을 포맷합니다.
     * 
     * @param email 로그인된 사용자 이메일 (Principal.getName())
     * @return 포맷팅된 문자열을 포함하는 MypageVo (사용자 정보)
     */
    public MypageVo getUserInfo(String email) {
        MypageVo vo = new MypageVo();

        // 1. User 기본 정보 조회
        User user = userMapper.findByEmailOrLoginId(email).orElse(null);
        if (user == null) {
            // 테스트용 또는 예외 처리: 사용자를 찾을 수 없는 경우 빈 VO 반환하거나 더미 데이터
            return new MypageVo();
        }

        // 2. UserProfile 상세 정보 조회
        UserProfile profile = userProfileMapper.findById(user.getId()).orElse(null);

        // 3. VO 매핑
        vo.setUserId(user.getEmail()); // 화면에 보여줄 ID (이메일 사용)
        vo.setUserName(user.getName()); // 실명
        vo.setStatus(user.getStatus()); // 계정 상태 (ACTIVE, PENDING_WITHDRAWAL 등)
        vo.setWithdrawalAt(user.getWithdrawalAt()); // 탈퇴 예정일

        if (profile != null) {

            // 2. 🚨 핵심: 헤더와 동일하게 User 테이블의 memImg를 최우선으로 참조 🚨
            // 헤더가 바뀌었다면 user.getMemImg()에 이미 새 경로가 들어있습니다.
            String latestPath = user.getMemImg();

            // 3. User 테이블에 경로가 없을 때만 Profile 테이블을 봅니다.
            if (latestPath == null || latestPath.isEmpty()) {
                latestPath = profile.getAvatarUrl();
            }

            // 4. [추가] DB에 이미지가 없지만 세션(헤더)에는 있는 경우 동기화 (Self-Healing)
            if (latestPath == null || latestPath.isEmpty()) {
                try {
                    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                    if (auth != null && auth.getPrincipal() != null) {
                        Object principal = auth.getPrincipal();
                        User sessionUser = null;

                        if (principal instanceof CustomUserDetails) {
                            sessionUser = ((CustomUserDetails) principal).getUser();
                        } else if (principal instanceof CustomOAuth2User) {
                            sessionUser = ((CustomOAuth2User) principal).getUser();
                        }

                        // 세션 유저가 현재 조회 대상 유저와 동일인이고, 이미지를 가지고 있다면 사용
                        if (sessionUser != null && sessionUser.getId().equals(user.getId())
                                && sessionUser.getMemImg() != null && !sessionUser.getMemImg().isEmpty()) {

                            latestPath = sessionUser.getMemImg();

                            // DB 업데이트 (자가 치유)
                            user.setMemImg(latestPath);
                            userMapper.update(user);

                            if (profile.getAvatarUrl() == null || profile.getAvatarUrl().isEmpty()) {
                                profile.setAvatarUrl(latestPath);
                                userProfileMapper.update(profile);
                            }
                            System.out.println(
                                    "Self-healed missing profile image from Session for user: " + user.getId());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to sync session image: " + e.getMessage());
                }
            }

            // 4. 경로 변환 로직 (웹 URL로 변환)
            if (latestPath != null && latestPath.startsWith("C:/profile_upload/")) {
                vo.setAvatarUrl(latestPath.replace("C:/profile_upload/", "/profile-images/"));
            } else {
                vo.setAvatarUrl(latestPath);
            }
            // --- 🚨 로직 추가 끝 🚨 ---

            vo.setUserName(profile.getNickname()); // 닉네임이 있으면 닉네임 우선 사용
            // vo.setAvatarUrl(profile.getAvatarUrl());
            vo.setEcoPoint(profile.getEcoPoints() != null ? profile.getEcoPoints() : 0);
            vo.setGender(profile.getGender());
            vo.setLastLoginAt(profile.getLastLoginAt());

            // 세션에서 '이전 로그인 시간' 가져오기 (보안 강화 기능)
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attr != null) {
                HttpSession session = attr.getRequest().getSession(false);
                if (session != null) {
                    java.time.LocalDateTime prev = (java.time.LocalDateTime) session.getAttribute("previousLoginAt");
                    vo.setPreviousLoginAt(prev);
                }
            }

            // Null Safety Check: DB에서 NULL이 넘어올 수 있으므로 기본값 0.0 처리
            double waterL = profile.getTotalWaterSavedL() != null ? profile.getTotalWaterSavedL() : 0.0;
            double carbonKg = profile.getTotalCarbonSavedKg() != null ? profile.getTotalCarbonSavedKg() : 0.0;

            vo.setTotalWater(waterL);
            vo.setTotalCarbon(carbonKg);

            // [에너지 절약량 계산]
            // 공식: 물절약량(m³) * 1 + 탄소감축량(kg) / 0.424
            // 1 m³ = 1000L 이므로 (L / 1000) 적용
            double energyValue = (waterL / 1000.0) * 1.0 + (carbonKg / 0.424);
            vo.setTotalEnergy(energyValue);

            // 뱃지 정보 조회 및 매핑
            List<Badge> allBadges = badgeMapper.findAll();
            List<UserBadge> userBadges = badgeMapper.findUserBadges(user.getId());

            vo.setBadgeCount(userBadges.size());
            vo.setTotalBadges(allBadges.size());

            // 획득 상태 반영
            java.util.Set<Long> acquiredIds = userBadges.stream()
                    .map(UserBadge::getBadgeId)
                    .collect(Collectors.toSet());

            for (Badge b : allBadges) {
                b.setAcquired(acquiredIds.contains(b.getId()));
            }
            vo.setBadges(allBadges);
        } else {
            // 프로필이 없는 경우 기본값 설정
            vo.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getName());
            vo.setEcoPoint(0);

            // 프로필 없을 때 통계값 0.0 초기화 (Null 방지)
            vo.setTotalWater(0.0);
            vo.setTotalCarbon(0.0);
            vo.setTotalEnergy(0.0);
        }

        // 4. 포맷팅
        vo.setFormattedTotalWater(formatNumber(vo.getTotalWater(), "#,##0.0"));
        vo.setFormattedTotalCarbon(formatNumber(vo.getTotalCarbon(), "#,##0.0"));
        vo.setFormattedTotalEnergy(formatNumber(vo.getTotalEnergy(), "#,##0"));

        return vo;
    }

    public void updateInfo(MypageVo vo) {
        // 데이터 유효성 검사, 트랜잭션 관리 등 비즈니스 로직이 이곳에 들어갑니다.
        // mypageDao.updateUserInfo(vo); // 필요 시 구현
    }

    /**
     * 범용적으로 숫자를 포맷하는 private 유틸리티 메서드
     */
    private String formatNumber(double value, String pattern) {
        DecimalFormat df = new DecimalFormat(pattern,
                new java.text.DecimalFormatSymbols(Locale.KOREA));
        return df.format(value);
    }

    public String saveProfileImage(String userId, MultipartFile file) throws Exception {
        // 1. 파일 저장 경로 설정
        String projectDir = System.getProperty("user.dir");
        String uploadDirSrc = projectDir + "/src/main/resources/static/uploads/";
        String uploadDirTarget = projectDir + "/target/classes/static/uploads/";

        java.io.File directorySrc = new java.io.File(uploadDirSrc);
        if (!directorySrc.exists())
            directorySrc.mkdirs();

        java.io.File directoryTarget = new java.io.File(uploadDirTarget);
        if (!directoryTarget.exists())
            directoryTarget.mkdirs();

        // 2. 고유한 파일 이름 생성
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedFileName = UUID.randomUUID().toString() + extension;

        // 3. 파일 저장 (Src 및 Target 모두 저장하여 즉시 반영)
        java.io.File destFileSrc = new java.io.File(uploadDirSrc + savedFileName);
        file.transferTo(destFileSrc);

        java.nio.file.Files.copy(destFileSrc.toPath(), new java.io.File(uploadDirTarget + savedFileName).toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // 4. DB 업데이트를 위한 URL 생성
        String webAccessibleUrl = "/uploads/" + savedFileName;

        // 5. DB 업데이트
        // userId는 이메일 또는 아이디일 수 있음
        if (userId != null) {
            User user = userMapper.findByEmailOrLoginId(userId).orElse(null);
            if (user != null) {
                // Update User Profile
                UserProfile profile = userProfileMapper.findById(user.getId()).orElse(null);
                if (profile != null) {
                    profile.setAvatarUrl(webAccessibleUrl);
                    userProfileMapper.update(profile);
                }

                // Update User Table (for Header consistency)
                user.setMemImg(webAccessibleUrl);
                userMapper.update(user);
            }
        }

        return webAccessibleUrl;
    }

    // List of all available Eco Missions
    private static final java.util.List<com.rebirth.my.domain.EcoTodoTask> ALL_MISSIONS = new java.util.ArrayList<>();

    static {
        // 1. Waste & Recycling
        addMission("WASTE_01", "오늘 발생한 쓰레기에서 1개라도 정확한 분리배출 해보기", 10);
        addMission("WASTE_02", "의류 소재(면/폴리에스터/모직) 확인하고 분리하여 세탁", 15);
        addMission("WASTE_03", "버리려는 옷 1벌은 재활용 수거함에 분리", 20);
        addMission("WASTE_04", "플라스틱 용기 깨끗이 헹구고 라벨 제거", 10);
        addMission("WASTE_05", "종이·비닐 혼합 포장재 재질 별로 분리하기", 10);
        addMission("WASTE_06", "배달 음식 주문 시 “수저·젓가락 제외” 옵션 선택", 10);
        addMission("WASTE_07", "오늘 하루 일회용컵 대신 텀블러 사용", 20);
        addMission("WASTE_08", "택배 박스를 오늘 하나라도 올바르게 접어서 배출", 10);

        // 2. Energy Saving
        addMission("ENERGY_01", "사용하지 않는 방의 불 전부 끄기", 10);
        addMission("ENERGY_02", "전자기기 충전 완료되면 충전기 바로 뽑기", 10);
        addMission("ENERGY_03", "냉난방 대신 적정 실내온도 유지(20–22℃)", 15);
        addMission("ENERGY_04", "엘리베이터 대신 계단 1번 이상 사용", 20);
        addMission("ENERGY_05", "컴퓨터 화면 밝기 20% 낮추기", 10);
        addMission("ENERGY_06", "불필요한 멀티탭 스위치 OFF 하기", 10);
        addMission("ENERGY_07", "세탁 시 에코/절전 모드 한번 사용해보기", 15);
        addMission("ENERGY_08", "외출 시 대기전력 차단 체크", 10);

        // 3. Lifestyle & Mobility
        addMission("LIFE_01", "가까운 거리는 걷기", 20);
        addMission("LIFE_02", "1회라도 대중교통 이용하기", 20);
        addMission("LIFE_03", "1시간 중 10분은 휴대전화 사용 줄이고 휴식", 10);
        addMission("LIFE_04", "오늘 구매할 물건 1개는 불필요하면 미루기(NO BUY)", 30);
        addMission("LIFE_05", "외식 시 잔반 남기지 않기", 20);
        addMission("LIFE_06", "장보기 시 비닐봉투 대신 장바구니 사용", 15);
        addMission("LIFE_07", "음식물 쓰레기 발생량 기록해보기", 15);
        addMission("LIFE_08", "종이 영수증 대신 전자영수증 요청", 10);
    }

    private static void addMission(String code, String title, int points) {
        com.rebirth.my.domain.EcoTodoTask task = new com.rebirth.my.domain.EcoTodoTask();
        task.setId((long) (ALL_MISSIONS.size() + 1)); // Temporary ID
        task.setCode(code);
        task.setTitle(title);
        task.setDefaultPoints(points);
        task.setIsActive("Y");
        ALL_MISSIONS.add(task);
    }

    /**
     * Get 3 random missions for the day based on the current date.
     * This ensures all users see the same missions on the same day, or you can use
     * userId to randomize per user.
     * Here we randomize per day.
     */
    public java.util.List<com.rebirth.my.domain.EcoTodoTask> getDailyMissions() {
        long seed = java.time.LocalDate.now().toEpochDay();
        java.util.Random random = new java.util.Random(seed);

        java.util.List<com.rebirth.my.domain.EcoTodoTask> dailyMissions = new java.util.ArrayList<>(ALL_MISSIONS);
        java.util.Collections.shuffle(dailyMissions, random);

        return dailyMissions.subList(0, Math.min(3, dailyMissions.size()));
    }

    public com.rebirth.my.domain.EcoTodoTask getTaskById(Long id) {
        return ALL_MISSIONS.stream()
                .filter(t -> t.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 사용자가 소유한 장식 코드 목록 조회
     */
    public List<String> getOwnedItemCodes(Long userId) {
        return mypageDao.selectOwnedItemCodes(userId);
    }

    /**
     * 장식 소유 정보 추가
     */
    public void addOwnedDecoration(Long userId, String itemCode) {
        List<String> owned = mypageDao.selectOwnedItemCodes(userId);
        if (!owned.contains(itemCode)) {
            mypageDao.insertOwnedDecoration(userId, itemCode);
        }
    }

    /**
     * 에코 포인트 적립 (기부, 판매 완료 등)
     */
    @org.springframework.transaction.annotation.Transactional
    public void addEcoPoints(Long userId, int points) {
        UserProfile profile = userProfileMapper.findById(userId).orElse(null);
        if (profile != null) {
            int currentPoints = profile.getEcoPoints() != null ? profile.getEcoPoints() : 0;
            profile.setEcoPoints(currentPoints + points);
            userProfileMapper.update(profile);

            // 포인트 변경 후 뱃지 조건 체크가 필요하다면 여기서 호출
            // badgeService.checkAndAwardBadges(userId);
        }
    }
}