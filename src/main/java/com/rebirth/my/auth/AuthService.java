package com.rebirth.my.auth;

import com.rebirth.my.domain.User;
import com.rebirth.my.domain.UserProfile;
import com.rebirth.my.mapper.UserMapper;
import com.rebirth.my.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserMapper userMapper;
    private final UserProfileMapper userProfileMapper;
    private final com.rebirth.my.mapper.OAuthAccountMapper oAuthAccountMapper;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void join(JoinRequest joinRequest, jakarta.servlet.http.HttpSession session) {
        // 1. 이메일 인증 여부 검증 (서버 사이드)
        Boolean isVerified = (Boolean) session.getAttribute("emailVerified");
        if (isVerified == null || !isVerified) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        // 2. 중복 회원 검증
        if (userMapper.findByEmail(joinRequest.getEmail()).isPresent()) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        // 3. User 엔티티 생성
        User user = new User();
        user.setLoginId(joinRequest.getLoginId());
        user.setEmail(joinRequest.getEmail());
        user.setPassword(passwordEncoder.encode(joinRequest.getPassword()));
        user.setName(joinRequest.getNickname()); // 닉네임을 이름으로 초기 설정
        user.setRole("USER");
        user.setStatus("ACTIVE");
        user.setPhone(joinRequest.getPhone());
        user.setEmailVerifStatus("VERIFIED"); // 인증 상태 설정

        // 주소와 상세주소 분리 저장 (Geocoding 정확도를 위해)
        // address: 도로명/지번 주소만 (Geocoding에 사용)
        // addressDetail: 건물명, 층, 호수 등 상세정보 (화면 표시용)
        String mainAddress = "";
        if (joinRequest.getZipcode() != null && !joinRequest.getZipcode().isEmpty()) {
            mainAddress += "(" + joinRequest.getZipcode() + ") ";
        }
        mainAddress += joinRequest.getAddress() != null ? joinRequest.getAddress() : "";
        user.setAddress(mainAddress.trim());
        user.setAddressDetail(joinRequest.getDetailAddress());

        // Parse Resident Number for BirthDate and Gender
        String front = joinRequest.getResidentNumberFront();
        String back = joinRequest.getResidentNumberBack();

        if (front != null && front.length() == 6 && back != null && back.length() >= 1) {
            char genderCode = back.charAt(0);
            int yearPrefix = (genderCode == '1' || genderCode == '2' || genderCode == '5' || genderCode == '6') ? 1900
                    : 2000;
            int year = yearPrefix + Integer.parseInt(front.substring(0, 2));
            int month = Integer.parseInt(front.substring(2, 4));
            int day = Integer.parseInt(front.substring(4, 6));

            user.setBirthDate(java.time.LocalDateTime.of(year, month, day, 0, 0));
        }

        // 3. DB 저장
        userMapper.save(user);

        // 4. UserProfile 생성
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(user.getId());
        userProfile.setNickname(joinRequest.getNickname());

        if (back != null && back.length() >= 1) {
            char genderCode = back.charAt(0);
            if (genderCode == '1' || genderCode == '3' || genderCode == '5' || genderCode == '7') {
                userProfile.setGender("MALE");
            } else if (genderCode == '2' || genderCode == '4' || genderCode == '6' || genderCode == '8') {
                userProfile.setGender("FEMALE");
            }
        }

        userProfileMapper.save(userProfile);
    }

    public boolean checkLoginIdDuplicate(String loginId) {
        return userMapper.findByLoginId(loginId).isPresent();
    }

    public boolean checkEmailDuplicate(String email) {
        return userMapper.findByEmail(email).isPresent();
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public User updateSocialUser(Long tempUserId, JoinRequest joinRequest, jakarta.servlet.http.HttpSession session) {
        // 1. 이메일 인증 여부 검증
        Boolean isVerified = (Boolean) session.getAttribute("emailVerified");
        if (isVerified == null || !isVerified) {
            throw new IllegalStateException("이메일 인증이 완료되지 않았습니다.");
        }

        User tempUser = userMapper.getUserById(tempUserId);
        if (tempUser == null) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }

        // ============================================
        // 🚀 계정 통합 로직 추가
        // ============================================
        String inputEmail = joinRequest.getEmail() != null ? joinRequest.getEmail().trim() : "";
        Optional<User> targetUserOpt = userMapper.findByEmail(inputEmail);

        if (targetUserOpt.isPresent()) {
            User targetUser = targetUserOpt.get();
            if (!targetUser.getId().equals(tempUserId)) { // 본인이 아닌 경우만
                System.out.println("Processing Account Merge: TempUser(" + tempUserId + ") -> TargetUser("
                        + targetUser.getId() + ")");

                // 1. 소셜 계정 이동 (Current -> Target)
                // 먼저 해당 유저의 소셜 계정이 있는지 확인
                java.util.List<com.rebirth.my.domain.OAuthAccount> accounts = oAuthAccountMapper
                        .findByUserId(tempUserId);

                if (accounts.isEmpty()) {
                    System.err.println(
                            "CRITICAL: No OAuthAccount found for pending user! Merge might fail to link provider.");
                    throw new IllegalStateException("소셜 계정 정보를 찾을 수 없습니다. (ID: " + tempUserId + ")");
                } else {
                    int updatedCount = oAuthAccountMapper.updateUserId(tempUserId, targetUser.getId());
                    System.out.println("Transferred " + updatedCount + " OAuthAccounts to Target User.");

                    if (updatedCount == 0) {
                        System.err.println("CRITICAL: Update failed even though accounts were found!");
                        throw new IllegalStateException("소셜 계정 연동 이동 실패 (업데이트 0건)");
                    }
                }

                // 2. 임시 계정(tempUser) 삭제
                try {
                    userMapper.deleteById(tempUserId);
                    System.out.println("Successfully deleted pending user: " + tempUserId);
                } catch (Exception e) {
                    System.err.println("Failed to delete pending user (Non-Critical): " + e.getMessage());
                }

                // 4. Target User 반환 (컨텍스트 업데이트용)
                return targetUser;
            }
        }

        // 2. 추가 정보 업데이트 (신규 가입 로직)
        User user = tempUser; // tempUser를 user 변수로 참조
        user.setLoginId(joinRequest.getLoginId());
        user.setPassword(passwordEncoder.encode(joinRequest.getPassword()));
        user.setName(joinRequest.getNickname());
        user.setEmail(inputEmail); // 위에서 trim한 이메일 사용
        System.out.println("Saving Social User Email: " + inputEmail); // 디버깅용 로그

        if (inputEmail.isEmpty()) {
            throw new IllegalStateException("이메일 정보가 유효하지 않습니다.");
        }

        user.setPhone(joinRequest.getPhone());
        user.setStatus("ACTIVE");
        user.setEmailVerifStatus("VERIFIED");

        // Combine Address
        String fullAddress = "";
        if (joinRequest.getZipcode() != null && !joinRequest.getZipcode().isEmpty()) {
            fullAddress += "(" + joinRequest.getZipcode() + ") ";
        }
        fullAddress += joinRequest.getAddress();
        user.setAddress(fullAddress);
        user.setAddressDetail(joinRequest.getDetailAddress()); // 상세주소 저장 추가

        // BirthDate (YYYYMMDD)
        String birthDateStr = joinRequest.getBirthDateString();
        if (birthDateStr != null && birthDateStr.length() == 8) {
            int year = Integer.parseInt(birthDateStr.substring(0, 4));
            int month = Integer.parseInt(birthDateStr.substring(4, 6));
            int day = Integer.parseInt(birthDateStr.substring(6, 8));
            tempUser.setBirthDate(java.time.LocalDateTime.of(year, month, day, 0, 0));
        }

        userMapper.update(tempUser);

        // 3. UserProfile 업데이트
        UserProfile userProfile = userProfileMapper.findById(tempUserId).orElse(null);
        if (userProfile == null) {
            userProfile = new UserProfile();
            userProfile.setUserId(tempUserId);
        }

        userProfile.setNickname(joinRequest.getNickname());
        userProfile.setGender(joinRequest.getGender());

        if (userProfileMapper.findById(tempUserId).isEmpty()) {
            userProfileMapper.save(userProfile);
        } else {
            userProfileMapper.update(userProfile);
        }

        return tempUser;
    }

    public boolean isEmailRegistered(String email) {
        return userMapper.findByEmail(email).isPresent();
    }

    @Transactional
    public void updatePassword(String email, String newPassword) {
        User user = userMapper.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        user.setPassword(passwordEncoder.encode(newPassword));
        userMapper.update(user);
    }
}
