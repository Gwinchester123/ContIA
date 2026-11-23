package com.contia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErroDeValidacao(IllegalArgumentException erro) {
        return ResponseEntity.badRequest().body(erro.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> tratarAcessoNegado(AccessDeniedException erro) {
        return ResponseEntity.status(403).body(erro.getMessage());
    }
}
