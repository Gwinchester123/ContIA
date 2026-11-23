package com.contia.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {
    @GetMapping("/me")
    public UsuarioAutenticado usuarioAtual(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return usuario;
    }
}
