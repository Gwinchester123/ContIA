package com.contia.security;

public record UsuarioAutenticado(Long id, PerfilAcesso perfil, Long estabelecimentoId) {
    public static UsuarioAutenticado proprietario(Long id) {
        return new UsuarioAutenticado(id, PerfilAcesso.PROPRIETARIO, null);
    }
}
