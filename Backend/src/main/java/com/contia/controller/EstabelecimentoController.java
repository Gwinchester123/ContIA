
package com.contia.controller;

import com.contia.model.Estabelecimento;
import com.contia.service.EstabelecimentoService;
import com.contia.security.UsuarioAutenticado;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estabelecimentos")
public class EstabelecimentoController {

    private final EstabelecimentoService estabelecimentoService;

    public EstabelecimentoController(
            EstabelecimentoService estabelecimentoService) {
        this.estabelecimentoService = estabelecimentoService;
    }

    @PostMapping
    public EstabelecimentoResposta cadastrar(
            @RequestBody EstabelecimentoRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        Estabelecimento estabelecimento = new Estabelecimento();
        estabelecimento.setNome(request.nome());
        estabelecimento.setTipo(request.tipo());

        Estabelecimento salvo = estabelecimentoService.cadastrar(
                usuario.id(), estabelecimento);

        return new EstabelecimentoResposta(
                salvo.getId(),
                salvo.getNome(),
                salvo.getTipo(),
                salvo.getProprietario().getId(),
                salvo.getPlano().name()
        );
    }

    @GetMapping("/proprietario/{proprietarioId}")
    public List<EstabelecimentoResposta> listar(
            @PathVariable Long proprietarioId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {

        if (!proprietarioId.equals(usuario.id())) {
            throw new AccessDeniedException("Acesso ao proprietário não autorizado.");
        }

        return estabelecimentoService
                .listarPorProprietario(proprietarioId)
                .stream()
                .map(estabelecimento -> new EstabelecimentoResposta(
                        estabelecimento.getId(),
                        estabelecimento.getNome(),
                        estabelecimento.getTipo(),
                        estabelecimento.getProprietario().getId(),
                        estabelecimento.getPlano().name()
                ))
                .toList();
    }

    @PutMapping("/{estabelecimentoId}/nome")
public EstabelecimentoResposta atualizarNome(
        @PathVariable Long estabelecimentoId,
        @RequestBody AtualizarNomeRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario) {

    Estabelecimento atualizado = estabelecimentoService.atualizarNome(
        estabelecimentoId,
        usuario.id(),
        request.nome()
    );

    return new EstabelecimentoResposta(
        atualizado.getId(),
        atualizado.getNome(),
        atualizado.getTipo(),
        atualizado.getProprietario().getId(),
        atualizado.getPlano().name()
    );
}

    public record EstabelecimentoRequest(
            String nome,
            String tipo,
            Long proprietarioId
    ) {}

    public record EstabelecimentoResposta(
            Long id,
            String nome,
            String tipo,
            Long proprietarioId,
            String plano
    ) {}

    public record AtualizarNomeRequest(Long proprietarioId, String nome) {}
}

