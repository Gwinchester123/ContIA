package com.contia.service;

import com.contia.model.PlanoEstabelecimento;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.security.EscopoEstabelecimentoService;
import com.contia.security.UsuarioAutenticado;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AcessoPlanoService {
    private final EstabelecimentoRepository estabelecimentos;
    private final EscopoEstabelecimentoService escopo;

    public AcessoPlanoService(EstabelecimentoRepository estabelecimentos, EscopoEstabelecimentoService escopo) {
        this.estabelecimentos = estabelecimentos;
        this.escopo = escopo;
    }

    public void exigirPremium(Long estabelecimentoId, UsuarioAutenticado usuario) {
        escopo.validar(estabelecimentoId, usuario);
        boolean premium = estabelecimentos.findById(estabelecimentoId)
                .map(estabelecimento -> estabelecimento.getPlano() == PlanoEstabelecimento.PREMIUM)
                .orElse(false);
        if (!premium) throw new AccessDeniedException("Este recurso está disponível somente no plano Premium.");
    }
}
