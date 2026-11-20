
package com.contia.service;

import com.contia.model.Venda;
import com.contia.model.Estabelecimento;
import com.contia.repository.VendaRepository;
import com.contia.repository.EstabelecimentoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class VendaService {

    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");

    private final VendaRepository vendaRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;

    public VendaService(
            VendaRepository vendaRepository,
            EstabelecimentoRepository estabelecimentoRepository) {
        this.vendaRepository = vendaRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
    }

    public Venda registrar(
            Long estabelecimentoId,
            String produto,
            Integer quantidade,
            BigDecimal valorUnitario) {

        validarDados(produto, quantidade, valorUnitario);

        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado."));

        BigDecimal total = valorUnitario.multiply(BigDecimal.valueOf(quantidade));

        Venda venda = new Venda();
        venda.setProduto(produto.trim());
        venda.setQuantidade(quantidade);
        venda.setValorUnitario(valorUnitario);
        venda.setTotal(total);
        venda.setDataHora(LocalDateTime.now());
        venda.setEstabelecimento(estabelecimento);

        return vendaRepository.save(venda);
    }

    public List<Venda> listarPorEstabelecimento(Long estabelecimentoId) {
        return vendaRepository.findByEstabelecimentoId(estabelecimentoId);
    }

    public Venda atualizar(
            Long vendaId,
            Long estabelecimentoId,
            String produto,
            Integer quantidade,
            BigDecimal valorUnitario) {

        validarDados(produto, quantidade, valorUnitario);
        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada."));

        if (estabelecimentoId == null
                || !estabelecimentoId.equals(venda.getEstabelecimento().getId())) {
            throw new IllegalArgumentException("Venda não encontrada para este estabelecimento.");
        }

        venda.setProduto(produto.trim());
        venda.setQuantidade(quantidade);
        venda.setValorUnitario(valorUnitario);
        venda.setTotal(valorUnitario.multiply(BigDecimal.valueOf(quantidade)));
        return vendaRepository.save(venda);
    }

    public void excluir(Long vendaId, Long estabelecimentoId) {
        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada."));

        if (estabelecimentoId == null
                || !estabelecimentoId.equals(venda.getEstabelecimento().getId())) {
            throw new IllegalArgumentException("Venda não encontrada para este estabelecimento.");
        }

        vendaRepository.delete(venda);
    }

    private void validarDados(String produto, Integer quantidade, BigDecimal valorUnitario) {
        if (produto == null || produto.trim().isEmpty()) {
            throw new IllegalArgumentException("O produto é obrigatório.");
        }
        if (produto.trim().length() > 100) {
            throw new IllegalArgumentException("O nome do produto deve ter no máximo 100 caracteres.");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor unitário deve ser maior que zero.");
        }
        if (valorUnitario.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O valor unitário deve ter no máximo duas casas decimais.");
        }
        if (valorUnitario.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O valor unitário excede o limite permitido.");
        }
        BigDecimal total = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
        if (total.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O total da venda excede o limite permitido.");
        }
    }
}
