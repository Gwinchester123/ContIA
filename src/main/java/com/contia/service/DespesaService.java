
package com.contia.service;

import com.contia.model.Despesa;
import com.contia.model.Estabelecimento;
import com.contia.repository.DespesaRepository;
import com.contia.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DespesaService {

    private final DespesaRepository despesaRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public DespesaService(
            DespesaRepository despesaRepository,
            EstabelecimentoRepository estabelecimentoRepository) {
        this.despesaRepository = despesaRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    public Despesa registrar(
            Long estabelecimentoId,
            String descricao,
            String categoria,
            BigDecimal valor) {

        if (descricao == null || descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }

        if (categoria == null || categoria.trim().isEmpty()) {
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }

        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }

        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado."));

        Despesa despesa = new Despesa();
        despesa.setDescricao(descricao.trim());
        despesa.setCategoria(categoria.trim());
        despesa.setValor(valor);
        despesa.setDataHora(LocalDateTime.now());
        despesa.setEstabelecimento(estabelecimento);

        return despesaRepository.save(despesa);
    }

    public List<Despesa> listarPorEstabelecimento(Long estabelecimentoId) {
        return despesaRepository.findByEstabelecimentoId(estabelecimentoId);
    }
}

