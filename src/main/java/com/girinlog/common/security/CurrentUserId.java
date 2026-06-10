package com.girinlog.common.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 현재 인증된 사용자의 id(=JWT subject)를 컨트롤러 파라미터로 주입한다.
 *
 * <pre>{@code
 * @GetMapping("/me")
 * public UserResponse getMe(@CurrentUserId Long userId) { ... }
 * }</pre>
 *
 * <p>{@code @AuthenticationPrincipal Jwt} 에서 subject 를 꺼내는 중복을 없앤다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUserId {
}
