package com.contia.service;

import com.contia.model.Proprietario;
import com.contia.repository.ProprietarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProprietarioServiceAutenticacaoTest {
    @Test
    void aceitaSenhaBcryptDoProprietarioExistente() {
        ProprietarioRepository repository = mock(ProprietarioRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Proprietario existente = new Proprietario();
        existente.setEmail("dona@contia.com");
        existente.setSenha(encoder.encode("senha-correta"));
        when(repository.findByEmail("dona@contia.com")).thenReturn(Optional.of(existente));

        Proprietario autenticado = new ProprietarioService(repository).autenticar("DONA@contia.com", "senha-correta");
        assertSame(existente, autenticado);
        verify(repository).findByEmail("dona@contia.com");
    }

    @Test
    void rejeitaSenhaIncorreta() {
        ProprietarioRepository repository = mock(ProprietarioRepository.class);
        Proprietario existente = new Proprietario();
        existente.setSenha(new BCryptPasswordEncoder().encode("senha-correta"));
        when(repository.findByEmail("dona@contia.com")).thenReturn(Optional.of(existente));
        assertThrows(IllegalArgumentException.class,
                () -> new ProprietarioService(repository).autenticar("dona@contia.com", "senha-errada"));
    }

    @Test
    void alteraSenhaAtualizandoSomenteHashBcrypt() {
        ProprietarioRepository repository = mock(ProprietarioRepository.class);
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        Proprietario existente = new Proprietario();
        existente.setSenha(encoder.encode("senha-atual-segura"));
        when(repository.findById(41L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        new ProprietarioService(repository).alterarSenha(41L, "senha-atual-segura", "senha-nova-segura");

        assertTrue(encoder.matches("senha-nova-segura", existente.getSenha()));
        assertFalse(encoder.matches("senha-atual-segura", existente.getSenha()));
        verify(repository).save(existente);
    }

    @Test
    void naoAlteraSenhaSeSenhaAtualNaoConfere() {
        ProprietarioRepository repository = mock(ProprietarioRepository.class);
        Proprietario existente = new Proprietario();
        existente.setSenha(new BCryptPasswordEncoder().encode("senha-atual-segura"));
        when(repository.findById(41L)).thenReturn(Optional.of(existente));

        assertThrows(IllegalArgumentException.class,
                () -> new ProprietarioService(repository).alterarSenha(41L, "incorreta", "senha-nova-segura"));
        verify(repository, never()).save(any(Proprietario.class));
    }
}
