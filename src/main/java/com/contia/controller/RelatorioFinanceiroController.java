package com.contia.controller;

import com.contia.service.RelatorioFinanceiroService;
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

    public RelatorioFinanceiroController(RelatorioFinanceiroService relatorioService) {
        this.relatorioService = relatorioService;
    }

    @GetMapping("/analise")
    public RelatorioFinanceiroService.Analise analisar(
            @RequestParam Long proprietarioId,
            @RequestParam Long estabelecimentoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorioService.analisar(proprietarioId, estabelecimentoId, inicio, fim);
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> baixarPdf(
            @RequestParam Long proprietarioId,
            @RequestParam Long estabelecimentoId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(defaultValue = "false") boolean completo) {
        byte[] pdf = relatorioService.gerarPdf(proprietarioId, estabelecimentoId, inicio, fim, completo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=contia-relatorio-" + inicio + "-" + fim + ".pdf")
                .body(pdf);
    }
}
