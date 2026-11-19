package com.contia.controller;

import com.contia.model.Proprietario;
import com.contia.service.ProprietarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proprietarios")
@CrossOrigin(origins = "http://localhost:5173")
public class ProprietarioController {

    private final ProprietarioService proprietarioService;

    public ProprietarioController(ProprietarioService proprietarioService) {
        this.proprietarioService = proprietarioService;
    }

    @PostMapping
    public ResponseEntity<ProprietarioResposta> cadastrar(
            @RequestBody Proprietario proprietario) {

        Proprietario salvo = proprietarioService.cadastrar(proprietario);

        ProprietarioResposta resposta = new ProprietarioResposta(
                salvo.getId(),
                salvo.getNome(),
                salvo.getEmail(),
                salvo.getTelefone()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    public record ProprietarioResposta(
            Long id,
            String nome,
            String email,
            String telefone
    ) {
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErro(IllegalArgumentException erro) {
        return ResponseEntity.badRequest().body(erro.getMessage());
    }

    @PostMapping("/login")
public ResponseEntity<?> login(@RequestBody LoginRequest dados) {

    Proprietario proprietario = proprietarioService.autenticar(
            dados.email(),
            dados.senha()
    );

    ProprietarioResposta resposta = new ProprietarioResposta(
            proprietario.getId(),
            proprietario.getNome(),
            proprietario.getEmail(),
            proprietario.getTelefone()
    );

    return ResponseEntity.ok(resposta);
}

public record LoginRequest(String email, String senha) {
}
}