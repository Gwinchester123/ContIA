package com.contia.security;

import com.contia.controller.ProprietarioController;
import com.contia.model.Proprietario;
import com.contia.security.SessaoTokenService;
import com.contia.security.UsuarioAutenticado;
import com.contia.service.ProprietarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AutenticacaoWebTest {
    @Autowired MockMvc mvc;
    @Autowired SessaoTokenService tokenService;
    @MockitoBean ProprietarioService proprietarioService;

    @Test
    void requisicaoSemTokenEhNegada() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginDeProprietarioEmiteTokenEIdentificaPerfil() throws Exception {
        when(proprietarioService.autenticar("dona@contia.com", "senha-correta"))
                .thenReturn(proprietario());

        String resposta = mvc.perform(post("/api/proprietarios/login")
                        .header("Origin", "http://localhost:5174")
                        .contentType("application/json")
                        .content("{\"email\":\"dona@contia.com\",\"senha\":\"senha-correta\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5174"))
                .andExpect(jsonPath("$.perfil").value("PROPRIETARIO"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.senhaCriptografada").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String token = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resposta).get("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(41))
                .andExpect(jsonPath("$.perfil").value("PROPRIETARIO"));
    }

    @Test
    void preflightPermiteOrigensEHeadersDosFrontendsLocais() throws Exception {
        for (String origem : new String[] {"http://localhost:5173", "http://localhost:5174"}) {
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .options("/api/proprietarios/login")
                            .header("Origin", origem)
                            .header("Access-Control-Request-Method", "POST")
                            .header("Access-Control-Request-Headers", "authorization,content-type"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Access-Control-Allow-Origin", origem))
                    .andExpect(header().string("Access-Control-Allow-Methods", org.hamcrest.Matchers.containsString("POST")))
                    .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.allOf(
                            org.hamcrest.Matchers.containsStringIgnoringCase("authorization"),
                            org.hamcrest.Matchers.containsStringIgnoringCase("content-type"))));
        }
    }

    @Test
    void cadastroContinuaPublico() throws Exception {
        when(proprietarioService.cadastrar(any(Proprietario.class))).thenReturn(proprietario());
        mvc.perform(post("/api/proprietarios")
                        .contentType("application/json")
                        .content("{\"nome\":\"Dona\",\"email\":\"dona@contia.com\",\"senha\":\"hash\",\"telefone\":\"11999999999\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("PROPRIETARIO"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    void alterarSenhaUsaOProprietarioAutenticadoERevogaSeuToken() throws Exception {
        String token = "Bearer " + tokenService.emitir(UsuarioAutenticado.proprietario(41L));

        mvc.perform(put("/api/proprietarios/senha")
                        .header("Authorization", token)
                        .contentType("application/json")
                        .content("{\"senhaAtual\":\"senha-antiga\",\"novaSenha\":\"senha-nova-segura\"}"))
                .andExpect(status().isNoContent());

        verify(proprietarioService).alterarSenha(41L, "senha-antiga", "senha-nova-segura");
        mvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void senhaIncorretaNaoAutentica() throws Exception {
        when(proprietarioService.autenticar("dona@contia.com", "errada"))
                .thenThrow(new IllegalArgumentException("E-mail ou senha inválidos."));
        mvc.perform(post("/api/proprietarios/login")
                        .contentType("application/json")
                        .content("{\"email\":\"dona@contia.com\",\"senha\":\"errada\"}"))
                .andExpect(status().isBadRequest());
    }

    private Proprietario proprietario() {
        Proprietario proprietario = new Proprietario();
        ReflectionTestUtils.setField(proprietario, "id", 41L);
        proprietario.setNome("Dona");
        proprietario.setEmail("dona@contia.com");
        proprietario.setTelefone("+5511999999999");
        proprietario.setSenha("$2a$12$hash-interno-nao-retornado");
        return proprietario;
    }
}
