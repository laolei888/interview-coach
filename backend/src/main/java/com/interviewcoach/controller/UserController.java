package com.interviewcoach.controller;

import com.interviewcoach.dto.Result;
import com.interviewcoach.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Result.badRequest("用户名和密码不能为空");
        }
        if (username.length() < 3 || username.length() > 20) {
            return Result.badRequest("用户名长度 3-20 位");
        }
        if (password.length() < 6) {
            return Result.badRequest("密码至少 6 位");
        }

        var user = userService.register(username, password);
        return Result.ok(Map.of("id", user.getId(), "username", user.getUsername()));
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return Result.badRequest("用户名和密码不能为空");
        }

        String token = userService.login(username, password);
        return Result.ok(Map.of("token", token, "username", username));
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/me")
    public Result<Map<String, Object>> me(@RequestAttribute("userId") Long userId) {
        var user = userService.getById(userId);
        return Result.ok(Map.of(
                "userId", user.getId(),
                "username", user.getUsername(),
                "avatar", avatarValue(user.getAvatar()),
                "gender", user.getGender() != null ? user.getGender() : "保密",
                "email", user.getEmail() != null ? user.getEmail() : ""
        ));
    }

    /**
     * 更新个人资料
     */
    @PostMapping("/update-profile")
    public Result<?> updateProfile(@RequestBody Map<String, String> body,
                                    @RequestAttribute("userId") Long userId) {
        var user = userService.getById(userId);
        String gender = body.getOrDefault("gender", "保密");
        String email = body.getOrDefault("email", "").trim();
        if (!Set.of("男", "女", "保密").contains(gender)) {
            return Result.badRequest("性别参数无效");
        }
        if (email.length() > 100 || (!email.isEmpty()
                && !email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))) {
            return Result.badRequest("邮箱格式无效");
        }
        user.setGender(gender);
        user.setEmail(email);
        userService.updateById(user);
        return Result.ok("保存成功");
    }

    /**
     * 上传头像
     */
    @PostMapping("/upload-avatar")
    public Result<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file,
                                                     @RequestAttribute("userId") Long userId) {
        if (file.isEmpty() || file.getOriginalFilename() == null) return Result.badRequest("请选择图片");
        if (file.getSize() > 2 * 1024 * 1024) return Result.badRequest("头像大小不能超过 2MB");
        String originalName = file.getOriginalFilename();
        String ext = extensionOf(originalName);
        if (!Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp").contains(ext)) {
            return Result.badRequest("仅支持 JPG、PNG、GIF、WEBP 图片");
        }
        String contentType = file.getContentType();
        if (contentType == null || !Set.of("image/jpeg", "image/png", "image/gif", "image/webp").contains(contentType)) {
            return Result.badRequest("图片类型无效");
        }
        String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(filename).toFile());
        } catch (IOException e) {
            log.error("头像上传失败", e);
            return Result.fail("上传失败: " + e.getMessage());
        }

        var user = userService.getById(userId);
        String previousAvatar = user.getAvatar();
        String avatarPath = "/api/user/avatar/" + filename;
        user.setAvatar(avatarPath);
        userService.updateById(user);
        deletePreviousAvatar(previousAvatar, userId, dir);

        return Result.ok(Map.of("avatar", avatarPath));
    }

    /**
     * 受 JWT 保护的头像访问接口，禁止用户读取其他账号的头像文件。
     */
    @GetMapping("/avatar/{filename:.+}")
    public ResponseEntity<Resource> avatar(@PathVariable String filename,
                                           @RequestAttribute("userId") Long userId) {
        String prefix = "avatar_" + userId + "_";
        if (!filename.startsWith(prefix) || filename.contains("/") || filename.contains("\\")
                || !filename.matches("avatar_\\d+_[a-f0-9-]+\\.(jpg|jpeg|png|gif|webp)")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Path avatarDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars")
                .toAbsolutePath().normalize();
        Path avatarPath = avatarDir.resolve(filename).normalize();
        if (!avatarPath.getParent().equals(avatarDir) || !Files.isRegularFile(avatarPath)) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = MediaTypeFactory.getMediaType(filename)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .body(new FileSystemResource(avatarPath));
    }

    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<?> changePassword(@RequestBody Map<String, String> body,
                                     @RequestAttribute("userId") Long userId) {
        String oldPwd = body.get("oldPassword");
        String newPwd = body.get("newPassword");
        if (oldPwd == null || newPwd == null || newPwd.length() < 6) {
            return Result.badRequest("新密码至少 6 位");
        }
        try {
            userService.changePassword(userId, oldPwd, newPwd);
            return Result.ok("密码修改成功");
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        }
    }

    private String avatarValue(String avatar) {
        if (avatar == null || avatar.isBlank()) {
            return "👤";
        }
        if (avatar.startsWith("/uploads/avatars/")) {
            String filename = avatar.substring("/uploads/avatars/".length());
            if (filename.matches("avatar_\\d+_[a-f0-9-]+\\.(jpg|jpeg|png|gif|webp)")) {
                return "/api/user/avatar/" + filename;
            }
            return "👤";
        }
        return avatar;
    }

    private String extensionOf(String originalName) {
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(dot).toLowerCase(Locale.ROOT);
    }

    private void deletePreviousAvatar(String avatar, Long userId, Path avatarDir) {
        if (avatar == null) return;
        String filename = avatar.substring(avatar.lastIndexOf('/') + 1);
        if (!filename.startsWith("avatar_" + userId + "_")) return;
        try {
            Files.deleteIfExists(avatarDir.resolve(filename).normalize());
        } catch (IOException e) {
            log.warn("旧头像清理失败: {}", filename, e);
        }
    }
}
