package com.contia.security;

import com.contia.model.Despesa;
import com.contia.model.Estabelecimento;
import com.contia.model.Funcionario;
import com.contia.model.Proprietario;
import com.contia.model.PlanoEstabelecimento;
import com.contia.model.Venda;
import com.contia.model.VendaItem;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.repository.FuncionarioRepository;
import com.contia.repository.ProprietarioRepository;
import com.contia.service.DespesaService;
import com.contia.service.RelatorioFinanceiroService;
import com.contia.service.VendaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FuncionarioWebTest {
    @Autowired MockMvc mvc;
    @Autowired SessaoTokenService tokenService;
    @MockitoBean FuncionarioRepository funcionarios;
    @MockitoBean EstabelecimentoRepository estabelecimentos;
    @MockitoBean ProprietarioRepository proprietarios;
    @MockitoBean DespesaService despesaService;
    @MockitoBean RelatorioFinanceiroService relatorioService;
    @MockitoBean VendaService vendaService;

    @Test
    void proprietarioCadastraFuncionarioNoProprioEstabelecimento() throws Exception {
        when(estabelecimentos.findByIdAndProprietarioId(9L, 41L)).thenReturn(Optional.of(estabelecimento(9L)));
        when(funcionarios.findByEmail("ana@contia.com")).thenReturn(Optional.empty());
        when(proprietarios.existsByEmail("ana@contia.com")).thenReturn(false);
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(invocation -> {
            Funcionario salvo = invocation.getArgument(0);
            ReflectionTestUtils.setField(salvo, "id", 71L);
            return salvo;
        });

        mvc.perform(post("/api/funcionarios").header("Authorization", tokenProprietario())
                        .contentType("application/json")
                        .content("{\"nome\":\"Ana Lima\",\"email\":\"ana@contia.com\",\"senha\":\"senha-segura\",\"estabelecimentoId\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(71))
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist());

        verify(funcionarios).save(argThat(f -> f.getSenha().startsWith("$2") && f.getEstabelecimento().getId().equals(9L)));
    }

    @Test
    void proprietarioEscolhePlanoPremiumAoCadastrarEstabelecimento() throws Exception {
        prepararCadastroEstabelecimento();

        mvc.perform(post("/api/estabelecimentos").header("Authorization", tokenProprietario())
                        .contentType("application/json")
                        .content("{\"nome\":\"Café ContIA\",\"tipo\":\"Alimentação\",\"plano\":\"PREMIUM\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("PREMIUM"));

        verify(estabelecimentos).save(argThat(e -> e.getPlano() == PlanoEstabelecimento.PREMIUM));
    }

    @Test
    void estabelecimentoSemPlanoInformadoContinuaBasico() throws Exception {
        prepararCadastroEstabelecimento();

        mvc.perform(post("/api/estabelecimentos").header("Authorization", tokenProprietario())
                        .contentType("application/json")
                        .content("{\"nome\":\"Café ContIA\",\"tipo\":\"Alimentação\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plano").value("BASICO"));
    }

    @Test
    void proprietarioListaSomenteSeusFuncionarios() throws Exception {
        when(funcionarios.findByEstabelecimentoProprietarioIdOrderByNomeAsc(41L))
                .thenReturn(List.of(funcionario(71L, "Ana Lima", "ana@contia.com", 9L, true, "senha-segura")));
        mvc.perform(get("/api/funcionarios").header("Authorization", tokenProprietario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(71))
                .andExpect(jsonPath("$[0].estabelecimentoId").value(9))
                .andExpect(jsonPath("$[0].senha").doesNotExist());
        verify(funcionarios).findByEstabelecimentoProprietarioIdOrderByNomeAsc(41L);
    }

    @Test
    void rejeitaCadastroEmEstabelecimentoDeOutroProprietario() throws Exception {
        when(estabelecimentos.findByIdAndProprietarioId(99L, 41L)).thenReturn(Optional.empty());
        mvc.perform(post("/api/funcionarios").header("Authorization", tokenProprietario())
                        .contentType("application/json")
                        .content("{\"nome\":\"Ana\",\"email\":\"ana@contia.com\",\"senha\":\"senha-segura\",\"estabelecimentoId\":99}"))
                .andExpect(status().isForbidden());
        verify(funcionarios, never()).save(any());
    }

    @Test
    void funcionarioFazLoginEAuthMeInformaSeuEstabelecimento() throws Exception {
        when(funcionarios.findByEmail("ana@contia.com"))
                .thenReturn(Optional.of(funcionario(71L, "Ana Lima", "ana@contia.com", 9L, true, "senha-segura")));
        String resposta = mvc.perform(post("/api/funcionarios/login").contentType("application/json")
                        .content("{\"email\":\"ANA@CONTIA.COM\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.estabelecimentoId").value(9))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        String token = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(resposta).get("token").asText();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(71))
                .andExpect(jsonPath("$.perfil").value("FUNCIONARIO"))
                .andExpect(jsonPath("$.estabelecimentoId").value(9));
    }

    @Test
    void funcionarioDesativadoNaoFazLogin() throws Exception {
        when(funcionarios.findByEmail("ana@contia.com"))
                .thenReturn(Optional.of(funcionario(71L, "Ana Lima", "ana@contia.com", 9L, false, "senha-segura")));
        mvc.perform(post("/api/funcionarios/login").contentType("application/json")
                        .content("{\"email\":\"ana@contia.com\",\"senha\":\"senha-segura\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void proprietarioPodeEditarEDesativarFuncionarioEIssoRevogaSeuToken() throws Exception {
        Funcionario funcionario = funcionario(71L, "Ana Lima", "ana@contia.com", 9L, true, "senha-segura");
        when(funcionarios.findByIdAndEstabelecimentoProprietarioId(71L, 41L))
                .thenReturn(Optional.of(funcionario));
        when(funcionarios.findByEmail("ana@contia.com")).thenReturn(Optional.of(funcionario));
        when(proprietarios.existsByEmail("ana@contia.com")).thenReturn(false);
        when(funcionarios.save(any(Funcionario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(put("/api/funcionarios/71").header("Authorization", tokenProprietario())
                        .contentType("application/json")
                        .content("{\"nome\":\"Ana Souza\",\"email\":\"ana@contia.com\",\"senha\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Ana Souza"));

        String tokenAtivo = tokenFuncionario(71L, 9L);
        mvc.perform(patch("/api/funcionarios/71/situacao").header("Authorization", tokenProprietario())
                        .contentType("application/json").content("{\"ativo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ativo").value(false));
        mvc.perform(get("/api/auth/me").header("Authorization", tokenAtivo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void funcionarioNaoAcessaDespesasRelatoriosProdutosOuGestaoDeFuncionarios() throws Exception {
        String token = tokenFuncionario(71L, 9L);
        mvc.perform(get("/api/despesas/estabelecimento/9").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/relatorios-financeiros/analise?estabelecimentoId=9&inicio=2026-01-01&fim=2026-01-31")
                        .header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/produtos").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(get("/api/funcionarios").header("Authorization", token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/funcionarios").header("Authorization", token)
                        .contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void funcionarioSoRegistraVendaNoEstabelecimentoAssociado() throws Exception {
        Venda venda = venda(9L);
        when(vendaService.registrar(eq(9L), anyList(), any())).thenReturn(venda);
        mvc.perform(post("/api/vendas").header("Authorization", tokenFuncionario(71L, 9L))
                        .contentType("application/json")
                        .content("{\"estabelecimentoId\":9,\"produto\":\"Café\",\"quantidade\":1,\"valorUnitario\":5.00}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/vendas").header("Authorization", tokenFuncionario(71L, 9L))
                        .contentType("application/json")
                        .content("{\"estabelecimentoId\":10,\"produto\":\"Café\",\"quantidade\":1,\"valorUnitario\":5.00}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/vendas/estabelecimento/9").header("Authorization", tokenFuncionario(71L, 9L)))
                .andExpect(status().isOk());
        verify(vendaService, times(1)).registrar(any(), anyList(), any());
    }

    @Test
    void endpointAceitaMultiplosItensEIgnoraSubtotaisETotalEnviados() throws Exception {
        Venda venda = venda(9L);
        venda.setTotal(new BigDecimal("11.50"));
        venda.setFuncionarioResponsavel(funcionario(71L, "Ana Lima", "ana@contia.com", 9L, true, "senha"));
        venda.getItens().clear();
        venda.adicionarItem(item("Café", 1, "5.00", "5.00"));
        venda.adicionarItem(item("Pão", 2, "3.25", "6.50"));
        when(vendaService.registrar(eq(9L), anyList(), any())).thenReturn(venda);

        mvc.perform(post("/api/vendas").header("Authorization", tokenFuncionario(71L, 9L))
                        .contentType("application/json")
                        .content("{\"estabelecimentoId\":9,\"itens\":["
                                + "{\"produto\":\"Café\",\"quantidade\":1,\"valorUnitario\":5.00,\"subtotal\":9999},"
                                + "{\"produto\":\"Pão\",\"quantidade\":2,\"valorUnitario\":3.25,\"subtotal\":-1}],\"total\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.total").value(11.50))
                .andExpect(jsonPath("$.responsavelId").value(71))
                .andExpect(jsonPath("$.responsavelNome").value("Ana Lima"))
                .andExpect(jsonPath("$.responsavelPerfil").value("FUNCIONARIO"));

        ArgumentCaptor<List<VendaService.ItemEntrada>> captor = ArgumentCaptor.forClass(List.class);
        verify(vendaService).registrar(eq(9L), captor.capture(), any());
        assertEquals(2, captor.getValue().size());
        assertEquals(new BigDecimal("5.00"), captor.getValue().get(0).valorUnitario());
        assertEquals(new BigDecimal("3.25"), captor.getValue().get(1).valorUnitario());
    }

    @Test
    void proprietarioPodeListarVendasEIdentificarResponsavel() throws Exception {
        Venda venda = venda(9L);
        venda.setFuncionarioResponsavel(funcionario(71L, "Ana Lima", "ana@contia.com", 9L, true, "senha"));
        when(vendaService.listar(any())).thenReturn(List.of(venda));

        mvc.perform(get("/api/vendas").header("Authorization", tokenProprietario()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].itens.length()").value(1))
                .andExpect(jsonPath("$[0].itens[0].produto").value("Café"))
                .andExpect(jsonPath("$[0].responsavelId").value(71))
                .andExpect(jsonPath("$[0].responsavelNome").value("Ana Lima"))
                .andExpect(jsonPath("$[0].responsavelPerfil").value("FUNCIONARIO"));
    }

    @Test
    void funcionarioPodeConsultarSugestoesDoProprioEstabelecimentoSemAcessarOutro() throws Exception {
        when(vendaService.sugerirItens(eq(9L), eq("caf"), any()))
                .thenReturn(List.of(new VendaService.SugestaoItem("Café coado", new BigDecimal("4.50"))));
        mvc.perform(get("/api/vendas/sugestoes?estabelecimentoId=9&q=caf")
                        .header("Authorization", tokenFuncionario(71L, 9L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].produto").value("Café coado"))
                .andExpect(jsonPath("$[0].ultimoPreco").value(4.50));
        mvc.perform(get("/api/vendas/sugestoes?estabelecimentoId=10&q=caf")
                        .header("Authorization", tokenFuncionario(71L, 9L)))
                .andExpect(status().isForbidden());
        verify(vendaService, times(1)).sugerirItens(eq(9L), eq("caf"), any());
    }

    @Test
    void proprietarioMantemAcessoAsDespesasDoProprioEstabelecimento() throws Exception {
        when(estabelecimentos.findByIdAndProprietarioId(9L, 41L)).thenReturn(Optional.of(estabelecimento(9L)));
        when(despesaService.listarPorEstabelecimento(9L)).thenReturn(List.<Despesa>of());
        mvc.perform(get("/api/despesas/estabelecimento/9").header("Authorization", tokenProprietario()))
                .andExpect(status().isOk());
        verify(despesaService).listarPorEstabelecimento(9L);
    }

    @Test
    void proprietarioRegistraLoteEBackendDefineDataHoraFortaleza() throws Exception {
        when(estabelecimentos.findByIdAndProprietarioId(9L, 41L)).thenReturn(Optional.of(estabelecimento(9L)));
        when(despesaService.registrarLote(eq(9L), anyList())).thenAnswer(invocation -> {
            List<DespesaService.DespesaEntrada> entradas = invocation.getArgument(1);
            return List.of(despesa(81L, entradas.get(0), 9L), despesa(82L, entradas.get(1), 9L));
        });

        mvc.perform(post("/api/despesas/lote").header("Authorization", tokenProprietario())
                        .contentType("application/json")
                        .content("{\"estabelecimentoId\":9,\"despesas\":["
                                + "{\"descricao\":\"Farinha\",\"categoria\":\"Ingredientes\",\"valor\":12.50,\"dataHora\":\"2000-01-01T00:00:00\"},"
                                + "{\"descricao\":\"Internet\",\"categoria\":\"Despesas operacionais\",\"valor\":99.90}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].descricao").value("Farinha"))
                .andExpect(jsonPath("$[0].valor").value(12.50))
                .andExpect(jsonPath("$[0].dataHora").value(org.hamcrest.Matchers.endsWith("-03:00")))
                .andExpect(jsonPath("$[1].descricao").value("Internet"));

        ArgumentCaptor<List<DespesaService.DespesaEntrada>> captor = ArgumentCaptor.forClass(List.class);
        verify(despesaService).registrarLote(eq(9L), captor.capture());
        assertEquals(2, captor.getValue().size());
        assertEquals(new BigDecimal("12.50"), captor.getValue().get(0).valor());
    }

    @Test
    void loteDeDespesaRejeitaEstabelecimentoNaoAutorizadoEFuncionario() throws Exception {
        when(estabelecimentos.findByIdAndProprietarioId(99L, 41L)).thenReturn(Optional.empty());
        mvc.perform(post("/api/despesas/lote").header("Authorization", tokenProprietario())
                        .contentType("application/json")
                        .content("{\"estabelecimentoId\":99,\"despesas\":[{\"descricao\":\"X\",\"categoria\":\"Insumos\",\"valor\":1}]}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/despesas/lote").header("Authorization", tokenFuncionario(71L, 9L))
                        .contentType("application/json")
                        .content("{\"estabelecimentoId\":9,\"despesas\":[{\"descricao\":\"X\",\"categoria\":\"Insumos\",\"valor\":1}]}"))
                .andExpect(status().isForbidden());
        verify(despesaService, never()).registrarLote(any(), anyList());
    }

    @Test
    void proprietarioBaixaPdfDoEstabelecimentoAutorizado() throws Exception {
        Estabelecimento premium = estabelecimento(9L);
        when(estabelecimentos.findByIdAndProprietarioId(9L, 41L)).thenReturn(Optional.of(premium));
        when(estabelecimentos.findById(9L)).thenReturn(Optional.of(premium));
        byte[] pdf = "%PDF-relatorio".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        when(relatorioService.gerarPdf(eq(41L), eq(9L), any(), any())).thenReturn(pdf);

        mvc.perform(get("/api/relatorios-financeiros/pdf?estabelecimentoId=9&inicio=2026-03-01&fim=2026-03-31")
                        .header("Authorization", tokenProprietario()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(content().bytes(pdf));
        verify(relatorioService).gerarPdf(eq(41L), eq(9L), any(), any());
    }

    @Test
    void proprietarioNaoAnalisaDadosDeOutroEstabelecimento() throws Exception {
        when(estabelecimentos.findByIdAndProprietarioId(99L, 41L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/relatorios-financeiros/analise?estabelecimentoId=99&inicio=2026-03-01&fim=2026-03-31")
                        .header("Authorization", tokenProprietario()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(relatorioService);
    }

    private String tokenProprietario() {
        return "Bearer " + tokenService.emitir(UsuarioAutenticado.proprietario(41L));
    }

    private String tokenFuncionario(Long id, Long estabelecimentoId) {
        return "Bearer " + tokenService.emitir(new UsuarioAutenticado(id, PerfilAcesso.FUNCIONARIO, estabelecimentoId));
    }

    private void prepararCadastroEstabelecimento() {
        Proprietario proprietario = new Proprietario();
        ReflectionTestUtils.setField(proprietario, "id", 41L);
        when(proprietarios.findById(41L)).thenReturn(Optional.of(proprietario));
        when(estabelecimentos.save(any(Estabelecimento.class))).thenAnswer(invocation -> {
            Estabelecimento estabelecimento = invocation.getArgument(0);
            ReflectionTestUtils.setField(estabelecimento, "id", 9L);
            return estabelecimento;
        });
    }

    private Estabelecimento estabelecimento(Long id) {
        Estabelecimento estabelecimento = new Estabelecimento();
        ReflectionTestUtils.setField(estabelecimento, "id", id);
        estabelecimento.setPlano(PlanoEstabelecimento.PREMIUM);
        return estabelecimento;
    }

    private Despesa despesa(Long id, DespesaService.DespesaEntrada entrada, Long estabelecimentoId) {
        Despesa despesa = new Despesa();
        ReflectionTestUtils.setField(despesa, "id", id);
        despesa.setDescricao(entrada.descricao());
        despesa.setCategoria(entrada.categoria());
        despesa.setValor(entrada.valor());
        despesa.setDataHora(LocalDateTime.now(ZoneId.of("America/Fortaleza")));
        despesa.setEstabelecimento(estabelecimento(estabelecimentoId));
        return despesa;
    }

    private Funcionario funcionario(Long id, String nome, String email, Long estabelecimentoId, boolean ativo, String senha) {
        Funcionario funcionario = new Funcionario();
        ReflectionTestUtils.setField(funcionario, "id", id);
        funcionario.setNome(nome);
        funcionario.setEmail(email);
        funcionario.setSenha(new BCryptPasswordEncoder().encode(senha));
        funcionario.setPerfil(PerfilAcesso.FUNCIONARIO);
        funcionario.setAtivo(ativo);
        funcionario.setEstabelecimento(estabelecimento(estabelecimentoId));
        return funcionario;
    }

    private Venda venda(Long estabelecimentoId) {
        Venda venda = new Venda();
        ReflectionTestUtils.setField(venda, "id", 901L);
        venda.setProduto("Café");
        venda.setQuantidade(1);
        venda.setValorUnitario(new BigDecimal("5.00"));
        venda.setTotal(new BigDecimal("5.00"));
        venda.setDataHora(LocalDateTime.now());
        venda.setEstabelecimento(estabelecimento(estabelecimentoId));
        return venda;
    }

    private VendaItem item(String produto, int quantidade, String valorUnitario, String subtotal) {
        VendaItem item = new VendaItem();
        item.setProduto(produto);
        item.setQuantidade(quantidade);
        item.setValorUnitario(new BigDecimal(valorUnitario));
        item.setSubtotal(new BigDecimal(subtotal));
        return item;
    }
}
