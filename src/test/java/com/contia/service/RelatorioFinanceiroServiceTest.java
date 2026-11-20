package com.contia.service;

import com.contia.model.Estabelecimento;
import com.contia.model.Despesa;
import com.contia.model.Venda;
import com.contia.repository.DespesaRepository;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.repository.VendaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RelatorioFinanceiroServiceTest {
    private VendaRepository vendas;
    private DespesaRepository despesas;
    private EstabelecimentoRepository estabelecimentos;
    private RelatorioFinanceiroService service;

    @BeforeEach
    void configurar() {
        vendas = mock(VendaRepository.class);
        despesas = mock(DespesaRepository.class);
        estabelecimentos = mock(EstabelecimentoRepository.class);
        service = new RelatorioFinanceiroService(vendas, despesas, estabelecimentos);
        Estabelecimento estabelecimento = mock(Estabelecimento.class);
        when(estabelecimento.getNome()).thenReturn("Café de teste");
        when(estabelecimentos.findByIdAndProprietarioId(15L, 7L)).thenReturn(Optional.of(estabelecimento));
        when(vendas.findByEstabelecimentoIdAndDataHoraBetween(eq(15L), any(), any())).thenReturn(List.of());
        when(despesas.findByEstabelecimentoIdAndDataHoraBetween(eq(15L), any(), any())).thenReturn(List.of());
    }

    @Test
    void consultaSomenteEstabelecimentoDoProprietarioEIntervaloInclusivo() {
        service.analisar(7L, 15L, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));

        verify(estabelecimentos, times(2)).findByIdAndProprietarioId(15L, 7L);
        verify(vendas).findByEstabelecimentoIdAndDataHoraBetween(15L,
                LocalDate.of(2026, 3, 10).atStartOfDay(),
                LocalDate.of(2026, 3, 13).atStartOfDay().minusNanos(1));
    }

    @Test
    void bloqueiaEstabelecimentoDeOutroProprietario() {
        when(estabelecimentos.findByIdAndProprietarioId(15L, 99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> service.analisar(99L, 15L, LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12)));
        verifyNoInteractions(vendas, despesas);
    }

    @Test
    void geraArquivoPdfValido() {
        LocalDate inicio = LocalDate.of(2026, 3, 10);
        LocalDate fim = LocalDate.of(2026, 3, 12);
        byte[] essencial = service.gerarPdf(7L, 15L, inicio, fim, false);
        byte[] completo = service.gerarPdf(7L, 15L, inicio, fim, true);

        assertTrue(essencial.length > 100);
        assertTrue(completo.length > essencial.length);
        assertTrue(new String(essencial, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
        assertTrue(new String(completo, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
    }

    @Test
    void calculaVendasDespesasSaldoECategoriasComOsRegistrosConsultados() {
        LocalDate inicio = LocalDate.of(2026, 3, 10);
        LocalDate fim = LocalDate.of(2026, 3, 12);
        Venda venda = mock(Venda.class);
        when(venda.getTotal()).thenReturn(new BigDecimal("100.00"));
        when(venda.getDataHora()).thenReturn(inicio.atTime(12, 0));
        Despesa despesa = mock(Despesa.class);
        when(despesa.getCategoria()).thenReturn("Insumos");
        when(despesa.getValor()).thenReturn(new BigDecimal("35.00"));
        when(vendas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay().minusNanos(1)))
                .thenReturn(List.of(venda));
        when(despesas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicio.atStartOfDay(), fim.plusDays(1).atStartOfDay().minusNanos(1)))
                .thenReturn(List.of(despesa));

        var analise = service.analisar(7L, 15L, inicio, fim);

        assertEquals(new BigDecimal("100.00"), analise.faturamento());
        assertEquals(new BigDecimal("35.00"), analise.despesas());
        assertEquals(new BigDecimal("65.00"), analise.resultado());
        assertEquals(new BigDecimal("35.00"), analise.categorias().get("Insumos"));
    }

    @Test
    void pdfCompletoIncluiMovimentacoesRegistradas() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 3, 10);
        LocalDate fim = LocalDate.of(2026, 3, 12);
        var inicioData = inicio.atStartOfDay();
        var fimData = fim.plusDays(1).atStartOfDay().minusNanos(1);
        Venda venda = mock(Venda.class);
        when(venda.getProduto()).thenReturn("Bolo caseiro");
        when(venda.getQuantidade()).thenReturn(2);
        when(venda.getTotal()).thenReturn(new BigDecimal("100.00"));
        when(venda.getDataHora()).thenReturn(inicio.atTime(12, 0));
        Despesa despesa = mock(Despesa.class);
        when(despesa.getDescricao()).thenReturn("Farinha de trigo");
        when(despesa.getCategoria()).thenReturn("Insumos");
        when(despesa.getValor()).thenReturn(new BigDecimal("20.00"));
        when(despesa.getDataHora()).thenReturn(inicio.atTime(13, 0));
        when(vendas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicioData, fimData)).thenReturn(List.of(venda));
        when(despesas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicioData, fimData)).thenReturn(List.of(despesa));

        byte[] pdf = service.gerarPdf(7L, 15L, inicio, fim, true);
        var leitor = new com.lowagie.text.pdf.PdfReader(pdf);
        String texto = new com.lowagie.text.pdf.parser.PdfTextExtractor(leitor).getTextFromPage(1);

        assertTrue(texto.contains("Bolo caseiro"));
        assertTrue(texto.contains("Farinha de trigo"));
        assertTrue(texto.contains("Insumos"));
        leitor.close();
    }
}
