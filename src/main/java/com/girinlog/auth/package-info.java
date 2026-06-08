/**
 * 인증 도메인 — User / GitHub OAuth. (BE-A 담당, conventions/coding.md 1절)
 *
 * <p>한 도메인 안에서만 controller / service / domain / repository 로 나눈다.
 * 도메인 간 직접 의존 금지 — 필요하면 공개 서비스 인터페이스로만 호출한다.
 */
package com.girinlog.auth;
