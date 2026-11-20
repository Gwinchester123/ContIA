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
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RelatorioFinanceiroService {
    private static final BigDecimal FATOR_ALERTA = new BigDecimal("1.10");

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
        long dias = java.time.temporal.ChronoUnit.DAYS.between(inicio, fim) + 1;
        Dados anterior = carregar(proprietarioId, estabelecimentoId, inicio.minusDays(dias), inicio.minusDays(1));

        List<String> alertas = new ArrayList<>();
        List<String> recomendacoes = new ArrayList<>();
        if (!anterior.vendas().isEmpty() || !anterior.despesas().isEmpty()) {
            if (anterior.totalDespesas().signum() > 0
                    && periodo.totalDespesas().compareTo(anterior.totalDespesas().multiply(FATOR_ALERTA)) > 0) {
                String categoria = categoriaComMaiorAumento(periodo, anterior);
                alertas.add(categoria == null
                        ? "Suas despesas aumentaram mais de 10% em relação ao período anterior."
                        : "Suas despesas com " + categoria + " aumentaram em relação ao período anterior.");
            }
            if (anterior.faturamento().signum() > 0
                    && periodo.faturamento().compareTo(anterior.faturamento().multiply(new BigDecimal("0.90"))) < 0) {
                alertas.add("Seu faturamento caiu mais de 10% em relação ao período anterior.");
            } else if (periodo.faturamento().compareTo(anterior.faturamento()) > 0) {
                recomendacoes.add("Seu faturamento foi maior neste período que no período anterior.");
            }
        }

        Map<String, BigDecimal> categorias = agruparCategorias(periodo.despesas());
        BigDecimal somaCategorias = periodo.totalDespesas();
        if (somaCategorias.signum() > 0 && !categorias.isEmpty()) {
            Map.Entry<String, BigDecimal> principal = categorias.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElseThrow();
            BigDecimal participacao = principal.getValue().multiply(new BigDecimal("100"))
                    .divide(somaCategorias, 1, RoundingMode.HALF_UP);
            if (participacao.compareTo(new BigDecimal("50")) >= 0) {
                recomendacoes.add("A maior parte das suas despesas está concentrada em " + principal.getKey()
                        + " (" + participacao.toPlainString() + "% do total).");
            }
        }
        if (periodo.vendas().isEmpty() && periodo.despesas().isEmpty()) {
            recomendacoes.add("Ainda não há lançamentos neste período para gerar análises.");
        }

        return new Analise(periodo.estabelecimento().getNome(), inicio, fim,
                periodo.faturamento(), periodo.totalDespesas(), periodo.resultado(), categorias,
                periodo.vendas().size(), periodo.despesas().size(), diaExtremo(periodo.vendas(), true),
                diaExtremo(periodo.vendas(), false), alertas, recomendacoes,
                anterior.faturamento(), anterior.totalDespesas(),
                !anterior.vendas().isEmpty() || !anterior.despesas().isEmpty());
    }

    public byte[] gerarPdf(Long proprietarioId, Long estabelecimentoId, LocalDate inicio, LocalDate fim,
            boolean completo) {
        Analise analise = analisar(proprietarioId, estabelecimentoId, inicio, fim);
        Dados periodo = carregar(proprietarioId, estabelecimentoId, inicio, fim);
        return new RelatorioPdfRenderer().render(analise, periodo.vendas(), periodo.despesas(), completo);
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
        BigDecimal faturamento = vendas.stream().map(Venda::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDespesas = despesas.stream().map(Despesa::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Dados(estabelecimento, vendas, despesas, faturamento, totalDespesas, faturamento.subtract(totalDespesas));
    }

    private Map<String, BigDecimal> agruparCategorias(List<Despesa> despesas) {
        return despesas.stream().collect(Collectors.groupingBy(Despesa::getCategoria, LinkedHashMap::new,
                Collectors.mapping(Despesa::getValor, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
    }

    private String categoriaComMaiorAumento(Dados atual, Dados anterior) {
        Map<String, BigDecimal> a = agruparCategorias(atual.despesas());
        Map<String, BigDecimal> b = agruparCategorias(anterior.despesas());
        return a.entrySet().stream().filter(e -> b.getOrDefault(e.getKey(), BigDecimal.ZERO).signum() > 0)
                .filter(e -> e.getValue().compareTo(b.get(e.getKey()).multiply(FATOR_ALERTA)) > 0)
                .max(Comparator.comparing(Map.Entry::getValue)).map(Map.Entry::getKey).orElse(null);
    }

    private DiaFaturamento diaExtremo(List<Venda> vendas, boolean maior) {
        if (vendas.stream().map(v -> v.getDataHora().toLocalDate()).distinct().count() < 2) {
            return null;
        }
        Map<LocalDate, BigDecimal> porDia = vendas.stream().collect(Collectors.groupingBy(v -> v.getDataHora().toLocalDate(),
                Collectors.mapping(Venda::getTotal, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));
        Comparator<Map.Entry<LocalDate, BigDecimal>> comparator = Map.Entry.comparingByValue();
        return porDia.entrySet().stream().sorted(maior ? comparator.reversed() : comparator).findFirst()
                .map(entry -> new DiaFaturamento(entry.getKey().getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("pt-BR")),
                        entry.getKey(), entry.getValue())).orElse(null);
    }

    private record Dados(Estabelecimento estabelecimento, List<Venda> vendas, List<Despesa> despesas,
            BigDecimal faturamento, BigDecimal totalDespesas, BigDecimal resultado) {}

    public record DiaFaturamento(String dia, LocalDate data, BigDecimal valor) {}
    public record Analise(String estabelecimento, LocalDate inicio, LocalDate fim, BigDecimal faturamento,
            BigDecimal despesas, BigDecimal resultado, Map<String, BigDecimal> categorias,
            int quantidadeVendas, int quantidadeDespesas, DiaFaturamento diaMaiorFaturamento,
            DiaFaturamento diaMenorFaturamento, List<String> alertas, List<String> recomendacoes,
            BigDecimal faturamentoAnterior, BigDecimal despesasAnteriores, boolean temComparacao) {}
}
