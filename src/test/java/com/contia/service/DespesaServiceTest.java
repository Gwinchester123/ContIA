package com.contia.service;

import com.contia.model.Despesa;
import com.contia.model.Estabelecimento;
import com.contia.repository.DespesaRepository;
import com.contia.repository.EstabelecimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class DespesaServiceTest {
    private DespesaRepository despesas;
    private EstabelecimentoRepository estabelecimentos;
    private DespesaService service;
    private Estabelecimento estabelecimento;

    @BeforeEach
    void configurar() {
        despesas = mock(DespesaRepository.class);
        estabelecimentos = mock(EstabelecimentoRepository.class);
        service = new DespesaService(despesas, estabelecimentos);
        estabelecimento = new Estabelecimento();
        when(estabelecimentos.findById(9L)).thenReturn(Optional.of(estabelecimento));
        when(despesas.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void salvaTodosOsLancamentosComDataHoraDoFusoDeFortaleza() {
        LocalDateTime antes = LocalDateTime.now(ZoneId.of("America/Fortaleza"));
        List<Despesa> salvas = service.registrarLote(9L, List.of(
                entrada("Farinha", "Ingredientes", "12.50"),
                entrada("Internet", "Despesas operacionais", "99.90")));
        LocalDateTime depois = LocalDateTime.now(ZoneId.of("America/Fortaleza"));

        assertEquals(2, salvas.size());
        assertEquals(new BigDecimal("12.50"), salvas.get(0).getValor());
        assertEquals(new BigDecimal("99.90"), salvas.get(1).getValor());
        assertTrue(salvas.stream().allMatch(d -> !d.getDataHora().isBefore(antes) && !d.getDataHora().isAfter(depois)));
        assertTrue(salvas.stream().allMatch(d -> d.getEstabelecimento() == estabelecimento));
        verify(despesas).saveAllAndFlush(anyList());
    }

    @Test
    void registraDespesaIndividualComDataHoraDoServidor() {
        when(despesas.save(any(Despesa.class))).thenAnswer(invocation -> invocation.getArgument(0));
        LocalDateTime antes = LocalDateTime.now(ZoneId.of("America/Fortaleza"));

        Despesa salva = service.registrar(9L, "Farinha", "Ingredientes", new BigDecimal("12.50"));

        LocalDateTime depois = LocalDateTime.now(ZoneId.of("America/Fortaleza"));
        assertEquals("Farinha", salva.getDescricao());
        assertEquals(new BigDecimal("12.50"), salva.getValor());
        assertTrue(!salva.getDataHora().isBefore(antes) && !salva.getDataHora().isAfter(depois));
        verify(despesas).save(any(Despesa.class));
    }

    @Test
    void itemInvalidoRejeitaLoteAntesDeQualquerGravacao() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarLote(9L, List.of(
                entrada("Farinha", "Ingredientes", "12.50"),
                entrada("Internet", "Despesas operacionais", "0"))));

        verifyNoInteractions(estabelecimentos, despesas);
    }

    @Test
    void loteVazioERejeitadoSemGravacao() {
        assertThrows(IllegalArgumentException.class, () -> service.registrarLote(9L, List.of()));
        verifyNoInteractions(estabelecimentos, despesas);
    }

    @Test
    void loteETransacionalParaRollbackEmFalhaDePersistencia() throws Exception {
        assertNotNull(DespesaService.class.getMethod("registrarLote", Long.class, List.class)
                .getAnnotation(Transactional.class));
    }

    private DespesaService.DespesaEntrada entrada(String descricao, String categoria, String valor) {
        return new DespesaService.DespesaEntrada(descricao, categoria, new BigDecimal(valor));
    }
}
