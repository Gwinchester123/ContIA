package com.contia.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> tratarErroDeValidacao(IllegalArgumentException erro) {
        return ResponseEntity.badRequest().body(erro.getMessage());
    }
}
