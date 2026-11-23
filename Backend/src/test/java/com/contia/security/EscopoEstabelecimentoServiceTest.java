package com.contia.security;

import com.contia.repository.EstabelecimentoRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class EscopoEstabelecimentoServiceTest {
    private final EstabelecimentoRepository repository = mock(EstabelecimentoRepository.class);
    private final EscopoEstabelecimentoService service = new EscopoEstabelecimentoService(repository);

    @Test
    void proprietarioNaoAcessaEstabelecimentoDeOutroProprietario() {
        when(repository.findByIdAndProprietarioId(88L, 41L)).thenReturn(Optional.empty());
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.validar(88L, UsuarioAutenticado.proprietario(41L)));
        verify(repository).findByIdAndProprietarioId(88L, 41L);
    }

    @Test
    void funcionarioSoPodeUsarSeuEstabelecimentoAssociado() {
        UsuarioAutenticado funcionario = new UsuarioAutenticado(5L, PerfilAcesso.FUNCIONARIO, 12L);
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> service.validar(13L, funcionario));
        service.validar(12L, funcionario);
        verifyNoInteractions(repository);
    }
}
