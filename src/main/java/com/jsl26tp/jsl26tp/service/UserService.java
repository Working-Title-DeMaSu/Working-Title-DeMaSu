package com.jsl26tp.jsl26tp.service;

import com.jsl26tp.jsl26tp.common.BusinessException;
import com.jsl26tp.jsl26tp.common.ErrorCode;
import com.jsl26tp.jsl26tp.domain.User;
import com.jsl26tp.jsl26tp.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    // 회원가입
    public void register(User user) {
        // ✅ 중복 체크 → BusinessException으로 통일
        if (userMapper.findByUsername(user.getUsername()) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        if (userMapper.findByNickname(user.getNickname()) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        if (userMapper.findByEmail(user.getEmail()) != null) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (user.getPassword().length() < 8) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }

        // 비밀번호 BCrypt 암호화
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 기본값 설정
        if (user.getRole() == null) {
            user.setRole("ROLE_USER");
        }
        if (user.getSocialType() == null) {
            user.setSocialType("LOCAL");
        }
        if (user.getIconUrl() == null || user.getIconUrl().isEmpty()) {
            user.setIconUrl("/img/default.png");
        }
        if (user.getStatus() == null) {
            user.setStatus("ACTIVE");
        }

        userMapper.insertUser(user);
    }

    // username 중복 체크
    public boolean isUsernameTaken(String username) {
        return userMapper.findByUsername(username) != null;
    }

    // 닉네임 중복 체크
    public boolean isNicknameTaken(String nickname) {
        return userMapper.findByNickname(nickname) != null;
    }

    // 이메일 중복 체크
    public boolean isEmailTaken(String email) {
        return userMapper.findByEmail(email) != null;
    }

    // 회원 정보 조회
    public User findById(Long id) {
        User user = userMapper.findById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    // 회원 정보 수정
    public void updateUser(User user) {
        userMapper.updateUser(user);
    }

    // 비밀번호 변경
    public void updatePassword(Long id, String newPassword) {
        if (newPassword.length() < 8) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        userMapper.updatePassword(id, passwordEncoder.encode(newPassword));
    }
}
