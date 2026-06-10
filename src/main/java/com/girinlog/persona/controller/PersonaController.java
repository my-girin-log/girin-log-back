package com.girinlog.persona.controller;

import com.girinlog.common.security.CurrentUserId;
import com.girinlog.persona.controller.dto.PersonaResponse;
import com.girinlog.persona.service.PersonaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 Persona 조회. (openapi GET /api/personas/me)
 */
@RestController
@RequestMapping("/api/personas")
public class PersonaController {

    private final PersonaService personaService;

    public PersonaController(PersonaService personaService) {
        this.personaService = personaService;
    }

    @GetMapping("/me")
    public PersonaResponse getMyPersona(@CurrentUserId Long userId) {
        return PersonaResponse.from(personaService.getByUserId(userId));
    }
}
