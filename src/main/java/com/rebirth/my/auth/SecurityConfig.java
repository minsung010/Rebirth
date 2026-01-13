package com.rebirth.my.auth;

import com.rebirth.my.auth.PendingUserFilter;
import com.rebirth.my.auth.OAuth2SuccessHandler;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 클래스
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

        private final CustomOAuth2UserService customOAuth2UserService;
        private final CustomLoginSuccessHandler customLoginSuccessHandler;
        private final OAuth2SuccessHandler oAuth2SuccessHandler;

        public SecurityConfig(CustomOAuth2UserService customOAuth2UserService,
                        CustomLoginSuccessHandler customLoginSuccessHandler,
                        OAuth2SuccessHandler oAuth2SuccessHandler) {
                this.customOAuth2UserService = customOAuth2UserService;
                this.customLoginSuccessHandler = customLoginSuccessHandler;
                this.oAuth2SuccessHandler = oAuth2SuccessHandler;
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf.disable()) // 개발 편의를 위해 CSRF 비활성화 (운영 환경에서는 활성화 권장)
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/", "/home", "/main", "/auth/**", "/api/**",
                                                                "/css/**", "/js/**",
                                                                "/images/**", "/img/**", "/video/**", "/error")
                                                .permitAll() // 로그인 페이지 및
                                                             // 정적 리소스 허용
                                                .anyRequest().authenticated() // 그 외 요청은 인증 필요
                                )
                                .oauth2Login(oauth2 -> oauth2
                                                .loginPage("/auth/login") // 커스텀 로그인 페이지 설정
                                                .userInfoEndpoint(userInfo -> userInfo
                                                                .userService(customOAuth2UserService) // 사용자 정보 처리 서비스
                                                                                                      // 등록
                                                )
                                                .successHandler(oAuth2SuccessHandler) // 커스텀 성공 핸들러 추가
                                // .defaultSuccessUrl("/main", true) // successHandler가 있으면 무시됨
                                )
                                .formLogin(form -> form
                                                .loginPage("/auth/login")
                                                .loginProcessingUrl("/auth/login")
                                                .usernameParameter("username")
                                                .passwordParameter("password")
                                                .successHandler(customLoginSuccessHandler)
                                                .defaultSuccessUrl("/main", true) // successHandler가 있으면 제거하거나 상충되지
                                                // 않게 설정 필요
                                                .failureUrl("/auth/login?error=true")
                                                .permitAll())
                                .logout(logout -> logout
                                                .logoutSuccessUrl("/main") // 로그아웃 성공 시 이동할 URL
                                                .invalidateHttpSession(true) // 세션 무효화
                                                .deleteCookies("JSESSIONID") // 쿠키 삭제
                                                .permitAll())
                                .addFilterAfter(new PendingUserFilter(), AnonymousAuthenticationFilter.class);

                return http.build();
        }
}
