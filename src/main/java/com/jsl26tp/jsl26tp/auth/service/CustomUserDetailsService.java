package com.jsl26tp.jsl26tp.auth.service;

import com.jsl26tp.jsl26tp.config.CustomUserDetails;
import com.jsl26tp.jsl26tp.auth.domain.User;
import com.jsl26tp.jsl26tp.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Spring Security가 로그인 시 사용자 정보를 DB에서 조회하는 서비스
// UserDetailsService 인터페이스 구현 필수

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // 1. DB에서 사용자 조회 (이메일로 로그인)
        User user = userMapper.findByEmail(username);

        // 2. 사용자가 없으면 예외 발생
        if (user == null) {
            throw new UsernameNotFoundException("ユーザーが見つかりません: " + username);
        }

        System.out.println("LOGIN USER: " + user.getUsername() + " / ROLE: " + user.getRole());

        // 3. CustomUserDetails 반환
        return new CustomUserDetails(user);
    }
}
