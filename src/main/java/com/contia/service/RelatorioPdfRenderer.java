package com.contia.service;

import com.contia.model.Despesa;
import com.contia.model.Venda;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
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

/** Renders the single Premium financial report using the existing analysis data. */
final class RelatorioPdfRenderer {
    private static final java.awt.Color GREEN = new java.awt.Color(24, 60, 50);
    private static final java.awt.Color GREEN_PALE = new java.awt.Color(235, 245, 238);
    private static final java.awt.Color AMOUNT_IN = new java.awt.Color(45, 112, 75);
    private static final java.awt.Color AMOUNT_OUT = new java.awt.Color(131, 72, 61);
    private static final java.awt.Color RED_PALE = new java.awt.Color(250, 239, 237);
    private static final java.awt.Color TEXT = new java.awt.Color(39, 54, 47);
    private static final java.awt.Color MUTED = new java.awt.Color(111, 124, 115);
    private static final java.awt.Color BORDER = new java.awt.Color(225, 233, 227);
    private static final java.awt.Color PALE = new java.awt.Color(248, 250, 248);
    private static final Set<String> CUSTOS_INSUMOS = Set.of("ingredientes", "materiais", "insumos", "embalagens");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", PT_BR);
    private static final String OBSERVACAO = "Valor calculado com base nas vendas e despesas registradas no sistema. Não representa necessariamente o lucro líquido contábil.";

