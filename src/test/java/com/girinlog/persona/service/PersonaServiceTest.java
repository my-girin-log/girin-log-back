package com.girinlog.persona.service;

import com.girinlog.common.error.BusinessException;
import com.girinlog.persona.domain.Persona;
import com.girinlog.persona.exception.PersonaErrorCode;
import com.girinlog.persona.generation.GeneratedPersona;
import com.girinlog.persona.repository.PersonaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonaServiceTest {

    @Mock
    private PersonaRepository personaRepository;

    @InjectMocks
    private PersonaService personaService;

    @Test
    @DisplayName("Persona가 없으면 PERSONA_NOT_FOUND")
    void getByUserIdThrowsWhenMissing() {
        when(personaRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> personaService.getByUserId(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(thrown -> assertThat(((BusinessException) thrown).errorCode())
                        .isEqualTo(PersonaErrorCode.PERSONA_NOT_FOUND));
    }

    @Test
    @DisplayName("Persona가 있으면 반환한다")
    void getByUserIdReturnsPersona() {
        Persona persona = Persona.create(1L, new GeneratedPersona(
                "담백한", "사건→감정", List.of("회고"), "핵심부터", "배운 점", "짧은 글", "요약", "# md"));
        when(personaRepository.findByUserId(1L)).thenReturn(Optional.of(persona));

        Persona found = personaService.getByUserId(1L);

        assertThat(found.tone()).isEqualTo("담백한");
    }
}
