
package com.contia.controller;

import com.contia.service.MensagemFinanceiraService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mensagens")
@CrossOrigin(origins = "http://localhost:5173")
public class MensagemFinanceiraController {

  private final MensagemFinanceiraService mensagemFinanceiraService;

  public MensagemFinanceiraController(
      MensagemFinanceiraService mensagemFinanceiraService) {
    this.mensagemFinanceiraService = mensagemFinanceiraService;
  }

  @PostMapping
  public Map<String, Object> processar(
      @RequestBody MensagemRequest request) {

    return mensagemFinanceiraService.processar(
        request.estabelecimentoId(),
        request.mensagem());
  }

  public record MensagemRequest(
      Long estabelecimentoId,
      String mensagem) {
  }
}