    byte[] render(RelatorioFinanceiroService.Analise analise, List<Venda> vendas, List<Despesa> despesas) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 38, 38, 34, 48);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new Rodape());
            document.open();

            adicionarCabecalho(document, analise);
            adicionarResumo(document, analise);
            adicionarCategorias(document, analise.categorias());
            adicionarMovimentacoes(document, vendas, despesas);

            document.close();
            return output.toByteArray();
        } catch (Exception erro) {
            throw new IllegalStateException("Não foi possível gerar o PDF.", erro);
        }
    }

    private void adicionarCabecalho(Document document, RelatorioFinanceiroService.Analise analise) throws Exception {
        PdfPTable banner = new PdfPTable(new float[] { 27, 73 });
        banner.setWidthPercentage(100);
        banner.setSpacingAfter(10);

        PdfPCell marca = new PdfPCell();
        marca.setBackgroundColor(java.awt.Color.WHITE);
        marca.setBorderColor(BORDER);
        marca.setBorderWidth(.7f);
        marca.setPadding(7);
        try (InputStream input = RelatorioPdfRenderer.class.getResourceAsStream("/contia-assets/logo-contia.png")) {
            if (input == null) {
                marca.addElement(new Paragraph("ContIA", fonte(17, Font.BOLD, GREEN)));
            } else {
                Image logo = Image.getInstance(imagemLogoClara(input.readAllBytes()));
                logo.scaleToFit(118, 40);
                marca.addElement(logo);
            }
        }
        banner.addCell(marca);

        PdfPCell titulo = new PdfPCell();
        titulo.setBackgroundColor(GREEN);
        titulo.setBorder(Rectangle.NO_BORDER);
        titulo.setPadding(13);
        titulo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        titulo.addElement(new Paragraph("RELATÓRIO FINANCEIRO", fonte(17, Font.BOLD, java.awt.Color.WHITE)));
        banner.addCell(titulo);
        document.add(banner);

        PdfPTable contexto = new PdfPTable(new float[] { 52, 24, 24 });
        contexto.setWidthPercentage(100);
        contexto.setSpacingAfter(5);
        contexto.addCell(meta("ESTABELECIMENTO", analise.estabelecimento()));
        contexto.addCell(meta("PERÍODO", DATA.format(analise.inicio()) + " a " + DATA.format(analise.fim())));
        contexto.addCell(meta("GERADO EM", DATA_HORA.format(LocalDateTime.now())));
        document.add(contexto);
    }

    /** Places the existing transparent logo on a rounded white tile for contrast on the green banner. */
    private byte[] imagemLogoClara(byte[] arquivoOriginal) throws Exception {
        BufferedImage origem = ImageIO.read(new ByteArrayInputStream(arquivoOriginal));
        int larguraCartao = 520;
        int alturaCartao = 170;
        int margem = 28;
        BufferedImage cartao = new BufferedImage(larguraCartao, alturaCartao, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = cartao.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics.setColor(java.awt.Color.WHITE);
        graphics.fillRoundRect(0, 0, larguraCartao, alturaCartao, 28, 28);
        double escala = Math.min((double) (larguraCartao - 2 * margem) / origem.getWidth(),
                (double) (alturaCartao - 2 * margem) / origem.getHeight());
        int larguraLogo = (int) (origem.getWidth() * escala);
        int alturaLogo = (int) (origem.getHeight() * escala);
        graphics.drawImage(origem, (larguraCartao - larguraLogo) / 2, (alturaCartao - alturaLogo) / 2,
                larguraLogo, alturaLogo, null);
        graphics.dispose();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(cartao, "png", output);
        return output.toByteArray();
    }

    private PdfPCell meta(String rotulo, String valor) {
        PdfPCell cell = new PdfPCell();
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(.6f);
        cell.setPadding(8);
        cell.addElement(new Paragraph(rotulo, fonte(7, Font.BOLD, MUTED)));
        cell.addElement(new Paragraph(valor == null ? "—" : valor, fonte(9, Font.BOLD, TEXT)));
        return cell;
    }

    private void adicionarResumo(Document document, RelatorioFinanceiroService.Analise analise) throws DocumentException {
        adicionarTitulo(document, "Resumo financeiro");
        PdfPTable resumo = new PdfPTable(3);
        resumo.setWidthPercentage(100);
        resumo.setWidths(new float[] { 1, 1, 1 });
        resumo.setSpacingAfter(3);
        resumo.addCell(kpi("TOTAL DE VENDAS", moeda(analise.faturamento()), GREEN_PALE, GREEN));
        resumo.addCell(kpi("TOTAL DE DESPESAS", moeda(analise.despesas()), PALE, TEXT));
        resumo.addCell(kpi("LUCRO ESTIMADO", moeda(analise.resultado()),
                analise.resultado().signum() < 0 ? RED_PALE : GREEN_PALE,
                analise.resultado().signum() < 0 ? AMOUNT_OUT : GREEN));
        document.add(resumo);
        document.add(paragrafo(OBSERVACAO, fonte(8, Font.ITALIC, MUTED)));
    }

    private void adicionarCategorias(Document document, Map<String, BigDecimal> categorias) throws DocumentException {
        adicionarTitulo(document, "Despesas por categoria");
        PdfPTable tabela = new PdfPTable(new float[] { 46, 30, 24 });
        tabela.setWidthPercentage(98.5f);
        tabela.setHeaderRows(1);
        tabela.setSplitRows(false);
        tabela.setSpacingBefore(8);
        tabela.setSpacingAfter(12);
        tabela.addCell(cabecalho("CATEGORIA"));
        tabela.addCell(cabecalho("CLASSIFICAÇÃO"));
        tabela.addCell(cabecalho("VALOR"));
        if (categorias == null || categorias.isEmpty()) {
            PdfPCell vazio = celula("Nenhuma despesa registrada no período.", fonte(9, Font.NORMAL, MUTED), java.awt.Color.WHITE, Element.ALIGN_LEFT);
            vazio.setColspan(3);
            tabela.addCell(vazio);
        } else {
            int linha = 0;
            for (Map.Entry<String, BigDecimal> item : categorias.entrySet()) {
                java.awt.Color fundo = linha++ % 2 == 0 ? java.awt.Color.WHITE : PALE;
                tabela.addCell(celula(item.getKey(), fonte(9.5f, Font.NORMAL, TEXT), fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(custoInsumo(item.getKey()) ? "Custo com insumos" : "Despesa operacional",
                        fonte(9.5f, Font.NORMAL, TEXT), fundo, Element.ALIGN_LEFT));
                tabela.addCell(celula(moeda(item.getValue()), fonte(9.5f, Font.BOLD, TEXT), fundo, Element.ALIGN_RIGHT));
            }
        }
        document.add(tabela);
    }

    private void adicionarMovimentacoes(Document document, List<Venda> vendas, List<Despesa> despesas) throws DocumentException {
        adicionarTitulo(document, "Movimentações detalhadas");
        PdfPTable tabela = new PdfPTable(new float[] { 20, 14, 46, 20 });
        tabela.setWidthPercentage(98.5f);
        tabela.setHeaderRows(1);
        tabela.setSplitRows(false);
        tabela.setSplitLate(true);
        tabela.setSpacingBefore(8);
        tabela.setSpacingAfter(10);
        tabela.addCell(cabecalho("DATA"));
        tabela.addCell(cabecalho("TIPO"));
        tabela.addCell(cabecalho("DESCRIÇÃO"));
        tabela.addCell(cabecalho("VALOR"));

        List<Movimento> movimentos = new ArrayList<>();
        for (Venda venda : vendas) {
            if (venda.getItens() == null || venda.getItens().isEmpty()) {
                String descricao = venda.getQuantidade() + " " + venda.getProduto();
                movimentos.add(new Movimento(venda.getDataHora(), descricao, "Venda", venda.getTotal(), true));
            } else {
                venda.getItens().forEach(item -> movimentos.add(new Movimento(
                        venda.getDataHora(),
                        item.getQuantidade() + " " + item.getProduto(),
                        "Venda",
                        item.getSubtotal(),
                        true)));
            }
        }
        for (Despesa despesa : despesas) {
            movimentos.add(new Movimento(despesa.getDataHora(), despesa.getDescricao(), "Despesa", despesa.getValor(), false));
        }
        movimentos.sort(Comparator.comparing(Movimento::dataHora));

        if (movimentos.isEmpty()) {
            PdfPCell vazio = celula("Nenhuma movimentação registrada neste período.", fonte(9, Font.NORMAL, MUTED), java.awt.Color.WHITE, Element.ALIGN_LEFT);
            vazio.setColspan(4);
            tabela.addCell(vazio);
        } else {
            int linha = 0;
            for (Movimento movimento : movimentos) {
                java.awt.Color fundo = linha++ % 2 == 0 ? java.awt.Color.WHITE : PALE;
                PdfPCell data = celula(DATA_HORA.format(movimento.dataHora()), fonte(8, Font.NORMAL, MUTED), fundo, Element.ALIGN_LEFT);
                PdfPCell tipo = celula(movimento.tipo(), fonte(8.5f, Font.NORMAL, TEXT), fundo, Element.ALIGN_LEFT);
                PdfPCell descricao = celula(movimento.descricao(), fonte(9, Font.NORMAL, TEXT), fundo, Element.ALIGN_LEFT);
                PdfPCell valor = celula((movimento.entrada() ? "+ " : "− ") + moeda(movimento.valor()),
                        fonte(9, Font.BOLD, movimento.entrada() ? AMOUNT_IN : AMOUNT_OUT), fundo, Element.ALIGN_RIGHT);
                data.setNoWrap(true);
                valor.setNoWrap(true);
                tabela.addCell(data);
                tabela.addCell(tipo);
                tabela.addCell(descricao);
                tabela.addCell(valor);
            }
        }
        document.add(tabela);
    }

    private void adicionarTitulo(Document document, String texto) throws DocumentException {
        Paragraph titulo = new Paragraph(texto, fonte(11, Font.BOLD, GREEN));
        titulo.setKeepTogether(true);
        titulo.setSpacingBefore(12);
        titulo.setSpacingAfter(8);
        document.add(titulo);
    }

    private PdfPCell kpi(String rotulo, String valor, java.awt.Color fundo, java.awt.Color destaque) {
        Paragraph texto = new Paragraph();
        texto.add(new Phrase(rotulo + "\n", fonte(7, Font.BOLD, MUTED)));
        texto.add(new Phrase(valor, fonte(13, Font.BOLD, destaque)));
        PdfPCell cell = new PdfPCell(texto);
        cell.setBackgroundColor(fundo);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(.6f);
        cell.setPadding(12);
        return cell;
    }

    private PdfPCell cabecalho(String texto) {
        PdfPCell cell = celula(texto, fonte(8, Font.BOLD, java.awt.Color.WHITE), GREEN, Element.ALIGN_LEFT);
        cell.setPadding(8);
        return cell;
    }

    private PdfPCell celula(String texto, Font font, java.awt.Color fundo, int alinhamento) {
        PdfPCell cell = new PdfPCell(new Phrase(texto == null ? "" : texto, font));
        cell.setBackgroundColor(fundo);
        cell.setHorizontalAlignment(alinhamento);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setBorderColor(BORDER);
        cell.setBorderWidth(.5f);
        cell.setPadding(6);
        return cell;
    }

    private Paragraph paragrafo(String texto, Font font) {
        Paragraph paragraph = new Paragraph(texto, font);
        paragraph.setSpacingAfter(4);
        return paragraph;
    }

    private Font fonte(float tamanho, int estilo, java.awt.Color cor) {
        return new Font(Font.HELVETICA, tamanho, estilo, cor);
    }

    private String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(PT_BR).format(valor == null ? BigDecimal.ZERO : valor);
    }

    private boolean custoInsumo(String categoria) {
        return CUSTOS_INSUMOS.contains(categoria == null ? "" : categoria.trim().toLowerCase(PT_BR));
    }

    private record Movimento(LocalDateTime dataHora, String descricao, String tipo, BigDecimal valor, boolean entrada) {}

    private static final class Rodape extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            var canvas = writer.getDirectContent();
            canvas.setColorStroke(BORDER);
            canvas.moveTo(document.left(), 34);
            canvas.lineTo(document.right(), 34);
            canvas.stroke();
            Font fonteRodape = new Font(Font.HELVETICA, 8, Font.NORMAL, MUTED);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Página " + writer.getPageNumber(), fonteRodape), document.right(), 20, 0);
        }
    }
}
