package com.girinlog.persona.domain;

/**
 * 원천 입력 분석 상태. BLOG_URL만 분석 대상이며, FAILED여도 설문 기반 Persona 생성은 가능하다.
 */
public enum AnalysisStatus {
    PENDING,
    ANALYZING,
    COMPLETED,
    FAILED
}
