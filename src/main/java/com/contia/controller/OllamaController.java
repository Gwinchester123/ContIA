
package com.contia.controller;

import com.contia.service.OllamaService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ia")
@CrossOrigin(origins = "http://localhost:5173")
public class OllamaController {

  private final OllamaService ollamaService;

  public OllamaController(OllamaService ollamaService) {
    this.ollamaService = ollamaService;
  }

  @PostMapping("/conversar")
  public Map<String, String> conversar(@RequestBody Map<String, String> requisicao) {

    String mensagem = requisicao.get("mensagem");

    if (mensagem == null || mensagem.isBlank()) {
      throw new IllegalArgumentException("A mensagem não pode estar vazia.");
    }

    String resposta = ollamaService.conversar(mensagem);

    return Map.of("resposta", resposta);
  }
}
