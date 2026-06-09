package com.girinlog.persona.generation;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 임시 Persona 생성기(MVP). 실제 LLM 연동 전까지 입력으로부터 결정적 플레이스홀더를 만든다.
 * TODO(공유/silok): 실록이 LLM 어댑터로 교체. 이 빈은 그때 @Primary 어댑터로 대체된다.
 */
@Component
public class StubPersonaGenerator implements PersonaGenerator {

    @Override
    public GeneratedPersona generate(PersonaGenerationInput input) {
        int answerCount = input.answers() == null ? 0 : input.answers().size();
        boolean hasWriting = (input.blogUrl() != null && !input.blogUrl().isBlank())
                || (input.rawText() != null && !input.rawText().isBlank());

        String basis = hasWriting ? "기존 글과 설문" : "온보딩 설문";
        return new GeneratedPersona(
                "담백한",
                "사건 → 감정 → 판단 순서로 정리",
                List.of("회고", "학습"),
                "핵심부터 짧게 정리",
                "무엇을 배웠고 다음에 무엇을 다르게 할지",
                "짧고 담백한 글",
                basis + " " + answerCount + "문항을 바탕으로 한 임시 Persona 요약(LLM 연동 전).",
                "# Persona (임시)\n\n- 말투: 담백한\n- 기준: 배운 점과 다음 행동\n");
    }
}
