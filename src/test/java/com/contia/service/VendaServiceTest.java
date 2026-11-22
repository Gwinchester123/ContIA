package com.contia.service;

import com.contia.model.Estabelecimento;
import com.contia.model.Funcionario;
import com.contia.model.Proprietario;
import com.contia.model.Venda;
import com.contia.model.VendaItem;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.repository.FuncionarioRepository;
import com.contia.repository.ProprietarioRepository;
import com.contia.repository.VendaRepository;
import com.contia.security.PerfilAcesso;
import com.contia.security.UsuarioAutenticado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VendaServiceTest {
    private VendaRepository vendas;
    private EstabelecimentoRepository estabelecimentos;
    private ProprietarioRepository proprietarios;
    private FuncionarioRepository funcionarios;
    private VendaService service;

    @BeforeEach
    void configurar() {
        vendas = mock(VendaRepository.class);
        estabelecimentos = mock(EstabelecimentoRepository.class);
        proprietarios = mock(ProprietarioRepository.class);
        funcionarios = mock(FuncionarioRepository.class);
        service = new VendaService(vendas, estabelecimentos, proprietarios, funcionarios);
        when(vendas.save(any(Venda.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void calculaSubtotaisETotalNoBackendEAssociaProprietario() {
        Estabelecimento estabelecimento = estabelecimento(12L);
        Proprietario proprietario = new Proprietario();
        ReflectionTestUtils.setField(proprietario, "id", 7L);
        when(estabelecimentos.findById(12L)).thenReturn(Optional.of(estabelecimento(12L)));
        when(estabelecimentos.findByIdAndProprietarioId(12L, 7L)).thenReturn(Optional.of(estabelecimento));
        when(proprietarios.findById(7L)).thenReturn(Optional.of(proprietario));

        Venda venda = service.registrar(12L, List.of(
                new VendaService.ItemEntrada("  Suco de laranja  ", 2, new BigDecimal("4.50")),
                new VendaService.ItemEntrada("Pão", 1, new BigDecimal("3.00"))),
                UsuarioAutenticado.proprietario(7L));

        assertEquals(new BigDecimal("12.00"), venda.getTotal());
        assertEquals(2, venda.getItens().size());
        assertEquals("Suco de laranja", venda.getItens().get(0).getProduto());
        assertEquals(new BigDecimal("9.00"), venda.getItens().get(0).getSubtotal());
        assertEquals(new BigDecimal("3.00"), venda.getItens().get(1).getSubtotal());
        assertEquals(7L, venda.getProprietarioResponsavel().getId());
        assertNull(venda.getFuncionarioResponsavel());
        assertEquals("Suco de laranja", venda.getProduto());
    }

    @Test
    void funcionarioEAssociadoAutomaticamenteEListagemFiltraPorEle() {
        Funcionario funcionario = funcionario(28L, 12L);
        UsuarioAutenticado identidade = new UsuarioAutenticado(28L, PerfilAcesso.FUNCIONARIO, 12L);
        when(estabelecimentos.findById(12L)).thenReturn(Optional.of(estabelecimento(12L)));
        when(funcionarios.findById(28L)).thenReturn(Optional.of(funcionario));

        Venda venda = service.registrar(12L,
                List.of(new VendaService.ItemEntrada("Bolo", 1, new BigDecimal("8.00"))), identidade);
        service.listarPorEstabelecimento(12L, identidade);

        assertEquals(28L, venda.getFuncionarioResponsavel().getId());
        assertNull(venda.getProprietarioResponsavel());
        verify(vendas).findByEstabelecimentoIdAndFuncionarioResponsavelId(12L, 28L);
        verify(vendas, never()).findByEstabelecimentoId(12L);
    }

    @Test
    void listagemGeralFiltraPorProprietarioOuPeloFuncionarioAutenticado() {
        Funcionario funcionario = funcionario(28L, 12L);
        when(funcionarios.findById(28L)).thenReturn(Optional.of(funcionario));

        service.listar(UsuarioAutenticado.proprietario(7L));
        service.listar(new UsuarioAutenticado(28L, PerfilAcesso.FUNCIONARIO, 12L));

        verify(vendas).findByEstabelecimentoProprietarioIdOrderByDataHoraDesc(7L);
        verify(vendas).findByEstabelecimentoIdAndFuncionarioResponsavelId(12L, 28L);
        verify(estabelecimentos, never()).findByIdAndProprietarioId(any(), any());
    }

    @Test
    void sugestoesAgrupamMaiusculasEspacosEUsamPrecoMaisRecenteSemFundirVariantes() {
        Estabelecimento estabelecimento = estabelecimento(12L);
        when(estabelecimentos.findByIdAndProprietarioId(12L, 7L)).thenReturn(Optional.of(estabelecimento));
        when(vendas.buscarHistoricoItens(12L)).thenReturn(List.of(
                item(" coca-cola ", "7.00", 12L, LocalDateTime.of(2026, 11, 20, 10, 0)),
                item("Coca-Cola", "5.00", 12L, LocalDateTime.of(2026, 11, 19, 10, 0)),
                item("Coca-Cola 2 litros", "12.00", 12L, LocalDateTime.of(2026, 11, 18, 10, 0))));

        List<VendaService.SugestaoItem> sugestoes = service.sugerirItens(12L, "  COCA ",
                UsuarioAutenticado.proprietario(7L));

        assertEquals(2, sugestoes.size());
        assertEquals("coca-cola", sugestoes.get(0).produto());
        assertEquals(new BigDecimal("7.00"), sugestoes.get(0).ultimoPreco());
        assertEquals("Coca-Cola 2 litros", sugestoes.get(1).produto());
        assertEquals(new BigDecimal("12.00"), sugestoes.get(1).ultimoPreco());
        verify(vendas).buscarHistoricoItens(12L);
    }

    @Test
    void rejeitaQuantidadePrecoOuListaInvalidosAntesDeSalvar() {
        UsuarioAutenticado proprietario = UsuarioAutenticado.proprietario(7L);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(12L, List.of(), proprietario));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(12L,
                List.of(new VendaService.ItemEntrada("Água", 0, new BigDecimal("2.00"))), proprietario));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(12L,
                List.of(new VendaService.ItemEntrada("Água", 1, new BigDecimal("-2.00"))), proprietario));
        verifyNoInteractions(vendas);
    }

    private Estabelecimento estabelecimento(Long id) {
        Estabelecimento estabelecimento = new Estabelecimento();
        ReflectionTestUtils.setField(estabelecimento, "id", id);
        return estabelecimento;
    }

    private Funcionario funcionario(Long id, Long estabelecimentoId) {
        Funcionario funcionario = new Funcionario();
        ReflectionTestUtils.setField(funcionario, "id", id);
        funcionario.setAtivo(true);
        funcionario.setEstabelecimento(estabelecimento(estabelecimentoId));
        return funcionario;
    }

    private VendaItem item(String produto, String valorUnitario, Long estabelecimentoId, LocalDateTime dataHora) {
        Venda venda = new Venda();
        venda.setEstabelecimento(estabelecimento(estabelecimentoId));
        venda.setDataHora(dataHora);
        VendaItem item = new VendaItem();
        item.setProduto(produto);
        item.setValorUnitario(new BigDecimal(valorUnitario));
        item.setVenda(venda);
        return item;
    }
}
