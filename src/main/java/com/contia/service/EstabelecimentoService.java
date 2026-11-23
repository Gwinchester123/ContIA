
package com.contia.service;

import com.contia.model.Estabelecimento;
import com.contia.model.Proprietario;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.repository.ProprietarioRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EstabelecimentoService {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProprietarioRepository proprietarioRepository;

    public EstabelecimentoService(
            EstabelecimentoRepository estabelecimentoRepository,
            ProprietarioRepository proprietarioRepository) {

        this.estabelecimentoRepository = estabelecimentoRepository;
        this.proprietarioRepository = proprietarioRepository;
    }

    public Estabelecimento cadastrar(
            Long proprietarioId,
            Estabelecimento estabelecimento) {

        if (estabelecimento.getNome() == null ||
                estabelecimento.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "O nome do estabelecimento é obrigatório.");
        }

        Proprietario proprietario = proprietarioRepository
                .findById(proprietarioId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Proprietário não encontrado."));

        estabelecimento.setProprietario(proprietario);

        return estabelecimentoRepository.save(estabelecimento);
    }

    public List<Estabelecimento> listarPorProprietario(Long proprietarioId) {
        return estabelecimentoRepository
                .findByProprietarioId(proprietarioId);
    }

    public Estabelecimento atualizarNome(Long estabelecimentoId, Long proprietarioId, String novoNome) {

    if (novoNome == null || novoNome.trim().isEmpty()) {
        throw new IllegalArgumentException("O nome do estabelecimento é obrigatório.");
    }

    Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
        .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado."));

    if (!estabelecimento.getProprietario().getId().equals(proprietarioId)) {
        throw new IllegalArgumentException("Você não tem permissão para editar este estabelecimento.");
    }

    estabelecimento.setNome(novoNome.trim());

    return estabelecimentoRepository.save(estabelecimento);
}
}
