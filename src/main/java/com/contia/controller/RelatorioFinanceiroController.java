package com.contia.controller;

import com.contia.service.RelatorioFinanceiroService;
import com.contia.security.UsuarioAutenticado;
import com.contia.service.AcessoPlanoService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/relatorios-financeiros")
public class RelatorioFinanceiroController {
    private final RelatorioFinanceiroService relatorioService;
    private final AcessoPlanoService acessoPlanoService;

    public RelatorioFinanceiroController(RelatorioFinanceiroService relatorioService,
            AcessoPlanoService acessoPlanoService) {
        this.relatorioService = relatorioService;
        this.acessoPlanoService = acessoPlanoService;
    }

    @GetMapping("/analise")
    public RelatorioFinanceiroService.Analise analisar(
            @RequestParam Long estabelecimentoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        acessoPlanoService.exigirPremium(estabelecimentoId, usuario);
        return relatorioService.analisar(usuario.id(), estabelecimentoId, inicio, fim);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> baixarPdf(
            @RequestParam Long estabelecimentoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        acessoPlanoService.exigirPremium(estabelecimentoId, usuario);
        byte[] pdf = relatorioService.gerarPdf(usuario.id(), estabelecimentoId, inicio, fim);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contia-relatorio-" + inicio + "-" + fim + ".pdf")
                .body(pdf);
    }
}
