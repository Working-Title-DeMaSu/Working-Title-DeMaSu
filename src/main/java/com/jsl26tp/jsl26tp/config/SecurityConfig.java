package com.jsl26tp.jsl26tp.config;

import com.jsl26tp.jsl26tp.auth.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    // 비밀번호 암호화 (BCrypt: 단방향 해시, 복호화 불가)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .authorizeHttpRequests(auth -> auth

                // 로그인 없이 누구나 접근 가능 (비회원도 지도 조회 가능)
                .requestMatchers(
                    "/",                    // 메인 페이지 (지도)
                    "/login",               // 로그인
                    "/register",            // 회원가입
                    "/api/toilets/**",      // 화장실 조회 API (비회원도 위치 조회 가능)
                    "/api/check-*",         // 중복 체크 API (회원가입 시 사용)
                    "/api/line/webhook",    // LINE 챗봇 Webhook
                    "/error",               // 에러 페이지 (리다이렉트 루프 방지)
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/img/**"
                ).permitAll()

                // 관리자 전용 페이지
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // 나머지는 로그인 필요 (리뷰, 신고, 문의 등)
                .anyRequest().authenticated()
            )

            // 로그인 설정
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login?error=true")
                .usernameParameter("email")
                .passwordParameter("password")
                .permitAll()
            )

            // 로그아웃 설정
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            )

            .userDetailsService(customUserDetailsService);

        return http.build();
    }
}