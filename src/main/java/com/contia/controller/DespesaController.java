
package com.contia.controller;

import com.contia.model.Despesa;
import com.contia.service.DespesaService;
import com.contia.security.EscopoEstabelecimentoService;
import com.contia.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/despesas")
public class DespesaController {

    private final DespesaService despesaService;
    private final EscopoEstabelecimentoService escopoService;

    public DespesaController(DespesaService despesaService, EscopoEstabelecimentoService escopoService) {
        this.despesaService = despesaService;
        this.escopoService = escopoService;
    }

    @PostMapping
    public DespesaResposta registrar(
            @RequestBody DespesaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(request.estabelecimentoId(), usuario);

        Despesa despesa = despesaService.registrar(
                request.estabelecimentoId(),
                request.descricao(),
                request.categoria(),
                request.valor()
        );

        return converterResposta(despesa);
    }

    @PostMapping("/lote")
    public ResponseEntity<List<DespesaResposta>> registrarLote(
            @RequestBody DespesaLoteRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(request.estabelecimentoId(), usuario);
        List<DespesaResposta> respostas = despesaService.registrarLote(
                        request.estabelecimentoId(),
                        request.despesas() == null ? null : request.despesas().stream()
                                .map(item -> item == null ? null : new DespesaService.DespesaEntrada(
                                        item.descricao(), item.categoria(), item.valor()))
                                .toList())
                .stream()
                .map(this::converterResposta)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(respostas);
    }

    @GetMapping("/estabelecimento/{estabelecimentoId}")
    public List<DespesaResposta> listar(
            @PathVariable Long estabelecimentoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(estabelecimentoId, usuario);

        return despesaService.listarPorEstabelecimento(estabelecimentoId)
                .stream()
                .map(this::converterResposta)
                .toList();
    }

    @PutMapping("/{despesaId}")
    public DespesaResposta atualizar(
            @PathVariable Long despesaId,
            @RequestBody DespesaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(request.estabelecimentoId(), usuario);
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
            @RequestParam Long estabelecimentoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(estabelecimentoId, usuario);
        despesaService.excluir(despesaId, estabelecimentoId);
        return ResponseEntity.noContent().build();
    }

    private DespesaResposta converterResposta(Despesa despesa) {
        return new DespesaResposta(
                despesa.getId(),
                despesa.getDescricao(),
                despesa.getCategoria(),
                despesa.getValor(),
                despesa.getDataHora().atZone(ZoneId.of("America/Fortaleza")).toOffsetDateTime(),
                despesa.getEstabelecimento().getId()
        );
    }

    public record DespesaRequest(
            Long estabelecimentoId,
            String descricao,
            String categoria,
            BigDecimal valor
    ) {}

    public record DespesaLoteRequest(Long estabelecimentoId, List<DespesaItemRequest> despesas) {}

    public record DespesaItemRequest(String descricao, String categoria, BigDecimal valor) {}

    public record DespesaResposta(
            Long id,
            String descricao,
            String categoria,
            BigDecimal valor,
            OffsetDateTime dataHora,
            Long estabelecimentoId
    ) {}
}

