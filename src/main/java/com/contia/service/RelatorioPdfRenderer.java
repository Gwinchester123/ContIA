package com.contia.service;

import com.contia.model.Despesa;
import com.contia.model.Venda;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Renders the essential and complete financial statements from database records. */
final class RelatorioPdfRenderer {
    private static final Color GREEN = new Color(24, 60, 50);
    private static final Color GREEN_LIGHT = new Color(235, 244, 237);
    private static final Color TEXT = new Color(44, 58, 49);
    private static final Color MUTED = new Color(111, 124, 115);
    private static final Color BORDER = new Color(225, 233, 227);
    private static final Color ROW_ALT = new Color(248, 250, 248);
    private static final Color AMOUNT_IN = new Color(45, 112, 75);
    private static final Color AMOUNT_OUT = new Color(131, 72, 61);
    private static final Set<String> CUSTOS_DIRETOS = Set.of("ingredientes", "materiais", "insumos", "embalagens");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR);

    byte[] render(RelatorioFinanceiroService.Analise analise, List<Venda> vendas,
            List<Despesa> despesas, boolean completo) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 40, 52);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new Rodape());
            document.open();

            adicionarCabecalho(document, completo);
            document.add(paragrafo(analise.estabelecimento(), fonte(18, Font.BOLD, GREEN)));
            document.add(paragrafo("Período de " + DATA.format(analise.inicio()) + " a " + DATA.format(analise.fim()),
                    fonte(10, Font.NORMAL, MUTED)));
            document.add(paragrafo("Gerado em " + DATA_HORA.format(LocalDateTime.now()), fonte(9, Font.NORMAL, MUTED)));
            document.add(new Paragraph(" ", fonte(5, Font.NORMAL, TEXT)));

            adicionarResumo(document, analise);
            adicionarCategorias(document, analise.categorias());
            if (completo) {
                adicionarMovimentacoes(document, vendas, despesas);
                adicionarAnalises(document, analise);
            } else {
                document.add(paragrafo("Este resumo reúne " + analise.quantidadeVendas() + " venda(s) e "
                        + analise.quantidadeDespesas() + " despesa(s) registradas no período.",
                        fonte(9, Font.NORMAL, MUTED)));
            }
            document.close();
            return output.toByteArray();
        } catch (Exception erro) {
            throw new IllegalStateException("Não foi possível gerar o PDF.", erro);
        }
    }

    private void adicionarCabecalho(Document document, boolean completo) throws DocumentException {
        PdfPTable banner = new PdfPTable(new float[] { 60, 40 });
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(22);
        PdfPCell marca = celula("ContIA", fonte(22, Font.BOLD, Color.WHITE), GREEN, Element.ALIGN_LEFT);
        marca.setPadding(17);
        PdfPCell tipo = celula(completo ? "RELATÓRIO COMPLETO\nANÁLISE DETALHADA" : "RELATÓRIO ESSENCIAL\nRESUMO FINANCEIRO",
                fonte(9, Font.BOLD, Color.WHITE), GREEN, Element.ALIGN_RIGHT);
        tipo.setPadding(17);
        banner.addCell(marca);
        banner.addCell(tipo);
        document.add(banner);
    }

    private void adicionarResumo(Document document, RelatorioFinanceiroService.Analise analise) throws DocumentException {
        adicionarTituloSecao(document, "Resumo financeiro");
        PdfPTable resumo = new PdfPTable(3);
        resumo.setWidthPercentage(100);
        resumo.setWidths(new float[] { 1, 1, 1 });
        resumo.setSpacingAfter(17);
        resumo.addCell(kpi("VENDAS", moeda(analise.faturamento()), GREEN_LIGHT, AMOUNT_IN));
        resumo.addCell(kpi("DESPESAS", moeda(analise.despesas()), ROW_ALT, TEXT));
        resumo.addCell(kpi("RESULTADO FINANCEIRO", moeda(analise.resultado()), GREEN_LIGHT,
                analise.resultado().signum() < 0 ? AMOUNT_OUT : GREEN));
        document.add(resumo);
        document.add(paragrafo("Resultado financeiro = total de vendas menos total de despesas. Não corresponde a lucro líquido contábil.",
                fonte(8, Font.NORMAL, MUTED)));
        document.add(paragrafo("Registros: " + analise.quantidadeVendas() + " venda(s)  ·  "
                + analise.quantidadeDespesas() + " despesa(s)", fonte(9, Font.NORMAL, MUTED)));
    }

    private void adicionarCategorias(Document document, Map<String, BigDecimal> categorias) throws DocumentException {
        adicionarTituloSecao(document, "Despesas por categoria");
        PdfPTable tabela = new PdfPTable(new float[] { 46, 30, 24 });
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        tabela.setSpacingAfter(17);
        tabela.addCell(cabecalhoTabela("CATEGORIA"));
        tabela.addCell(cabecalhoTabela("CLASSIFICAÇÃO"));
        tabela.addCell(cabecalhoTabela("VALOR"));
        if (categorias.isEmpty()) {
            PdfPCell vazio = celula("Nenhuma despesa registrada neste período", fonte(9, Font.NORMAL, MUTED), Color.WHITE, Element.ALIGN_LEFT);
            vazio.setColspan(3);
            tabela.addCell(vazio);
        } else {
            int linha = 0;
            for (Map.Entry<String, BigDecimal> item : categorias.entrySet()) {
                Color fundo = linha++ % 2 == 0 ? Color.WHITE : ROW_ALT;
                tabela.addCell(celula(item.getKey(), fonte(9, Font.NORMAL, TEXT), fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(custoDireto(item.getKey()) ? "Custo direto" : "Operacional",
                        fonte(8, Font.NORMAL, MUTED), fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(moeda(item.getValue()), fonte(9, Font.BOLD, TEXT), fundo, Element.ALIGN_RIGHT));
            }
        }
        document.add(tabela);
    }

    private void adicionarMovimentacoes(Document document, List<Venda> vendas, List<Despesa> despesas)
            throws DocumentException {
        adicionarTituloSecao(document, "Movimentações detalhadas");
        PdfPTable tabela = new PdfPTable(new float[] { 20, 13, 31, 17, 19 });
        tabela.setWidthPercentage(100);
        tabela.setHeaderRows(1);
        tabela.setSplitRows(true);
        tabela.setSpacingAfter(16);
        tabela.addCell(cabecalhoTabela("DATA"));
        tabela.addCell(cabecalhoTabela("TIPO"));
        tabela.addCell(cabecalhoTabela("DESCRIÇÃO"));
        tabela.addCell(cabecalhoTabela("CATEGORIA"));
        tabela.addCell(cabecalhoTabela("VALOR"));

        List<Movimento> movimentos = new ArrayList<>();
        vendas.forEach(venda -> movimentos.add(new Movimento(venda.getDataHora(), "Venda",
                venda.getProduto() + " · " + venda.getQuantidade() + " un.", "Vendas", venda.getTotal(), true)));
        despesas.forEach(despesa -> movimentos.add(new Movimento(despesa.getDataHora(), "Despesa",
                despesa.getDescricao(), despesa.getCategoria(), despesa.getValor(), false)));
        movimentos.sort(Comparator.comparing(Movimento::dataHora));

        if (movimentos.isEmpty()) {
            PdfPCell vazio = celula("Nenhuma movimentação registrada neste período", fonte(9, Font.NORMAL, MUTED), Color.WHITE, Element.ALIGN_LEFT);
            vazio.setColspan(5);
            tabela.addCell(vazio);
        } else {
            int linha = 0;
            for (Movimento movimento : movimentos) {
                Color fundo = linha++ % 2 == 0 ? Color.WHITE : ROW_ALT;
                tabela.addCell(celula(DATA_HORA.format(movimento.dataHora()), fonte(7, Font.NORMAL, MUTED), fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(movimento.tipo(), fonte(8, Font.NORMAL, TEXT), fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(movimento.descricao(), fonte(8, Font.NORMAL, TEXT), fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(movimento.categoria(), fonte(7, Font.NORMAL, MUTED), fundo, Element.ALIGN_LEFT));
                String valor = (movimento.entrada() ? "+ " : "− ") + moeda(movimento.valor());
                tabela.addCell(celula(valor, fonte(8, Font.BOLD, movimento.entrada() ? AMOUNT_IN : AMOUNT_OUT), fundo, Element.ALIGN_RIGHT));
            }
        }
        document.add(tabela);
    }

    private void adicionarAnalises(Document document, RelatorioFinanceiroService.Analise analise) throws DocumentException {
        adicionarTituloSecao(document, "Comparação e observações");
        if (analise.temComparacao()) {
            PdfPTable comparacao = new PdfPTable(2);
            comparacao.setWidthPercentage(100);
            comparacao.setSpacingAfter(10);
            comparacao.addCell(kpi("VENDAS · PERÍODO ANTERIOR", moeda(analise.faturamentoAnterior()), ROW_ALT, TEXT));
            comparacao.addCell(kpi("DESPESAS · PERÍODO ANTERIOR", moeda(analise.despesasAnteriores()), ROW_ALT, TEXT));
            document.add(comparacao);
        } else {
            document.add(paragrafo("Sem dados anteriores suficientes para comparação.", fonte(9, Font.NORMAL, MUTED)));
        }
        for (String alerta : analise.alertas()) adicionarNota(document, "ALERTA", alerta, new Color(255, 249, 235), new Color(147, 99, 24));
        for (String recomendacao : analise.recomendacoes()) adicionarNota(document, "LEITURA", recomendacao, GREEN_LIGHT, GREEN);
        if (analise.diaMaiorFaturamento() != null && analise.diaMenorFaturamento() != null) {
            document.add(paragrafo("Maior faturamento diário: " + analise.diaMaiorFaturamento().dia() + " · "
                    + moeda(analise.diaMaiorFaturamento().valor()), fonte(9, Font.NORMAL, TEXT)));
            document.add(paragrafo("Menor faturamento diário registrado: " + analise.diaMenorFaturamento().dia() + " · "
                    + moeda(analise.diaMenorFaturamento().valor()), fonte(9, Font.NORMAL, TEXT)));
        }
    }

    private void adicionarNota(Document document, String tipo, String texto, Color fundo, Color destaque)
            throws DocumentException {
        PdfPTable nota = new PdfPTable(1);
        nota.setWidthPercentage(100);
        nota.setSpacingBefore(5);
        nota.setSpacingAfter(5);
        PdfPCell conteudo = celula(tipo + "  ·  " + texto, fonte(9, Font.NORMAL, destaque), fundo, Element.ALIGN_LEFT);
        conteudo.setPadding(10);
        nota.addCell(conteudo);
        document.add(nota);
    }

    private void adicionarTituloSecao(Document document, String titulo) throws DocumentException {
        Paragraph cabecalho = new Paragraph(titulo, fonte(12, Font.BOLD, GREEN));
        cabecalho.setSpacingBefore(12);
        cabecalho.setSpacingAfter(8);
        document.add(cabecalho);
    }

    private PdfPCell kpi(String rotulo, String valor, Color fundo, Color corValor) {
        Paragraph texto = new Paragraph();
        texto.add(new Phrase(rotulo + "\n", fonte(7, Font.BOLD, MUTED)));
        texto.add(new Phrase(valor, fonte(13, Font.BOLD, corValor)));
        PdfPCell cell = new PdfPCell(texto);
        cell.setBackgroundColor(fundo);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(12);
        return cell;
    }

    private PdfPCell cabecalhoTabela(String texto) {
        PdfPCell cell = celula(texto, fonte(7, Font.BOLD, Color.WHITE), GREEN, Element.ALIGN_LEFT);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell celula(String texto, Font font, Color fundo, int alinhamento) {
        PdfPCell cell = new PdfPCell(new Phrase(texto == null ? "" : texto, font));
        cell.setBackgroundColor(fundo);
        cell.setHorizontalAlignment(alinhamento);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(0.5f);
        cell.setPadding(7);
        return cell;
    }

    private Paragraph paragrafo(String texto, Font font) {
        Paragraph paragraph = new Paragraph(texto, font);
        paragraph.setSpacingAfter(4);
        return paragraph;
    }

    private Font fonte(float tamanho, int estilo, Color cor) {
        return new Font(Font.HELVETICA, tamanho, estilo, cor);
    }

    private String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor);
    }

    private boolean custoDireto(String categoria) {
        return CUSTOS_DIRETOS.contains(categoria == null ? "" : categoria.trim().toLowerCase(PT_BR));
    }

    private record Movimento(LocalDateTime dataHora, String tipo, String descricao,
            String categoria, BigDecimal valor, boolean entrada) {}

    private static final class Rodape extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            var canvas = writer.getDirectContent();
            canvas.setColorStroke(BORDER);
            canvas.moveTo(document.left(), 34);
            canvas.lineTo(document.right(), 34);
            canvas.stroke();
            Font rodape = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase("ContIA · Relatório do estabelecimento selecionado", rodape), document.left(), 22, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), rodape), document.right(), 22, 0);
        }
    }
}
