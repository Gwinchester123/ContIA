
package com.contia.controller;

import com.contia.model.Venda;
import com.contia.service.VendaService;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaService vendaService;

    public VendaController(VendaService vendaService) {
        this.vendaService = vendaService;
    }

    @PostMapping
    public VendaResposta registrar(@RequestBody VendaRequest request) {

        Venda venda = vendaService.registrar(
                request.estabelecimentoId(),
                request.produto(),
                request.quantidade(),
                request.valorUnitario()
        );

        return converterResposta(venda);
    }

    @GetMapping("/estabelecimento/{estabelecimentoId}")
    public List<VendaResposta> listar(
            @PathVariable Long estabelecimentoId) {

        return vendaService.listarPorEstabelecimento(estabelecimentoId)
                .stream()
                .map(this::converterResposta)
                .toList();
    }

    @PutMapping("/{vendaId}")
    public VendaResposta atualizar(
            @PathVariable Long vendaId,
            @RequestBody VendaRequest request) {
        return converterResposta(vendaService.atualizar(
                vendaId,
                request.estabelecimentoId(),
                request.produto(),
                request.quantidade(),
                request.valorUnitario()));
    }

    @DeleteMapping("/{vendaId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long vendaId,
            @RequestParam Long estabelecimentoId) {
        vendaService.excluir(vendaId, estabelecimentoId);
        return ResponseEntity.noContent().build();
    }

    private VendaResposta converterResposta(Venda venda) {
        return new VendaResposta(
                venda.getId(),
                venda.getProduto(),
                venda.getQuantidade(),
                venda.getValorUnitario(),
                venda.getTotal(),
                venda.getDataHora(),
                venda.getEstabelecimento().getId()
        );
    }

    public record VendaRequest(
            Long estabelecimentoId,
            String produto,
            Integer quantidade,
            BigDecimal valorUnitario
    ) {}

    public record VendaResposta(
            Long id,
            String produto,
            Integer quantidade,
            BigDecimal valorUnitario,
            BigDecimal total,
            LocalDateTime dataHora,
            Long estabelecimentoId
    ) {}
}

