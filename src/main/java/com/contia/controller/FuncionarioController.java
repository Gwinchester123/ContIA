package com.contia.controller;

import com.contia.model.Funcionario;
import com.contia.security.PerfilAcesso;
import com.contia.security.SessaoTokenService;
import com.contia.security.UsuarioAutenticado;
import com.contia.service.FuncionarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/funcionarios")
public class FuncionarioController {
    private final FuncionarioService funcionarioService;
    private final SessaoTokenService tokenService;

    public FuncionarioController(FuncionarioService funcionarioService, SessaoTokenService tokenService) {
        this.funcionarioService = funcionarioService;
        this.tokenService = tokenService;
    }

    @PostMapping
    public ResponseEntity<FuncionarioResposta> cadastrar(@RequestBody FuncionarioCadastroRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Funcionario funcionario = funcionarioService.cadastrar(usuario.id(), request.estabelecimentoId(),
                request.nome(), request.email(), request.senha());
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta(funcionario));
    }

    @GetMapping
    public List<FuncionarioResposta> listar(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return funcionarioService.listar(usuario.id()).stream().map(this::resposta).toList();
    }

    @GetMapping("/{funcionarioId}")
    public FuncionarioResposta consultar(@PathVariable Long funcionarioId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return resposta(funcionarioService.consultar(usuario.id(), funcionarioId));
    }

    @PutMapping("/{funcionarioId}")
    public FuncionarioResposta atualizar(@PathVariable Long funcionarioId,
            @RequestBody FuncionarioAtualizacaoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return resposta(funcionarioService.atualizar(usuario.id(), funcionarioId,
                request.nome(), request.email(), request.senha()));
    }

    @PatchMapping("/{funcionarioId}/situacao")
    public FuncionarioResposta alterarSituacao(@PathVariable Long funcionarioId,
            @RequestBody SituacaoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return resposta(funcionarioService.alterarSituacao(usuario.id(), funcionarioId, request.ativo()));
    }

    @PostMapping("/login")
    public LoginResposta login(@RequestBody LoginRequest request) {
        Funcionario funcionario = funcionarioService.autenticar(request.email(), request.senha());
        UsuarioAutenticado usuario = new UsuarioAutenticado(funcionario.getId(), PerfilAcesso.FUNCIONARIO,
                funcionario.getEstabelecimento().getId());
        return new LoginResposta(funcionario.getId(), funcionario.getNome(), funcionario.getEmail(),
                funcionario.getEstabelecimento().getId(), PerfilAcesso.FUNCIONARIO.name(), tokenService.emitir(usuario));
    }

    private FuncionarioResposta resposta(Funcionario funcionario) {
        return new FuncionarioResposta(funcionario.getId(), funcionario.getNome(), funcionario.getEmail(),
                funcionario.getEstabelecimento().getId(), PerfilAcesso.FUNCIONARIO.name(), funcionario.isAtivo());
    }

    public record FuncionarioCadastroRequest(String nome, String email, String senha, Long estabelecimentoId) {}
    public record FuncionarioAtualizacaoRequest(String nome, String email, String senha) {}
    public record SituacaoRequest(boolean ativo) {}
    public record LoginRequest(String email, String senha) {}
    public record FuncionarioResposta(Long id, String nome, String email, Long estabelecimentoId,
            String perfil, boolean ativo) {}
    public record LoginResposta(Long id, String nome, String email, Long estabelecimentoId, String perfil, String token) {}
}
