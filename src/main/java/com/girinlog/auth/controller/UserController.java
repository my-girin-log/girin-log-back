package com.girinlog.auth.controller;

import com.girinlog.auth.controller.dto.UpdateUserRequest;
import com.girinlog.auth.controller.dto.UserResponse;
import com.girinlog.auth.service.UserService;
import com.girinlog.common.security.CurrentUserId;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 정보 조회/수정. (openapi /api/users/me) 현재 사용자 id는 {@link CurrentUserId} 로 주입받는다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse getMe(@CurrentUserId Long userId) {
        return UserResponse.from(userService.getById(userId));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@CurrentUserId Long userId, @Valid @RequestBody UpdateUserRequest request) {
        return UserResponse.from(userService.changeNickname(userId, request.nickname()));
    }
}
