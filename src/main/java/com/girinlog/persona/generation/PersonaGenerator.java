package com.girinlog.persona.generation;

/**
 * Persona 생성 포트. 실제 LLM 연동은 silok 모듈의 어댑터로 교체한다(coding.md 8절).
 */
public interface PersonaGenerator {

    GeneratedPersona generate(PersonaGenerationInput input);
}
