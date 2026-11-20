
package com.contia.controller;

import com.contia.model.Despesa;
import com.contia.service.DespesaService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    private final DespesaService despesaService;

    public DespesaController(DespesaService despesaService) {
        this.despesaService = despesaService;
    }

    @PostMapping
    public DespesaResposta registrar(
            @RequestBody DespesaRequest request) {

        Despesa despesa = despesaService.registrar(
                request.estabelecimentoId(),
                request.descricao(),
                request.categoria(),
                request.valor()
        );

        return converterResposta(despesa);
    }

    @GetMapping("/estabelecimento/{estabelecimentoId}")
    public List<DespesaResposta> listar(
            @PathVariable Long estabelecimentoId) {

        return despesaService.listarPorEstabelecimento(estabelecimentoId)
                .stream()
                .map(this::converterResposta)
                .toList();
    }

    @PutMapping("/{despesaId}")
    public DespesaResposta atualizar(
            @PathVariable Long despesaId,
            @RequestBody DespesaRequest request) {
        return converterResposta(despesaService.atualizar(
                despesaId,
                request.estabelecimentoId(),
                request.descricao(),
                request.categoria(),
                request.valor()));
    }

    @DeleteMapping("/{despesaId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long despesaId,
            @RequestParam Long estabelecimentoId) {
        despesaService.excluir(despesaId, estabelecimentoId);
        return ResponseEntity.noContent().build();
    }

    private DespesaResposta converterResposta(Despesa despesa) {
        return new DespesaResposta(
                despesa.getId(),
                despesa.getDescricao(),
                despesa.getCategoria(),
                despesa.getValor(),
                despesa.getDataHora(),
                despesa.getEstabelecimento().getId()
        );
    }

    public record DespesaRequest(
            Long estabelecimentoId,
            String descricao,
            String categoria,
            BigDecimal valor
    ) {}

    public record DespesaResposta(
            Long id,
            String descricao,
            String categoria,
            BigDecimal valor,
            LocalDateTime dataHora,
            Long estabelecimentoId
    ) {}
}

