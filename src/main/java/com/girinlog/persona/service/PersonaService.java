package com.girinlog.persona.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.persona.domain.Persona;
import com.girinlog.persona.exception.PersonaErrorCode;
import com.girinlog.persona.repository.PersonaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PersonaService {

    private final PersonaRepository personaRepository;

    public PersonaService(PersonaRepository personaRepository) {
        this.personaRepository = personaRepository;
    }

    public Persona getByUserId(Long userId) {
        return personaRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(PersonaErrorCode.PERSONA_NOT_FOUND));
    }
}
