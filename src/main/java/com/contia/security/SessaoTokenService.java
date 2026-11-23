package com.contia.security;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SessaoTokenService {
    private static final Duration DURACAO = Duration.ofHours(8);
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Sessao> sessoes = new ConcurrentHashMap<>();

    public String emitir(UsuarioAutenticado usuario) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        sessoes.put(token, new Sessao(usuario, Instant.now().plus(DURACAO)));
        return token;
    }

    public UsuarioAutenticado autenticar(String token) {
        if (token == null || token.isBlank()) return null;
        Sessao sessao = sessoes.get(token);
        if (sessao == null) return null;
        if (Instant.now().isAfter(sessao.expiraEm())) {
            sessoes.remove(token, sessao);
            return null;
        }
        return sessao.usuario();
    }

    public void revogar(PerfilAcesso perfil, Long usuarioId) {
        sessoes.entrySet().removeIf(entry -> entry.getValue().usuario().perfil() == perfil
                && entry.getValue().usuario().id().equals(usuarioId));
    }

    private record Sessao(UsuarioAutenticado usuario, Instant expiraEm) {}
}
