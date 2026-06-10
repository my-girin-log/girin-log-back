package com.girinlog.persona.analysis;

import java.util.Optional;

/**
 * 블로그 링크에서 Persona 생성에 쓸 본문 텍스트를 추출하는 포트.
 * 실패(접근 불가·차단·파싱 실패)해도 예외를 던지지 않고 빈 값을 준다(온보딩 비차단).
 */
public interface BlogAnalyzer {

    Optional<String> fetchReadableText(String blogUrl);
}
