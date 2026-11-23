package com.contia.service;

import com.contia.model.Estabelecimento;
import com.contia.model.PlanoEstabelecimento;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.security.EscopoEstabelecimentoService;
import com.contia.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AcessoPlanoServiceTest {
    private EstabelecimentoRepository estabelecimentos;
    private AcessoPlanoService acessoPlano;
    private final UsuarioAutenticado proprietario = UsuarioAutenticado.proprietario(41L);

    @BeforeEach
    void configurar() {
        estabelecimentos = mock(EstabelecimentoRepository.class);
        acessoPlano = new AcessoPlanoService(estabelecimentos, new EscopoEstabelecimentoService(estabelecimentos));
    }

    @Test
    void permiteInteligenciaFinanceiraEpdfNoPremiumDoProprietario() {
        Estabelecimento estabelecimento = estabelecimento(PlanoEstabelecimento.PREMIUM);
        when(estabelecimentos.findByIdAndProprietarioId(9L, 41L)).thenReturn(Optional.of(estabelecimento));
        when(estabelecimentos.findById(9L)).thenReturn(Optional.of(estabelecimento));

        assertDoesNotThrow(() -> acessoPlano.exigirPremium(9L, proprietario));
    }

    @Test
    void bloqueiaInteligenciaFinanceiraEpdfNoBasico() {
        Estabelecimento estabelecimento = estabelecimento(PlanoEstabelecimento.BASICO);
        when(estabelecimentos.findByIdAndProprietarioId(9L, 41L)).thenReturn(Optional.of(estabelecimento));
        when(estabelecimentos.findById(9L)).thenReturn(Optional.of(estabelecimento));

        assertThrows(AccessDeniedException.class, () -> acessoPlano.exigirPremium(9L, proprietario));
    }

    @Test
    void bloqueiaEstabelecimentoDeOutroProprietarioAntesDeConsultarPlano() {
        when(estabelecimentos.findByIdAndProprietarioId(9L, 41L)).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class, () -> acessoPlano.exigirPremium(9L, proprietario));
    }

    private Estabelecimento estabelecimento(PlanoEstabelecimento plano) {
        Estabelecimento estabelecimento = mock(Estabelecimento.class);
        when(estabelecimento.getPlano()).thenReturn(plano);
        return estabelecimento;
    }
}
