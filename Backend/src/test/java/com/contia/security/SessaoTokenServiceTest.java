package com.contia.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class SessaoTokenServiceTest {
    @Test
    void tokenExpiradoEhRejeitado() throws Exception {
        SessaoTokenService service = new SessaoTokenService();
        UsuarioAutenticado usuario = UsuarioAutenticado.proprietario(41L);
        String token = service.emitir(usuario);

        @SuppressWarnings("unchecked")
        Map<String, Object> sessoes = (Map<String, Object>) ReflectionTestUtils.getField(service, "sessoes");
        Object sessao = sessoes.get(token);
        Constructor<?> construtor = sessao.getClass()
                .getDeclaredConstructor(UsuarioAutenticado.class, Instant.class);
        construtor.setAccessible(true);
        sessoes.put(token, construtor.newInstance(usuario, Instant.now().minusSeconds(1)));

        assertNull(service.autenticar(token));
        assertFalse(sessoes.containsKey(token));
    }

    @Test
    void tokenAtivoIdentificaUsuarioERevogacaoEncerraASessao() {
        SessaoTokenService service = new SessaoTokenService();
        UsuarioAutenticado usuario = UsuarioAutenticado.proprietario(41L);
        String token = service.emitir(usuario);

        assertSame(usuario, service.autenticar(token));
        service.revogar(PerfilAcesso.PROPRIETARIO, 41L);
        assertNull(service.autenticar(token));
    }
}
