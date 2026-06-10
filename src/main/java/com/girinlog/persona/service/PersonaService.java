package com.girinlog.persona.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.persona.domain.Persona;
import com.girinlog.persona.exception.PersonaErrorCode;
import com.girinlog.persona.repository.PersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class PersonaService {

    private final PersonaRepository personaRepository;

    public PersonaService(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    /** /api/personas/me 용. 없으면 404. */
    public Persona getByUserId(Long userId) {
        return personaRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(PersonaErrorCode.PERSONA_NOT_FOUND));
    }

    /**
     * 다른 도메인(예: Retrospective)이 회고 입력으로 쓰는 persona.md 접점.
     * persona가 없거나 markdown이 비어도 **차단하지 않고 빈 값**을 준다
     * (회고 생성은 persona가 없어도 graceful, 2026-06-09 결정). 도메인 간 호출은 이 공개 메서드로만.
     */
    public Optional<String> findPersonaMarkdown(Long userId) {
        return personaRepository.findByUserId(userId)
                .map(Persona::markdown)
                .filter(markdown -> markdown != null && !markdown.isBlank());
    }
}
