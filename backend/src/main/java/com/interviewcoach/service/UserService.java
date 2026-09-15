package com.interviewcoach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.interviewcoach.entity.User;
import com.interviewcoach.mapper.UserMapper;
import com.interviewcoach.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<UserMapper, User> {

    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    /**
     * 注册
     */
    public User register(String username, String password) {
        // 查重
        long count = count(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (count > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PASSWORD_ENCODER.encode(password));
        save(user);
        log.info("新用户注册: {}", username);
        return user;
    }

    /**
     * 登录，返回 JWT Token
     */
    public String login(String username, String password) {
        User user = getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null || !matches(password, user.getPassword())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (isLegacyMd5(user.getPassword())) {
            user.setPassword(PASSWORD_ENCODER.encode(password));
            updateById(user);
            log.info("用户 {} 的密码摘要已升级", username);
        }
        log.info("用户登录: {}", username);
        return JwtUtil.generate(user.getId(), user.getUsername());
    }

    /**
     * 修改密码
     */
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        User user = getById(userId);
        if (user == null) throw new IllegalArgumentException("用户不存在");
        if (!matches(oldPwd, user.getPassword())) throw new IllegalArgumentException("原密码错误");
        user.setPassword(PASSWORD_ENCODER.encode(newPwd));
        updateById(user);
        log.info("用户 {} 修改了密码", user.getUsername());
    }

    private String md5(String password) {
        return DigestUtils.md5DigestAsHex(password.getBytes(StandardCharsets.UTF_8));
    }

    private boolean matches(String rawPassword, String storedPassword) {
        if (storedPassword == null) return false;
        return isLegacyMd5(storedPassword)
                ? storedPassword.equalsIgnoreCase(md5(rawPassword))
                : PASSWORD_ENCODER.matches(rawPassword, storedPassword);
    }

    private boolean isLegacyMd5(String password) {
        return password != null && password.matches("(?i)^[a-f0-9]{32}$");
    }
}
