package com.girinlog.silok;

import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.generation.PersonaGenerationInput;
import com.girinlog.persona.generation.PersonaGenerator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 임시 Persona 생성기(MVP). 실제 LLM 연동 전까지 입력으로부터 결정적 플레이스홀더를 만든다.
 * 포트({@link PersonaGenerator})는 persona 도메인 소유, 구현은 silok가 가진다(coding.md 8절).
 * TODO: 실록이 LLM 어댑터로 교체.
 */
@Component
class DefaultPersonaGenerator implements PersonaGenerator {

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
