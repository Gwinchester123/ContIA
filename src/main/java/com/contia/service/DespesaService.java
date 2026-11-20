
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

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");

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

        validarDados(descricao, categoria, valor);

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

    public Despesa atualizar(
            Long despesaId,
            Long estabelecimentoId,
            String descricao,
            String categoria,
            BigDecimal valor) {

        validarDados(descricao, categoria, valor);
        Despesa despesa = despesaRepository.findById(despesaId)
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada."));

        if (estabelecimentoId == null
                || !estabelecimentoId.equals(despesa.getEstabelecimento().getId())) {
            throw new IllegalArgumentException("Despesa não encontrada para este estabelecimento.");
        }

        despesa.setDescricao(descricao.trim());
        despesa.setCategoria(categoria.trim());
        despesa.setValor(valor);
        return despesaRepository.save(despesa);
    }

    public void excluir(Long despesaId, Long estabelecimentoId) {
        Despesa despesa = despesaRepository.findById(despesaId)
                .orElseThrow(() -> new IllegalArgumentException("Despesa não encontrada."));

        if (estabelecimentoId == null
                || !estabelecimentoId.equals(despesa.getEstabelecimento().getId())) {
            throw new IllegalArgumentException("Despesa não encontrada para este estabelecimento.");
        }

        despesaRepository.delete(despesa);
    }

    private void validarDados(String descricao, String categoria, BigDecimal valor) {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }
        if (descricao.trim().length() > 150) {
            throw new IllegalArgumentException("A descrição deve ter no máximo 150 caracteres.");
        }
        if (categoria == null || categoria.trim().isEmpty()) {
            throw new IllegalArgumentException("A categoria é obrigatória.");
        }
        if (categoria.trim().length() > 80) {
            throw new IllegalArgumentException("A categoria deve ter no máximo 80 caracteres.");
        }
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero.");
        }
        if (valor.stripTrailingZeros().scale() > 2 || valor.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O valor da despesa excede o limite permitido.");
        }
    }
}

