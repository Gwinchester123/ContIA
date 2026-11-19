
package com.contia.controller;

import com.contia.model.Estabelecimento;
import com.contia.service.EstabelecimentoService;

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estabelecimentos")
@CrossOrigin(origins = "http://localhost:5173")
public class EstabelecimentoController {

    private final EstabelecimentoService estabelecimentoService;

    public EstabelecimentoController(
            EstabelecimentoService estabelecimentoService) {
        this.estabelecimentoService = estabelecimentoService;
    }

    @PostMapping
    public EstabelecimentoResposta cadastrar(
            @RequestBody EstabelecimentoRequest request) {

        Estabelecimento estabelecimento = new Estabelecimento();
        estabelecimento.setNome(request.nome());
        estabelecimento.setTipo(request.tipo());

        Estabelecimento salvo = estabelecimentoService.cadastrar(
                request.proprietarioId(), estabelecimento);

        return new EstabelecimentoResposta(
                salvo.getId(),
                salvo.getNome(),
                salvo.getTipo(),
                salvo.getProprietario().getId()
        );
    }

    @GetMapping("/proprietario/{proprietarioId}")
    public List<EstabelecimentoResposta> listar(
            @PathVariable Long proprietarioId) {

        return estabelecimentoService
                .listarPorProprietario(proprietarioId)
                .stream()
                .map(estabelecimento -> new EstabelecimentoResposta(
                        estabelecimento.getId(),
                        estabelecimento.getNome(),
                        estabelecimento.getTipo(),
                        estabelecimento.getProprietario().getId()
                ))
                .toList();
    }

    @PutMapping("/{estabelecimentoId}/nome")
public EstabelecimentoResposta atualizarNome(
        @PathVariable Long estabelecimentoId,
        @RequestBody AtualizarNomeRequest request) {

    Estabelecimento atualizado = estabelecimentoService.atualizarNome(
        estabelecimentoId,
        request.proprietarioId(),
        request.nome()
    );

    return new EstabelecimentoResposta(
        atualizado.getId(),
        atualizado.getNome(),
        atualizado.getTipo(),
        atualizado.getProprietario().getId()
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
            Long proprietarioId
    ) {}

    public record AtualizarNomeRequest(Long proprietarioId, String nome) {}
}

