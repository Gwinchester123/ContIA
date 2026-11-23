
package com.contia.controller;

import com.contia.model.Venda;
import com.contia.service.VendaService;
import com.contia.security.EscopoEstabelecimentoService;
import com.contia.security.UsuarioAutenticado;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vendas")
public class VendaController {

    private final VendaService vendaService;
    private final EscopoEstabelecimentoService escopoService;

    public VendaController(VendaService vendaService, EscopoEstabelecimentoService escopoService) {
        this.vendaService = vendaService;
        this.escopoService = escopoService;
    }

    @PostMapping
    public VendaResposta registrar(@RequestBody VendaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(request.estabelecimentoId(), usuario);
        Venda venda = vendaService.registrar(request.estabelecimentoId(), itens(request), usuario);
        return converterResposta(venda);
    }

    @GetMapping
    public List<VendaResposta> listarTodas(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return vendaService.listar(usuario).stream().map(this::converterResposta).toList();
    }

    @GetMapping("/sugestoes")
    public List<SugestaoResposta> sugerirItens(@RequestParam Long estabelecimentoId,
            @RequestParam(defaultValue = "") String q,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(estabelecimentoId, usuario);
        return vendaService.sugerirItens(estabelecimentoId, q, usuario).stream()
                .map(item -> new SugestaoResposta(item.produto(), item.ultimoPreco()))
                .toList();
    }

    @GetMapping("/estabelecimento/{estabelecimentoId}")
    public List<VendaResposta> listar(
            @PathVariable Long estabelecimentoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(estabelecimentoId, usuario);

        return vendaService.listarPorEstabelecimento(estabelecimentoId, usuario)
                .stream()
                .map(this::converterResposta)
                .toList();
    }

    @PutMapping("/{vendaId}")
    public VendaResposta atualizar(
            @PathVariable Long vendaId,
            @RequestBody VendaRequest request,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(request.estabelecimentoId(), usuario);
        return converterResposta(vendaService.atualizar(
                vendaId,
                request.estabelecimentoId(),
                itens(request),
                usuario));
    }

    @DeleteMapping("/{vendaId}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long vendaId,
            @RequestParam Long estabelecimentoId,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        escopoService.validar(estabelecimentoId, usuario);
        vendaService.excluir(vendaId, estabelecimentoId, usuario);
        return ResponseEntity.noContent().build();
    }

    private VendaResposta converterResposta(Venda venda) {
        List<ItemResposta> itens = venda.getItens().isEmpty()
                ? List.of(new ItemResposta(null, venda.getProduto(), venda.getQuantidade(),
                        venda.getValorUnitario(), venda.getTotal()))
                : venda.getItens().stream()
                        .map(item -> new ItemResposta(item.getId(), item.getProduto(), item.getQuantidade(),
                                item.getValorUnitario(), item.getSubtotal()))
                        .toList();
        ItemResposta primeiro = itens.get(0);
        Long responsavelId = venda.getFuncionarioResponsavel() != null
                ? venda.getFuncionarioResponsavel().getId()
                : venda.getProprietarioResponsavel() != null ? venda.getProprietarioResponsavel().getId() : null;
        String responsavelNome = venda.getFuncionarioResponsavel() != null
                ? venda.getFuncionarioResponsavel().getNome()
                : venda.getProprietarioResponsavel() != null ? venda.getProprietarioResponsavel().getNome() : null;
        String responsavelPerfil = venda.getFuncionarioResponsavel() != null ? "FUNCIONARIO"
                : venda.getProprietarioResponsavel() != null ? "PROPRIETARIO" : null;
        return new VendaResposta(
                venda.getId(),
                venda.getEstabelecimento().getId(),
                venda.getDataHora(),
                itens,
                venda.getTotal(),
                responsavelId,
                responsavelNome,
                responsavelPerfil,
                primeiro.produto(),
                primeiro.quantidade(),
                primeiro.valorUnitario()
        );
    }

    private List<VendaService.ItemEntrada> itens(VendaRequest request) {
        boolean temCamposLegados = request.produto() != null || request.quantidade() != null
                || request.valorUnitario() != null;
        if (request.itens() != null && !request.itens().isEmpty()) {
            if (temCamposLegados) {
                throw new IllegalArgumentException("Envie os itens em apenas um formato.");
            }
            return request.itens().stream()
                    .map(item -> item == null ? null
                            : new VendaService.ItemEntrada(item.produto(), item.quantidade(), item.valorUnitario()))
                    .toList();
        }
        if (temCamposLegados) {
            return List.of(new VendaService.ItemEntrada(request.produto(), request.quantidade(), request.valorUnitario()));
        }
        throw new IllegalArgumentException("A venda deve conter pelo menos um item.");
    }

    public record VendaRequest(
            Long estabelecimentoId,
            List<ItemRequest> itens,
            String produto,
            Integer quantidade,
            BigDecimal valorUnitario,
            BigDecimal total
    ) {}

    public record ItemRequest(String produto, Integer quantidade, BigDecimal valorUnitario, BigDecimal subtotal) {}

    public record ItemResposta(Long id, String produto, Integer quantidade, BigDecimal valorUnitario, BigDecimal subtotal) {}

    public record SugestaoResposta(String produto, BigDecimal ultimoPreco) {}

    public record VendaResposta(
            Long id,
            Long estabelecimentoId,
            LocalDateTime dataHora,
            List<ItemResposta> itens,
            BigDecimal total,
            Long responsavelId,
            String responsavelNome,
            String responsavelPerfil,
            String produto,
            Integer quantidade,
            BigDecimal valorUnitario
    ) {}
}

