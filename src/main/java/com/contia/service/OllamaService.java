
package com.contia.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class OllamaService {

  @Value("${ollama.url}")
  private String ollamaUrl;

  @Value("${ollama.model}")
  private String ollamaModel;

  private final RestClient restClient = RestClient.create();

  private final ObjectMapper objectMapper = new ObjectMapper();

  public String conversar(String mensagem) {

    String instrucao = """
        Você é o assistente financeiro do ContIA.

        Identifique se a mensagem representa uma venda ou despesa.

        Responda exclusivamente com JSON válido.

        Para uma VENDA, utilize este formato:
        {
          "tipo": "VENDA",
          "produtos": [
            {
              "produto": "Nome do produto",
              "quantidade": 1,
              "valorUnitario": 10.00
            }
          ]
        }

        Para uma DESPESA, utilize este formato:
        {
          "tipo": "DESPESA",
          "descricao": "Descrição da despesa",
          "categoria": "Insumos",
          "valor": 50.00
        }

        Regras gerais:
        - Não invente informações.
        - Se faltar algum dado obrigatório, utilize null.
        - Na venda, retorne TODOS os produtos mencionados.
        - Cada produto deve ter seu próprio nome, quantidade e valorUnitario.
        - Não junte produtos diferentes em um único item.
        - Não some as quantidades de produtos diferentes.
        - Na despesa, descricao e valor são obrigatórios.
        - Utilize números nos campos de valores.
        - Não calcule valores que não foram informados.
        - Não registre informações no banco de dados.
        - Preserve os nomes dos produtos exatamente como foram escritos.
        - Não substitua palavras por outras parecidas.

        Regras para identificação de preços:
        - Quando a mensagem começar com "vendi", classifique como VENDA., mesmo que faltem informações.
        - Expressões como "algumas cocas", "alguns espetinhos", vendi coca" ou "vendi refrigerantes" indicam uma venda.
        - Se o comerciante não informar a quantidade, utilize null no campo quantidade.
        - Se o comerciante não informar o preço, utilize null no campo valorUnitario.
        - Nunca classifique uma venda como NAO_IDENTIFICADO apenas porque faltam quantidade ou preço.

        Regras de preços:
        - Quando o comerciante informar um único preço usando expressões
          como "cada um por 5 reais" ou "cada uma por 5 reais",
          aplique esse preço a todos os produtos mencionados,
          desde que o contexto deixe claro que vale para todos.
        - Se cada produto tiver um preço informado separadamente,
          respeite o preço individual.
        - Não invente preços quando eles não forem informados.

        Regras para respostas complementares:
        - A mensagem pode conter uma venda original seguida de uma resposta
          do comerciante para completar uma informação que faltava.
        - Analise toda a conversa, considerando a mensagem original.
        - Se um produto já possui preço informado, preserve esse preço.
        - Se existir somente um produto sem preço e o comerciante
          informar um preço na continuação, associe esse preço
          somente ao produto que estava sem preço.
        - Não aplique o preço complementar aos produtos que já
          possuem preço informado.
        - Se ainda faltar alguma informação, utilize null.
        - Não confunda uma resposta complementar com uma nova venda.
        - Se a mensagem original indicar uma venda e estiver faltando
          a quantidade de um produto, interprete uma resposta numérica
          curta, como "2" ou "3", como a quantidade desse produto.
        - Associe a quantidade informada ao produto que estava
          aguardando essa informação.
        - Preserve o nome do produto e qualquer preço já informado.
        - Depois de preencher a quantidade, se o preço ainda estiver
          faltando, mantenha valorUnitario como null.
        - Não classifique uma resposta numérica curta como
          NAO_IDENTIFICADO quando houver uma venda pendente.
        - Quando a mensagem original já identificar o produto e a
          quantidade, nunca substitua essas informações por exemplos
          do formato JSON.
        - Se a mensagem original contiver um produto com quantidade
          definida e preço null, preserve o produto e a quantidade.
        - Ao receber somente o preço na resposta complementar,
          preencha apenas o campo valorUnitario.
        - Nunca utilize expressões como "Nome do produto",
          "Nome do produto" ou outros textos de exemplo como dados reais.

        Mensagem do comerciante:
        """ + mensagem;

    Map<String, Object> requisicao = Map.of(
        "model", ollamaModel,
        "prompt", instrucao,
        "stream", false);

    Map<?, ?> resposta = restClient.post()
        .uri(ollamaUrl + "/api/generate")
        .body(requisicao)
        .retrieve()
        .body(Map.class);

    String respostaIA = (String) resposta.get("response");

    try {
      JsonNode json = objectMapper.readTree(respostaIA);

      if ("VENDA".equals(json.path("tipo").asText())) {

        JsonNode produtos = json.path("produtos");

        List<String> nomesOriginais = extrairProdutosOriginais(mensagem);

        if (produtos.isArray()
            && produtos.size() == nomesOriginais.size()) {

          for (int i = 0; i < produtos.size(); i++) {

            if (produtos.get(i) instanceof ObjectNode produto) {
              produto.put("produto", nomesOriginais.get(i));
            }
          }
        }
      }

      return objectMapper.writeValueAsString(json);

    } catch (Exception e) {
      throw new IllegalStateException(
          "Não foi possível interpretar o JSON retornado pela IA.", e);
    }
  }

  private List<String> extrairProdutosOriginais(String mensagem) {

    List<String> produtos = new ArrayList<>();

    Pattern padrao = Pattern.compile(
        "(?i)(?:\\bvendi\\s+|\\be\\s+)\\d+\\s+(.+?)" +
            "(?=\\s+por\\s+\\d+(?:[,.]\\d{1,2})?" +
            "|,?\\s*cada\\s+(?:um|uma)\\b" +
            "|\\s+e\\s+\\d+\\s+|[.!?]|$)");

    Matcher matcher = padrao.matcher(mensagem.trim());

    while (matcher.find()) {
      produtos.add(matcher.group(1).trim());
    }

    return produtos;
  }
}
