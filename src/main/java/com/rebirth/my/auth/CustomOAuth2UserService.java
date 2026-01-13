package com.rebirth.my.auth;

import com.rebirth.my.domain.OAuthAccount;
import com.rebirth.my.domain.User;
import com.rebirth.my.domain.UserProfile;
import com.rebirth.my.mapper.OAuthAccountMapper;
import com.rebirth.my.mapper.UserMapper;
import com.rebirth.my.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * 소셜 로그인 시 사용자 정보를 처리하는 서비스 클래스입니다.
 * 각 소셜 제공자(Google, Kakao, Naver)의 사용자 정보를 통합적으로 처리합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;
    private final OAuthAccountMapper oAuthAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;

    /**
     * 소셜 로그인 완료 후 사용자 정보를 가져오는 메서드입니다.
     * 
     * @param userRequest OAuth2 사용자 요청 정보
     * @return OAuth2User 인증된 사용자 정보
     * @throws OAuth2AuthenticationException 인증 실패 시 예외 발생
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService를 통해 사용자 정보를 가져옵니다.
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // 2. 제공자(google, kakao, naver)와 속성 정보를 확인합니다.
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 3. 제공자에 따라 적절한 OAuth2UserInfo 객체를 생성합니다.
        OAuth2UserInfo oAuth2UserInfo = null;
        if (registrationId.equals("google")) {
            oAuth2UserInfo = new GoogleOAuth2UserInfo(attributes);
        } else if (registrationId.equals("kakao")) {
            oAuth2UserInfo = new KakaoOAuth2UserInfo(attributes);
        } else if (registrationId.equals("naver")) {
            oAuth2UserInfo = new NaverOAuth2UserInfo(attributes);
        } else {
            System.out.println("지원하지 않는 소셜 로그인 제공자입니다: " + registrationId);
            return oAuth2User;
        }

        // 4. 사용자 정보를 로그에 출력합니다. (디버깅 용도)
        System.out.println("소셜 로그인 제공자: " + oAuth2UserInfo.getProvider());
        System.out.println("사용자 이메일: " + oAuth2UserInfo.getEmail());
        System.out.println("사용자 이름: " + oAuth2UserInfo.getName());

        // 5. DB에 사용자 정보를 저장하거나 업데이트합니다.
        User user = saveOrUpdateUser(oAuth2UserInfo);

        String userNameAttributeName = userRequest.getClientRegistration().getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();

        return new CustomOAuth2User(user, attributes, userNameAttributeName);
    }

    private User saveOrUpdateUser(OAuth2UserInfo oAuth2UserInfo) {
        String provider = oAuth2UserInfo.getProvider();
        String providerUserId = oAuth2UserInfo.getProviderId();
        String email = oAuth2UserInfo.getEmail();
        String name = oAuth2UserInfo.getName();
        String imageUrl = oAuth2UserInfo.getImageUrl();

        // 1. OAuthAccount (기존 연동 계정) 확인
        Optional<OAuthAccount> oAuthAccountOptional = oAuthAccountMapper.findByProviderAndProviderUserId(provider,
                providerUserId);

        if (oAuthAccountOptional.isPresent()) {
            // 1-A. 이미 연동된 계정이 있는 경우 -> 로그인 처리
            Long userId = oAuthAccountOptional.get().getUserId();
            User user = userMapper.getUserById(userId); // PK로 조회

            // 사용자 정보 업데이트 로직 (이미지 등)
            if (user != null && imageUrl != null) {
                boolean shouldUpdate = false;
                String currentImg = user.getMemImg();

                if (currentImg == null || currentImg.isEmpty()) {
                    shouldUpdate = true;
                } else if (!currentImg.startsWith("/") && currentImg.startsWith("http")
                        && !currentImg.equals(imageUrl)) {
                    shouldUpdate = true;
                }

                if (shouldUpdate) {
                    user.setMemImg(imageUrl);
                    userMapper.update(user);

                    Optional<UserProfile> profileOpt = userProfileMapper.findById(user.getId());
                    if (profileOpt.isPresent()) {
                        UserProfile profile = profileOpt.get();
                        profile.setAvatarUrl(imageUrl);
                        userProfileMapper.update(profile);
                    }
                }
                updateLastLogin(user);
                return user;
            }
            // 만약 user가 null이면(DB 정합성 오류), 아래 로직으로 넘어가서 복구 시도
        }

        // 2. 연동된 계정이 없는 경우 -> 이메일 기반 자동 통합 시도
        User user;
        Optional<User> userOptional = Optional.empty();

        if (email != null && !email.isEmpty()) {
            userOptional = userMapper.findByEmail(email);
        }

        if (userOptional.isPresent()) {
            // 2-A. 같은 이메일을 가진 기존 회원이 존재함 -> 자동 통합 (계정 연동)
            user = userOptional.get();
            System.out.println("[Integration] 기존 계정 발견 및 자동 통합 진행: " + email + " (ID: " + user.getId() + ")");

            // 기존 유저 정보 업데이트 (이미지 등)
            if (imageUrl != null) {
                boolean shouldUpdate = false;
                String currentImg = user.getMemImg();

                if (currentImg == null || currentImg.isEmpty()) {
                    shouldUpdate = true;
                } else if (!currentImg.startsWith("/") && currentImg.startsWith("http")
                        && !currentImg.equals(imageUrl)) {
                    shouldUpdate = true;
                }

                if (shouldUpdate) {
                    user.setMemImg(imageUrl);
                    userMapper.update(user);

                    Optional<UserProfile> profileOpt = userProfileMapper.findById(user.getId());
                    if (profileOpt.isPresent()) {
                        UserProfile profile = profileOpt.get();
                        String currentProfileImg = profile.getAvatarUrl();
                        if (currentProfileImg == null || currentProfileImg.isEmpty()
                                || (!currentProfileImg.startsWith("/") && currentProfileImg.startsWith("http"))) {
                            profile.setAvatarUrl(imageUrl);
                            userProfileMapper.update(profile);
                        }
                    }
                }
            }
        } else {
            // 2-B. 기존 회원 없음 -> 신규 가입
            System.out.println("[New User] 신규 소셜 회원 가입 진행: " + email);
            user = createUser(email, name, imageUrl);
        }

        // 3. 소셜 계정 연동 정보 저장 (통합/가입 공통)
        createOAuthAccount(user, provider, providerUserId);
        updateLastLogin(user);

        return user;
    }

    private User createUser(String email, String name, String imageUrl) {
        User user = new User();
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        user.setLoginId("social_" + uuid);
        user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setName(name != null ? name : "Social User");
        user.setEmail(email);
        user.setRole("USER");
        user.setStatus("PENDING"); // 추가 정보 입력 전까지 PENDING 상태
        user.setMemImg(imageUrl); // 프로필 이미지 설정

        userMapper.save(user);

        // UserProfile 생성
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(user.getId());
        userProfile.setNickname(name);
        userProfile.setAvatarUrl(imageUrl);

        // 통계 값 초기화 (Null 방지)
        userProfile.setTotalWaterSavedL(0.0);
        userProfile.setTotalCarbonSavedKg(0.0);
        userProfile.setTotalCollectedKg(0.0);
        userProfile.setEcoPoints(0);
        userProfile.setTotalClothingCount(0);

        userProfileMapper.save(userProfile);

        return user;
    }

    private void createOAuthAccount(User user, String provider, String providerUserId) {
        OAuthAccount oAuthAccount = new OAuthAccount();
        oAuthAccount.setUserId(user.getId());
        oAuthAccount.setProvider(provider);
        oAuthAccount.setProviderUserId(providerUserId);
        oAuthAccountMapper.save(oAuthAccount);
    }

    private void updateLastLogin(User user) {
        System.out.println("=== OAuth2 Login Success: Updating Last Login ===");
        System.out.println("User ID: " + user.getId());
        try {
            userProfileMapper.updateLastLoginAt(user.getId(), LocalDateTime.now());
            System.out.println("LastLoginAt updated successfully via OAuth2");
        } catch (Exception e) {
            System.err.println("OAuth2 LastLoginAt update failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 소셜 로그인 제공자별로 상이한 사용자 정보를 통일된 방식으로 접근하기 위한 인터페이스입니다.
     */
    public interface OAuth2UserInfo {
        Map<String, Object> getAttributes();

        String getProviderId();

        String getProvider();

        String getEmail();

        String getName();

        String getImageUrl(); // 이미지 URL 추가
    }

    /**
     * 구글(Google) 사용자 정보를 처리하는 클래스입니다.
     */
    public static class GoogleOAuth2UserInfo implements OAuth2UserInfo {

        private Map<String, Object> attributes;

        public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String getProviderId() {
            return (String) attributes.get("sub");
        }

        @Override
        public String getProvider() {
            return "google";
        }

        @Override
        public String getEmail() {
            return (String) attributes.get("email");
        }

        @Override
        public String getName() {
            return (String) attributes.get("name");
        }

        @Override
        public String getImageUrl() {
            return (String) attributes.get("picture");
        }
    }

    /**
     * 카카오(Kakao) 사용자 정보를 처리하는 클래스입니다.
     * 카카오는 사용자 정보가 kakao_account 내부에 중첩되어 있습니다.
     */
    public static class KakaoOAuth2UserInfo implements OAuth2UserInfo {

        private Map<String, Object> attributes;
        private Map<String, Object> attributesAccount;
        private Map<String, Object> attributesProfile;

        @SuppressWarnings("unchecked")
        public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
            // kakao_account 키를 통해 계정 정보를 가져옵니다.
            this.attributesAccount = (Map<String, Object>) attributes.get("kakao_account");
            if (this.attributesAccount != null) {
                // profile 키를 통해 프로필 정보를 가져옵니다.
                this.attributesProfile = (Map<String, Object>) attributesAccount.get("profile");
            }
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String getProviderId() {
            return String.valueOf(attributes.get("id"));
        }

        @Override
        public String getProvider() {
            return "kakao";
        }

        @Override
        public String getEmail() {
            if (attributesAccount == null) {
                return null;
            }
            return (String) attributesAccount.get("email");
        }

        @Override
        public String getName() {
            if (attributesProfile == null) {
                return null;
            }
            return (String) attributesProfile.get("nickname");
        }

        @Override
        public String getImageUrl() {
            if (attributesProfile == null) {
                return null;
            }
            // 카카오는 profile_image_url 또는 thumbnail_image_url 제공
            return (String) attributesProfile.get("profile_image_url");
        }
    }

    /**
     * 네이버(Naver) 사용자 정보를 처리하는 클래스입니다.
     * 네이버는 사용자 정보가 response 객체 내부에 있습니다.
     */
    public static class NaverOAuth2UserInfo implements OAuth2UserInfo {

        private Map<String, Object> attributes;
        private Map<String, Object> attributesResponse;

        @SuppressWarnings("unchecked")
        public NaverOAuth2UserInfo(Map<String, Object> attributes) {
            this.attributes = attributes;
            // response 키를 통해 사용자 정보를 가져옵니다.
            this.attributesResponse = (Map<String, Object>) attributes.get("response");
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }

        @Override
        public String getProviderId() {
            if (attributesResponse == null) {
                return null;
            }
            return (String) attributesResponse.get("id");
        }

        @Override
        public String getProvider() {
            return "naver";
        }

        @Override
        public String getEmail() {
            if (attributesResponse == null) {
                return null;
            }
            return (String) attributesResponse.get("email");
        }

        @Override
        public String getName() {
            if (attributesResponse == null) {
                return null;
            }
            return (String) attributesResponse.get("name");
        }

        @Override
        public String getImageUrl() {
            if (attributesResponse == null) {
                return null;
            }
            return (String) attributesResponse.get("profile_image");
        }
    }
}
