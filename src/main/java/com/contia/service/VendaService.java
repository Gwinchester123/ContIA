
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

        if (produto == null || produto.trim().isEmpty()) {
            throw new IllegalArgumentException("O produto é obrigatório.");
        }

        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }

        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor unitário deve ser maior que zero.");
        }

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
}
