package com.interviewcoach.service;

import com.interviewcoach.utils.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testMd5() {
        String pwd = "123456";
        String md5 = DigestUtils.md5DigestAsHex(pwd.getBytes(StandardCharsets.UTF_8));
        assertEquals("e10adc3949ba59abbe56e057f20f883e", md5);
    }

    @Test
    void testJwtGenerateAndValidate() {
        String token = JwtUtil.generate(1L, "test");
        assertNotNull(token);
        assertTrue(JwtUtil.validate(token));
        assertEquals(1L, JwtUtil.getUserId(token));
        assertEquals("test", JwtUtil.getUsername(token));
    }

    @Test
    void testInvalidToken() {
        assertFalse(JwtUtil.validate("invalid.token.here"));
        assertNull(JwtUtil.getUserId("invalid"));
    }
}
