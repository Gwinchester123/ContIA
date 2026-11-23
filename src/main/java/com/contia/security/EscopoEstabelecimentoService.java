package com.contia.security;

import com.contia.repository.EstabelecimentoRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class EscopoEstabelecimentoService {
    private final EstabelecimentoRepository estabelecimentos;

    public EscopoEstabelecimentoService(EstabelecimentoRepository estabelecimentos) {
        this.estabelecimentos = estabelecimentos;
    }

    public void validar(Long estabelecimentoId, UsuarioAutenticado usuario) {
        if (estabelecimentoId == null || usuario == null) {
            throw new AccessDeniedException("Acesso ao estabelecimento não autorizado.");
        }
        boolean permitido = switch (usuario.perfil()) {
            case PROPRIETARIO -> estabelecimentos
                    .findByIdAndProprietarioId(estabelecimentoId, usuario.id()).isPresent();
            case FUNCIONARIO -> estabelecimentoId.equals(usuario.estabelecimentoId());
        };
        if (!permitido) throw new AccessDeniedException("Acesso ao estabelecimento não autorizado.");
    }
}
