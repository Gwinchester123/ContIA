package com.contia.controller;

import com.contia.model.Proprietario;
import com.contia.security.PerfilAcesso;
import com.contia.security.SessaoTokenService;
import com.contia.security.UsuarioAutenticado;
import com.contia.service.ProprietarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proprietarios")
public class ProprietarioController {

    private final ProprietarioService proprietarioService;
    private final SessaoTokenService tokenService;

    public ProprietarioController(ProprietarioService proprietarioService, SessaoTokenService tokenService) {
        this.proprietarioService = proprietarioService;
        this.tokenService = tokenService;
    }

    @GetMapping("/me")
    public ProprietarioContaResposta minhaConta(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        Proprietario proprietario = proprietarioService.buscarPorId(usuario.id());
        return new ProprietarioContaResposta(proprietario.getId(), proprietario.getNome(), proprietario.getEmail());
    }

    @PutMapping("/senha")
    public ResponseEntity<Void> alterarSenha(@RequestBody AlterarSenhaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        proprietarioService.alterarSenha(usuario.id(), request.senhaAtual(), request.novaSenha());
        tokenService.revogar(PerfilAcesso.PROPRIETARIO, usuario.id());
        return ResponseEntity.noContent().build();
    }

    public record ProprietarioContaResposta(Long id, String nome, String email) {}
    public record AlterarSenhaRequest(String senhaAtual, String novaSenha) {}

    @PostMapping
    public ResponseEntity<ProprietarioResposta> cadastrar(
            @RequestBody Proprietario proprietario) {

        Proprietario salvo = proprietarioService.cadastrar(proprietario);

        ProprietarioResposta resposta = new ProprietarioResposta(
                salvo.getId(),
                salvo.getNome(),
                salvo.getEmail(),
                salvo.getTelefone(),
                tokenService.emitir(UsuarioAutenticado.proprietario(salvo.getId())),
                PerfilAcesso.PROPRIETARIO.name()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    public record ProprietarioResposta(
            Long id,
            String nome,
            String email,
            String telefone,
            String token,
            String perfil
    ) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErro(IllegalArgumentException erro) {
        return ResponseEntity.badRequest().body(erro.getMessage());
    }

    @PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest dados) {

    Proprietario proprietario = proprietarioService.autenticar(
            dados.email(),
            dados.senha()
    );

    LoginResposta resposta = new LoginResposta(
            proprietario.getId(),
            proprietario.getNome(),
            proprietario.getEmail(),
            proprietario.getTelefone(),
            tokenService.emitir(UsuarioAutenticado.proprietario(proprietario.getId())),
            PerfilAcesso.PROPRIETARIO.name()
    );

    return ResponseEntity.ok(resposta);
}

public record LoginRequest(String email, String senha) {
}

public record LoginResposta(Long id, String nome, String email, String telefone, String token, String perfil) {
}
}
