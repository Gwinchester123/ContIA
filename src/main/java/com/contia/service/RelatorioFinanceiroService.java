package com.contia.service;

import com.contia.model.Despesa;
import com.contia.model.Estabelecimento;
import com.contia.model.Venda;
import com.contia.repository.DespesaRepository;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.repository.VendaRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class RelatorioFinanceiroService {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal FATOR_ALERTA = new BigDecimal("1.10");
    private static final BigDecimal LIMIAR_MUDANCA_MARGEM = new BigDecimal("3.00");
    private static final Set<String> CATEGORIAS_CUSTO_INSUMO =
            Set.of("ingredientes", "materiais", "insumos", "embalagens");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final VendaRepository vendaRepository;
    private final DespesaRepository despesaRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public RelatorioFinanceiroService(VendaRepository vendaRepository,
            DespesaRepository despesaRepository,
            EstabelecimentoRepository estabelecimentoRepository) {
        this.vendaRepository = vendaRepository;
        this.despesaRepository = despesaRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    public Analise analisar(Long proprietarioId, Long estabelecimentoId, LocalDate inicio, LocalDate fim) {
        Dados periodo = carregar(proprietarioId, estabelecimentoId, inicio, fim);
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        Dados anterior = carregar(proprietarioId, estabelecimentoId, inicio.minusDays(dias), inicio.minusDays(1));

        List<String> alertas = new ArrayList<>();
        List<String> recomendacoes = new ArrayList<>();
        boolean temDadosParaAnalisar = !periodo.vendas().isEmpty() || !periodo.despesas().isEmpty()
                || !anterior.vendas().isEmpty() || !anterior.despesas().isEmpty();

        if (temDadosParaAnalisar) {
            if (anterior.totalDespesas().signum() > 0
                    && periodo.totalDespesas().compareTo(anterior.totalDespesas().multiply(FATOR_ALERTA)) > 0) {
                String categoria = categoriaComMaiorAumento(periodo, anterior);
                alertas.add(categoria == null
                        ? "As despesas aumentaram mais de 10% em relação ao período anterior. Os lançamentos não indicam a causa."
                        : "Os gastos com " + categoria + " aumentaram mais de 10% em relação ao período anterior.");
            }
            if (anterior.faturamento().signum() > 0
                    && periodo.faturamento().compareTo(anterior.faturamento().multiply(new BigDecimal("0.90"))) < 0) {
                alertas.add("As vendas caíram mais de 10% em relação ao período anterior. Os lançamentos não indicam a causa.");
            } else if (anterior.faturamento().signum() > 0
                    && periodo.faturamento().compareTo(anterior.faturamento()) > 0) {
                recomendacoes.add("As vendas registradas aumentaram em relação ao período anterior.");
            }

            if (periodo.faturamento().signum() > 0 && anterior.faturamento().signum() > 0) {
                BigDecimal margemAtual = margem(periodo.resultado(), periodo.faturamento());
                BigDecimal margemAnterior = margem(anterior.resultado(), anterior.faturamento());
                BigDecimal variacaoMargem = margemAtual.subtract(margemAnterior);
                if (variacaoMargem.abs().compareTo(LIMIAR_MUDANCA_MARGEM) >= 0) {
                    String direcao = variacaoMargem.signum() > 0 ? "subiu" : "caiu";
                    recomendacoes.add("A margem de resultado " + direcao + " "
                            + variacaoMargem.abs().setScale(1, RoundingMode.HALF_UP).toPlainString()
                            + " pontos percentuais. Confira os lançamentos de vendas e despesas.");
                }
            }

            Map<String, BigDecimal> categoriasAtuais = agruparCategorias(periodo.despesas());
            if (periodo.totalDespesas().signum() > 0 && !categoriasAtuais.isEmpty()) {
                Map.Entry<String, BigDecimal> principal = categoriasAtuais.entrySet().stream()
                        .max(Map.Entry.comparingByValue()).orElseThrow();
                BigDecimal participacao = percentual(principal.getValue(), periodo.totalDespesas());
                if (participacao.compareTo(new BigDecimal("40")) >= 0) {
                    recomendacoes.add("Avalie os gastos registrados em " + principal.getKey()
                            + ", que concentrou " + participacao.toPlainString() + "% das despesas deste período.");
                }
            }
        }

        BigDecimal margemAtual = margem(periodo.resultado(), periodo.faturamento());
        BigDecimal margemAnterior = margem(anterior.resultado(), anterior.faturamento());
        return new Analise(periodo.estabelecimento().getNome(), inicio, fim,
                periodo.faturamento(), periodo.custosInsumos(), periodo.despesasOperacionais(),
                periodo.totalDespesas(), periodo.resultado(), margemAtual,
                agruparCategorias(periodo.despesas()), periodo.vendas().size(), periodo.despesas().size(),
                diaExtremo(periodo.vendas(), inicio, fim, true), diaExtremo(periodo.vendas(), inicio, fim, false),
                alertas, recomendacoes, anterior.faturamento(), anterior.custosInsumos(),
                anterior.despesasOperacionais(), anterior.totalDespesas(), anterior.resultado(),
                margemAnterior, montarEvolucao(periodo.vendas(), periodo.despesas()),
                !anterior.vendas().isEmpty() || !anterior.despesas().isEmpty());
    }

    public byte[] gerarPdf(Long proprietarioId, Long estabelecimentoId, LocalDate inicio, LocalDate fim) {
        Analise analise = analisar(proprietarioId, estabelecimentoId, inicio, fim);
        Dados periodo = carregar(proprietarioId, estabelecimentoId, inicio, fim);
        return new RelatorioPdfRenderer().render(analise, periodo.vendas(), periodo.despesas());
    }

    private Dados carregar(Long proprietarioId, Long estabelecimentoId, LocalDate inicio, LocalDate fim) {
        if (inicio == null || fim == null || inicio.isAfter(fim)) {
            throw new IllegalArgumentException("Informe um período válido.");
        }
        Estabelecimento estabelecimento = estabelecimentoRepository.findByIdAndProprietarioId(estabelecimentoId, proprietarioId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado para este proprietário."));
        LocalDateTime inicioData = inicio.atStartOfDay();
        LocalDateTime fimData = fim.plusDays(1).atStartOfDay().minusNanos(1);
        List<Venda> vendas = vendaRepository.findByEstabelecimentoIdAndDataHoraBetween(estabelecimentoId, inicioData, fimData);
        List<Despesa> despesas = despesaRepository.findByEstabelecimentoIdAndDataHoraBetween(estabelecimentoId, inicioData, fimData);
        BigDecimal faturamento = vendas.stream().map(Venda::getTotal).reduce(ZERO, BigDecimal::add);
        BigDecimal totalDespesas = despesas.stream().map(Despesa::getValor).reduce(ZERO, BigDecimal::add);
        BigDecimal custosInsumos = despesas.stream().filter(d -> ehCustoInsumo(d.getCategoria()))
                .map(Despesa::getValor).reduce(ZERO, BigDecimal::add);
        BigDecimal despesasOperacionais = totalDespesas.subtract(custosInsumos);
        return new Dados(estabelecimento, vendas, despesas, faturamento, custosInsumos,
                despesasOperacionais, totalDespesas, faturamento.subtract(totalDespesas));
    }

    private boolean ehCustoInsumo(String categoria) {
        return CATEGORIAS_CUSTO_INSUMO.contains(categoria == null ? "" : categoria.trim().toLowerCase(PT_BR));
    }

    private BigDecimal margem(BigDecimal resultado, BigDecimal faturamento) {
        if (faturamento.signum() == 0) return ZERO.setScale(2, RoundingMode.HALF_UP);
        return resultado.multiply(new BigDecimal("100")).divide(faturamento, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal percentual(BigDecimal parte, BigDecimal total) {
        if (total.signum() == 0) return ZERO.setScale(1, RoundingMode.HALF_UP);
        return parte.multiply(new BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP);
    }

    private Map<String, BigDecimal> agruparCategorias(List<Despesa> despesas) {
        return despesas.stream().collect(Collectors.groupingBy(Despesa::getCategoria, LinkedHashMap::new,
                Collectors.mapping(Despesa::getValor, Collectors.reducing(ZERO, BigDecimal::add))));
    }

    private String categoriaComMaiorAumento(Dados atual, Dados anterior) {
        Map<String, BigDecimal> a = agruparCategorias(atual.despesas());
        Map<String, BigDecimal> b = agruparCategorias(anterior.despesas());
        return a.entrySet().stream().filter(e -> b.getOrDefault(e.getKey(), ZERO).signum() > 0)
                .filter(e -> e.getValue().compareTo(b.get(e.getKey()).multiply(FATOR_ALERTA)) > 0)
                .max(Comparator.comparing(Map.Entry::getValue)).map(Map.Entry::getKey).orElse(null);
    }

    private List<EvolucaoDia> montarEvolucao(List<Venda> vendas, List<Despesa> despesas) {
        Map<LocalDate, EvolucaoAcumulada> dias = new TreeMap<>();
        for (Venda venda : vendas) {
            EvolucaoAcumulada dia = dias.computeIfAbsent(venda.getDataHora().toLocalDate(), ignorado -> new EvolucaoAcumulada());
            dia.vendas = dia.vendas.add(venda.getTotal());
        }
        for (Despesa despesa : despesas) {
            EvolucaoAcumulada dia = dias.computeIfAbsent(despesa.getDataHora().toLocalDate(), ignorado -> new EvolucaoAcumulada());
            dia.despesas = dia.despesas.add(despesa.getValor());
            if (ehCustoInsumo(despesa.getCategoria())) dia.custosInsumos = dia.custosInsumos.add(despesa.getValor());
            else dia.despesasOperacionais = dia.despesasOperacionais.add(despesa.getValor());
        }
        return dias.entrySet().stream().map(e -> new EvolucaoDia(e.getKey(), e.getValue().vendas,
                e.getValue().despesas, e.getValue().custosInsumos, e.getValue().despesasOperacionais)).toList();
    }

    private DiaFaturamento diaExtremo(List<Venda> vendas, LocalDate inicio, LocalDate fim, boolean maior) {
        if (vendas.isEmpty() || inicio.equals(fim)) return null;
        Map<LocalDate, BigDecimal> porDia = vendas.stream().collect(Collectors.groupingBy(v -> v.getDataHora().toLocalDate(),
                Collectors.mapping(Venda::getTotal, Collectors.reducing(ZERO, BigDecimal::add))));
        Map.Entry<LocalDate, BigDecimal> extremo = null;
        for (LocalDate data = inicio; !data.isAfter(fim); data = data.plusDays(1)) {
            Map.Entry<LocalDate, BigDecimal> candidato = Map.entry(data, porDia.getOrDefault(data, ZERO));
            if (extremo == null || (maior
                    ? candidato.getValue().compareTo(extremo.getValue()) > 0
                    : candidato.getValue().compareTo(extremo.getValue()) < 0)) {
                extremo = candidato;
            }
        }
        return extremo == null ? null : new DiaFaturamento(
                extremo.getKey().getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR),
                extremo.getKey(), extremo.getValue());
    }

    private record Dados(Estabelecimento estabelecimento, List<Venda> vendas, List<Despesa> despesas,
            BigDecimal faturamento, BigDecimal custosInsumos, BigDecimal despesasOperacionais,
            BigDecimal totalDespesas, BigDecimal resultado) {}

    private static final class EvolucaoAcumulada {
        private BigDecimal vendas = ZERO;
        private BigDecimal despesas = ZERO;
        private BigDecimal custosInsumos = ZERO;
        private BigDecimal despesasOperacionais = ZERO;
    }

    public record DiaFaturamento(String dia, LocalDate data, BigDecimal valor) {}
    public record EvolucaoDia(LocalDate data, BigDecimal vendas, BigDecimal despesas,
            BigDecimal custosInsumos, BigDecimal despesasOperacionais) {}
    public record Analise(String estabelecimento, LocalDate inicio, LocalDate fim,
            BigDecimal faturamento, BigDecimal custosInsumos, BigDecimal despesasOperacionais,
            BigDecimal despesas, BigDecimal resultado, BigDecimal margemResultadoPercentual,
            Map<String, BigDecimal> categorias, int quantidadeVendas, int quantidadeDespesas,
            DiaFaturamento diaMaiorFaturamento, DiaFaturamento diaMenorFaturamento,
            List<String> alertas, List<String> recomendacoes,
            BigDecimal faturamentoAnterior, BigDecimal custosInsumosAnteriores,
            BigDecimal despesasOperacionaisAnteriores, BigDecimal despesasAnteriores,
            BigDecimal resultadoAnterior, BigDecimal margemResultadoAnteriorPercentual,
            List<EvolucaoDia> evolucao, boolean temComparacao) {}
}
