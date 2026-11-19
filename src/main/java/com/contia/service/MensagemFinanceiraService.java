
package com.contia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MensagemFinanceiraService {

  private final OllamaService ollamaService;
  private final VendaService vendaService;
  private final DespesaService despesaService;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final Map<Long, String> mensagensPendentes = new ConcurrentHashMap<>();
  private final Map<Long, ArrayNode> produtosPendentes = new ConcurrentHashMap<>();

  private boolean temPrecoInformado(String mensagem) {

    String texto = mensagem.toLowerCase();

    return texto.matches("(?s).*\\bpor\\s+\\d+([,.]\\d{1,2})?.*")
        || texto.matches("(?s).*r\\$\\s*\\d+([,.]\\d{1,2})?.*")
        || texto.matches("(?s).*\\d+([,.]\\d{1,2})?\\s+reais?.*");
  }

  public MensagemFinanceiraService(
      OllamaService ollamaService,
      VendaService vendaService,
      DespesaService despesaService) {

    this.ollamaService = ollamaService;
    this.vendaService = vendaService;
    this.despesaService = despesaService;
  }

  public Map<String, Object> processar(
      Long estabelecimentoId,
      String mensagem) {

    ArrayNode produtosJaIdentificados = produtosPendentes.get(estabelecimentoId);

    String mensagemOriginal = mensagensPendentes.get(estabelecimentoId);

    String mensagemCompleta = mensagemOriginal == null
        ? mensagem
        : mensagemOriginal + " " + mensagem;

    String respostaIA = ollamaService.conversar(mensagemCompleta);

    System.out.println("RESPOSTA DA IA: " + respostaIA);
    System.out.println("PREÇO ENCONTRADO: " + temPrecoInformado(mensagem));

    try {
      JsonNode dados = objectMapper.readTree(respostaIA);

      String tipo = dados.path("tipo").asText("");

      if ("VENDA".equals(tipo)) {

        JsonNode produtos = dados.path("produtos");

        // Recupera os dados anteriores e combina com a nova resposta da IA.
        if (produtosJaIdentificados != null
            && produtos.isArray()
            && produtosJaIdentificados.size() == produtos.size()) {

          ArrayNode produtosAtualizados = objectMapper.createArrayNode();

          for (int i = 0; i < produtosJaIdentificados.size(); i++) {

            ObjectNode produtoAnterior = (ObjectNode) produtosJaIdentificados.get(i).deepCopy();

            JsonNode produtoNovo = produtos.get(i);

            JsonNode novaQuantidade = produtoNovo.get("quantidade");
            JsonNode novoPreco = produtoNovo.get("valorUnitario");

            // Atualiza a quantidade somente se a IA informar um valor válido.
            if (novaQuantidade != null
                && novaQuantidade.isIntegralNumber()
                && novaQuantidade.asInt() > 0) {
              produtoAnterior.set("quantidade", novaQuantidade.deepCopy());
            }

            // Atualiza o preço somente se a IA informar um valor válido.
            if (novoPreco != null
                && novoPreco.isNumber()
                && novoPreco.decimalValue().compareTo(BigDecimal.ZERO) > 0) {
              produtoAnterior.set("valorUnitario", novoPreco.deepCopy());
            }

            produtosAtualizados.add(produtoAnterior);
          }

          ((ObjectNode) dados).set("produtos", produtosAtualizados);
          produtos = dados.path("produtos");
        }

        if (!produtos.isArray() || produtos.isEmpty()) {
          mensagensPendentes.put(estabelecimentoId, mensagemCompleta);

          return resultado("PRECISA_DE_INFORMACAO",
              "Quais produtos foram vendidos e em quais quantidades?");
        }

        // Primeiro, verifica todos os produtos.
        // Nenhuma venda será registrada enquanto faltar informação.

        for (JsonNode item : produtos) {

          String produto = item.path("produto").asText(null);
          JsonNode quantidadeNode = item.get("quantidade");
          JsonNode valorNode = item.get("valorUnitario");

          if (!temPrecoInformado(mensagemCompleta)) {

            ((ObjectNode) item).putNull("valorUnitario");

            valorNode = item.get("valorUnitario");
          }

          System.out.println("PRODUTO FINAL: " + produto);
          System.out.println("QUANTIDADE FINAL: " + quantidadeNode);
          System.out.println("PREÇO FINAL: " + valorNode);
          System.out.println("PRODUTOS PENDENTES: " + produtosJaIdentificados);

          if (produto == null || produto.isBlank()) {

            produtosPendentes.put(
                estabelecimentoId,
                (ArrayNode) produtos.deepCopy());

            mensagensPendentes.put(estabelecimentoId, mensagemCompleta);

            return resultado("PRECISA_DE_INFORMACAO",
                "Não consegui identificar o nome de um dos produtos.");
          }

          if (quantidadeNode == null || quantidadeNode.isNull()
              || !quantidadeNode.isIntegralNumber()
              || quantidadeNode.asInt() <= 0) {

            produtosPendentes.put(
                estabelecimentoId,
                (ArrayNode) produtos.deepCopy());

            mensagensPendentes.put(estabelecimentoId, mensagemCompleta);

            return resultado("PRECISA_DE_INFORMACAO",
                "Qual foi a quantidade de " + produto + " que você vendeu?");
          }
          if (valorNode == null || valorNode.isNull()
              || !valorNode.isNumber()
              || valorNode.decimalValue().compareTo(BigDecimal.ZERO) <= 0) {

            produtosPendentes.put(
                estabelecimentoId,
                (ArrayNode) produtos.deepCopy());

            mensagensPendentes.put(estabelecimentoId, mensagemCompleta);

            return resultado("PRECISA_DE_INFORMACAO",
                "Por quanto você vendeu cada unidade de " + produto + "?");
          }
        }

        // Todas as informações estão completas.
        // Agora registra cada produto.

        for (JsonNode item : produtos) {

          String produto = item.path("produto").asText();
          Integer quantidade = item.path("quantidade").asInt();

          BigDecimal valorUnitario = item.path("valorUnitario").decimalValue();

          vendaService.registrar(
              estabelecimentoId,
              produto,
              quantidade,
              valorUnitario);
        }

        mensagensPendentes.remove(estabelecimentoId);

        produtosPendentes.remove(estabelecimentoId);

        return resultado("SUCESSO",
            produtos.size() + " produto(s) registrado(s) com sucesso.");
      }

      if ("DESPESA".equals(tipo)) {

        String descricao = dados.path("descricao").asText(null);
        String categoria = dados.path("categoria").asText(null);

        JsonNode valorNode = dados.get("valor");

        if (descricao == null || descricao.isBlank()
            || categoria == null || categoria.isBlank()
            || valorNode == null || valorNode.isNull()
            || !temPrecoInformado(mensagemCompleta)) {

          mensagensPendentes.put(estabelecimentoId, mensagemCompleta);

          return resultado(
              "PRECISA_DE_INFORMACAO",
              "Qual foi o valor da despesa?");
        }

        BigDecimal valor = valorNode.isNumber()
            ? valorNode.decimalValue()
            : null;

        if (valor == null) {
          return resultado(
              "PRECISA_DE_INFORMACAO",
              "O valor da despesa precisa ser informado corretamente.");
        }

        despesaService.registrar(
            estabelecimentoId,
            descricao,
            categoria,
            valor);

        mensagensPendentes.remove(estabelecimentoId);

        return resultado("SUCESSO", "Despesa registrada com sucesso.");
      }

      return resultado(
          "NAO_IDENTIFICADO",
          "Não consegui identificar se a mensagem é uma venda ou despesa.");

    } catch (Exception e) {
      throw new IllegalStateException(
          "Erro ao processar a mensagem financeira.", e);
    }
  }

  private Map<String, Object> resultado(String status, String mensagem) {

    Map<String, Object> resposta = new LinkedHashMap<>();
    resposta.put("status", status);
    resposta.put("mensagem", mensagem);

    return resposta;
  }
}
