package com.contia.service;

import com.contia.model.Estabelecimento;
import com.contia.model.Despesa;
import com.contia.model.Venda;
import com.contia.model.VendaItem;
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
        byte[] pdf = service.gerarPdf(7L, 15L, inicio, fim);

        assertTrue(pdf.length > 100);
        assertTrue(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).startsWith("%PDF-"));
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
        when(despesa.getDataHora()).thenReturn(inicio.atTime(13, 0));
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
    void calculaCustosOperacionaisMargemEvolucaoComDadosDoPeriodo() {
        LocalDate inicio = LocalDate.of(2026, 3, 10);
        LocalDate fim = LocalDate.of(2026, 3, 12);
        Venda primeiraVenda = mock(Venda.class);
        when(primeiraVenda.getTotal()).thenReturn(new BigDecimal("100.00"));
        when(primeiraVenda.getDataHora()).thenReturn(inicio.atTime(10, 0));
        Venda segundaVenda = mock(Venda.class);
        when(segundaVenda.getTotal()).thenReturn(new BigDecimal("50.00"));
        when(segundaVenda.getDataHora()).thenReturn(inicio.plusDays(1).atTime(11, 0));
        Despesa ingredientes = despesa("Ingredientes", "20.00", inicio.atTime(12, 0));
        Despesa aluguel = despesa("Aluguel", "30.00", inicio.plusDays(1).atTime(12, 0));
        when(vendas.findByEstabelecimentoIdAndDataHoraBetween(eq(15L), any(), any()))
                .thenReturn(List.of(primeiraVenda, segundaVenda));
        when(despesas.findByEstabelecimentoIdAndDataHoraBetween(eq(15L), any(), any()))
                .thenReturn(List.of(ingredientes, aluguel));

        var analise = service.analisar(7L, 15L, inicio, fim);

        assertEquals(new BigDecimal("150.00"), analise.faturamento());
        assertEquals(new BigDecimal("20.00"), analise.custosInsumos());
        assertEquals(new BigDecimal("30.00"), analise.despesasOperacionais());
        assertEquals(new BigDecimal("50.00"), analise.despesas());
        assertEquals(new BigDecimal("100.00"), analise.resultado());
        assertEquals(new BigDecimal("66.67"), analise.margemResultadoPercentual());
        assertEquals(2, analise.evolucao().size());
        assertEquals(new BigDecimal("100.00"), analise.evolucao().get(0).vendas());
        assertEquals(new BigDecimal("20.00"), analise.evolucao().get(0).custosInsumos());
        assertEquals(new BigDecimal("30.00"), analise.evolucao().get(1).despesasOperacionais());
    }

    @Test
    void periodoSemMovimentacaoTemMargemZeroESemRecomendacoesFicticias() {
        var analise = service.analisar(7L, 15L,
                LocalDate.of(2026, 3, 10), LocalDate.of(2026, 3, 12));

        assertEquals(BigDecimal.ZERO.setScale(2), analise.margemResultadoPercentual());
        assertEquals(BigDecimal.ZERO, analise.faturamento());
        assertEquals(BigDecimal.ZERO, analise.despesas());
        assertTrue(analise.alertas().isEmpty());
        assertTrue(analise.recomendacoes().isEmpty());
        assertTrue(analise.evolucao().isEmpty());
        assertFalse(analise.temComparacao());
    }

    @Test
    void comparacaoDoPeriodoAnteriorMantemResultadoEMargem() {
        LocalDate inicio = LocalDate.of(2026, 3, 10);
        LocalDate fim = LocalDate.of(2026, 3, 12);
        LocalDateTime inicioAtual = inicio.atStartOfDay();
        LocalDateTime fimAtual = fim.plusDays(1).atStartOfDay().minusNanos(1);
        LocalDateTime inicioAnterior = inicio.minusDays(3).atStartOfDay();
        LocalDateTime fimAnterior = inicio.minusDays(1).atTime(23, 59, 59, 999_999_999);
        Venda atual = venda("100.00", inicio.atTime(10, 0));
        Venda anterior = venda("200.00", inicio.minusDays(1).atTime(10, 0));
        Despesa atualDespesa = despesa("Aluguel", "80.00", inicio.atTime(12, 0));
        Despesa anteriorDespesa = despesa("Aluguel", "100.00", inicio.minusDays(1).atTime(12, 0));
        when(vendas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicioAtual, fimAtual)).thenReturn(List.of(atual));
        when(despesas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicioAtual, fimAtual)).thenReturn(List.of(atualDespesa));
        when(vendas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicioAnterior, fimAnterior)).thenReturn(List.of(anterior));
        when(despesas.findByEstabelecimentoIdAndDataHoraBetween(15L, inicioAnterior, fimAnterior)).thenReturn(List.of(anteriorDespesa));

        var analise = service.analisar(7L, 15L, inicio, fim);

        assertTrue(analise.temComparacao());
        assertEquals(new BigDecimal("200.00"), analise.faturamentoAnterior());
        assertEquals(new BigDecimal("100.00"), analise.despesasAnteriores());
        assertEquals(new BigDecimal("100.00"), analise.resultadoAnterior());
        assertEquals(new BigDecimal("50.00"), analise.margemResultadoAnteriorPercentual());
        assertTrue(analise.alertas().stream().anyMatch(texto -> texto.contains("caíram")));
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

        byte[] pdf = service.gerarPdf(7L, 15L, inicio, fim);
        var leitor = new com.lowagie.text.pdf.PdfReader(pdf);
        String texto = new com.lowagie.text.pdf.parser.PdfTextExtractor(leitor).getTextFromPage(1);

        assertTrue(texto.contains("Bolo caseiro"));
        assertTrue(texto.contains("Farinha de trigo"));
        assertTrue(texto.contains("Insumos"));
        leitor.close();
    }

    @Test
    void pdfCompletoListaCadaItemDaVendaSemAlterarOTotalDoRelatorio() throws Exception {
        LocalDate inicio = LocalDate.of(2026, 3, 10);
        LocalDate fim = LocalDate.of(2026, 3, 12);
        Venda venda = new Venda();
        venda.setTotal(new BigDecimal("11.50"));
        venda.setDataHora(inicio.atTime(12, 0));
        VendaItem cafe = new VendaItem();
        cafe.setProduto("Café");
        cafe.setQuantidade(1);
        cafe.setValorUnitario(new BigDecimal("5.00"));
        cafe.setSubtotal(new BigDecimal("5.00"));
        venda.adicionarItem(cafe);
        VendaItem pao = new VendaItem();
        pao.setProduto("Pão francês");
        pao.setQuantidade(2);
        pao.setValorUnitario(new BigDecimal("3.25"));
        pao.setSubtotal(new BigDecimal("6.50"));
        venda.adicionarItem(pao);
        when(vendas.findByEstabelecimentoIdAndDataHoraBetween(eq(15L), any(), any())).thenReturn(List.of(venda));

        byte[] pdf = service.gerarPdf(7L, 15L, inicio, fim);
        var leitor = new com.lowagie.text.pdf.PdfReader(pdf);
        String texto = new com.lowagie.text.pdf.parser.PdfTextExtractor(leitor).getTextFromPage(1);

        assertTrue(texto.contains("Café"));
        assertTrue(texto.contains("Pão francês"));
        assertEquals(2, texto.split("Venda", -1).length - 1, "cada item deve ter sua própria linha de venda");
        assertTrue(texto.contains("5,00"));
        assertTrue(texto.contains("6,50"));
        assertEquals(new BigDecimal("11.50"), service.analisar(7L, 15L, inicio, fim).faturamento());
        leitor.close();
    }

    private Venda venda(String total, LocalDateTime dataHora) {
        Venda venda = mock(Venda.class);
        when(venda.getTotal()).thenReturn(new BigDecimal(total));
        when(venda.getDataHora()).thenReturn(dataHora);
        return venda;
    }

    private Despesa despesa(String categoria, String valor, LocalDateTime dataHora) {
        Despesa despesa = mock(Despesa.class);
        when(despesa.getCategoria()).thenReturn(categoria);
        when(despesa.getValor()).thenReturn(new BigDecimal(valor));
        when(despesa.getDataHora()).thenReturn(dataHora);
        return despesa;
    }
}
