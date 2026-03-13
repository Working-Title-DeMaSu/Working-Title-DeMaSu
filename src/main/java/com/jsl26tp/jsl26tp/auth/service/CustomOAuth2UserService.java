package com.jsl26tp.jsl26tp.auth.service;

import com.jsl26tp.jsl26tp.auth.domain.User;
import com.jsl26tp.jsl26tp.auth.mapper.UserMapper;
import com.jsl26tp.jsl26tp.config.CustomUserDetails;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Google OAuth2 ログイン処理
 *
 * - 既存ユーザー → ログイン
 * - 新規ユーザー → 自動会員登録 → ログイン
 */
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public CustomOAuth2UserService(UserMapper userMapper, @Lazy PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Google 프로필 정보 추출
        String socialType = "GOOGLE";
        String socialId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // DB에서 기존 Google 유저 조회
        User user = userMapper.findBySocial(socialType, socialId);

        if (user == null) {
            // 동일 이메일로 이미 가입된 계정이 있는지 확인
            User existingByEmail = userMapper.findByEmail(email);
            if (existingByEmail != null) {
                throw new OAuth2AuthenticationException(
                        "このメールアドレスは既に登録されています。通常ログインをご利用ください。"
                );
            }

            // 신규 유저 자동 회원가입
            user = new User();
            user.setUsername("google_" + socialId);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            user.setNickname(name != null ? name : "User_" + socialId.substring(0, 6));
            user.setEmail(email);
            user.setSocialType(socialType);
            user.setSocialId(socialId);
            user.setRole("ROLE_USER");
            user.setStatus("ACTIVE");
            user.setIconUrl(picture != null ? picture : "/img/default.png");

            // 닉네임 중복 처리
            if (userMapper.findByNickname(user.getNickname()) != null) {
                user.setNickname(user.getNickname() + "_" + socialId.substring(0, 4));
            }

            userMapper.insertUser(user);

            // INSERT 후 ID를 받기 위해 다시 조회
            user = userMapper.findBySocial(socialType, socialId);
        }

        // CustomUserDetails 생성 (폼 로그인과 동일한 Principal)
        CustomUserDetails userDetails = new CustomUserDetails(user);
        userDetails.setAttributes(oAuth2User.getAttributes());
        return userDetails;
    }
}
