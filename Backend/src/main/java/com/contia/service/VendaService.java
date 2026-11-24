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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class VendaService {
    private static final BigDecimal VALOR_MAXIMO = new BigDecimal("99999999.99");
    private static final ZoneId FUSO_HORARIO = ZoneId.of("America/Fortaleza");

    private final VendaRepository vendaRepository;
    private final EstabelecimentoRepository estabelecimentoRepository;
    private final ProprietarioRepository proprietarioRepository;
    private final FuncionarioRepository funcionarioRepository;

    public VendaService(VendaRepository vendaRepository, EstabelecimentoRepository estabelecimentoRepository,
            ProprietarioRepository proprietarioRepository, FuncionarioRepository funcionarioRepository) {
        this.vendaRepository = vendaRepository;
        this.estabelecimentoRepository = estabelecimentoRepository;
        this.proprietarioRepository = proprietarioRepository;
        this.funcionarioRepository = funcionarioRepository;
    }

    @Transactional
    public Venda registrar(Long estabelecimentoId, List<ItemEntrada> entradas, UsuarioAutenticado usuario) {
        List<ItemCalculado> itens = validarECalcular(entradas);
        Estabelecimento estabelecimento = estabelecimentoRepository.findById(estabelecimentoId)
                .orElseThrow(() -> new IllegalArgumentException("Estabelecimento não encontrado."));

        Venda venda = new Venda();
        venda.setEstabelecimento(estabelecimento);
        venda.setDataHora(LocalDateTime.now(FUSO_HORARIO));
        atribuirResponsavel(venda, usuario, estabelecimentoId);

        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < itens.size(); index++) {
            ItemCalculado calculado = itens.get(index);
            VendaItem item = new VendaItem();
            item.setProduto(calculado.produto());
            item.setQuantidade(calculado.quantidade());
            item.setValorUnitario(calculado.valorUnitario());
            item.setSubtotal(calculado.subtotal());
            item.setOrdem(index);
            venda.adicionarItem(item);
            total = total.add(calculado.subtotal());
        }
        if (total.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O total da venda excede o limite permitido.");
        }

        // Mantém as colunas históricas intactas e compatíveis com clientes antigos.
        ItemCalculado primeiro = itens.get(0);
        venda.setProduto(primeiro.produto());
        venda.setQuantidade(primeiro.quantidade());
        venda.setValorUnitario(primeiro.valorUnitario());
        venda.setTotal(total.setScale(2, RoundingMode.UNNECESSARY));
        return vendaRepository.save(venda);
    }

    @Transactional(readOnly = true)
    public List<Venda> listarPorEstabelecimento(Long estabelecimentoId, UsuarioAutenticado usuario) {
        validarAcessoEstabelecimento(estabelecimentoId, usuario);
        if (usuario.perfil() == PerfilAcesso.FUNCIONARIO) {
            validarFuncionarioAtivo(usuario.id(), estabelecimentoId);
            return vendaRepository.findByEstabelecimentoIdAndFuncionarioResponsavelId(estabelecimentoId, usuario.id());
        }
        return vendaRepository.findByEstabelecimentoId(estabelecimentoId);
    }

    @Transactional(readOnly = true)
    public List<Venda> listar(UsuarioAutenticado usuario) {
        if (usuario == null) throw new AccessDeniedException("Usuário não autenticado.");
        if (usuario.perfil() == PerfilAcesso.PROPRIETARIO) {
            return vendaRepository.findByEstabelecimentoProprietarioIdOrderByDataHoraDesc(usuario.id());
        }
        Funcionario funcionario = validarFuncionarioAtivo(usuario.id(), usuario.estabelecimentoId());
        return vendaRepository.findByEstabelecimentoIdAndFuncionarioResponsavelId(
                funcionario.getEstabelecimento().getId(), funcionario.getId());
    }

    @Transactional(readOnly = true)
    public List<SugestaoItem> sugerirItens(Long estabelecimentoId, String consulta, UsuarioAutenticado usuario) {
        validarAcessoEstabelecimento(estabelecimentoId, usuario);
        if (usuario.perfil() == PerfilAcesso.FUNCIONARIO) {
            validarFuncionarioAtivo(usuario.id(), estabelecimentoId);
        }

        String busca = normalizarNome(consulta == null ? "" : consulta);
        Map<String, SugestaoItem> maisRecentesPorNome = new LinkedHashMap<>();
        for (VendaItem item : vendaRepository.buscarHistoricoItens(estabelecimentoId)) {
            String nomeExibicao = item.getProduto().trim().replaceAll("\\s+", " ");
            String nomeNormalizado = normalizarNome(nomeExibicao);
            if (nomeNormalizado.isEmpty() || !nomeNormalizado.startsWith(busca)) continue;
            maisRecentesPorNome.putIfAbsent(nomeNormalizado,
                    new SugestaoItem(nomeExibicao, item.getValorUnitario()));
            if (maisRecentesPorNome.size() == 10) break;
        }
        return List.copyOf(maisRecentesPorNome.values());
    }

    @Transactional
    public Venda atualizar(Long vendaId, Long estabelecimentoId, List<ItemEntrada> entradas,
            UsuarioAutenticado usuario) {
        validarAcessoEstabelecimento(estabelecimentoId, usuario);
        exigirProprietario(usuario);
        List<ItemCalculado> itens = validarECalcular(entradas);
        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada."));
        if (!estabelecimentoId.equals(venda.getEstabelecimento().getId())) {
            throw new IllegalArgumentException("Venda não encontrada para este estabelecimento.");
        }

        venda.setItens(new ArrayList<>());
        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < itens.size(); index++) {
            ItemCalculado calculado = itens.get(index);
            VendaItem item = new VendaItem();
            item.setProduto(calculado.produto());
            item.setQuantidade(calculado.quantidade());
            item.setValorUnitario(calculado.valorUnitario());
            item.setSubtotal(calculado.subtotal());
            item.setOrdem(index);
            venda.adicionarItem(item);
            total = total.add(calculado.subtotal());
        }
        if (total.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O total da venda excede o limite permitido.");
        }
        ItemCalculado primeiro = itens.get(0);
        venda.setProduto(primeiro.produto());
        venda.setQuantidade(primeiro.quantidade());
        venda.setValorUnitario(primeiro.valorUnitario());
        venda.setTotal(total.setScale(2, RoundingMode.UNNECESSARY));
        return vendaRepository.save(venda);
    }

    @Transactional
    public void excluir(Long vendaId, Long estabelecimentoId, UsuarioAutenticado usuario) {
        validarAcessoEstabelecimento(estabelecimentoId, usuario);
        exigirProprietario(usuario);
        Venda venda = vendaRepository.findById(vendaId)
                .orElseThrow(() -> new IllegalArgumentException("Venda não encontrada."));
        if (!estabelecimentoId.equals(venda.getEstabelecimento().getId())) {
            throw new IllegalArgumentException("Venda não encontrada para este estabelecimento.");
        }
        vendaRepository.delete(venda);
    }

    private void atribuirResponsavel(Venda venda, UsuarioAutenticado usuario, Long estabelecimentoId) {
        if (usuario == null) throw new AccessDeniedException("Usuário não autenticado.");
        if (usuario.perfil() == PerfilAcesso.PROPRIETARIO) {
            Estabelecimento autorizado = estabelecimentoRepository.findByIdAndProprietarioId(estabelecimentoId, usuario.id())
                    .orElseThrow(() -> new AccessDeniedException("Acesso ao estabelecimento não autorizado."));
            Proprietario responsavel = proprietarioRepository.findById(usuario.id())
                    .orElseThrow(() -> new AccessDeniedException("Proprietário autenticado não encontrado."));
            venda.setEstabelecimento(autorizado);
            venda.setProprietarioResponsavel(responsavel);
            return;
        }
        Funcionario responsavel = validarFuncionarioAtivo(usuario.id(), estabelecimentoId);
        venda.setFuncionarioResponsavel(responsavel);
    }

    private Funcionario validarFuncionarioAtivo(Long funcionarioId, Long estabelecimentoId) {
        Funcionario funcionario = funcionarioRepository.findById(funcionarioId)
                .filter(Funcionario::isAtivo)
                .orElseThrow(() -> new AccessDeniedException("Funcionário inativo ou não encontrado."));
        if (funcionario.getEstabelecimento() == null
                || !estabelecimentoId.equals(funcionario.getEstabelecimento().getId())) {
            throw new AccessDeniedException("Acesso ao estabelecimento não autorizado.");
        }
        return funcionario;
    }

    private void validarAcessoEstabelecimento(Long estabelecimentoId, UsuarioAutenticado usuario) {
        if (usuario == null || estabelecimentoId == null) {
            throw new AccessDeniedException("Acesso ao estabelecimento não autorizado.");
        }
        if (usuario.perfil() == PerfilAcesso.PROPRIETARIO) {
            if (estabelecimentoRepository.findByIdAndProprietarioId(estabelecimentoId, usuario.id()).isEmpty()) {
                throw new AccessDeniedException("Acesso ao estabelecimento não autorizado.");
            }
        } else {
            validarFuncionarioAtivo(usuario.id(), estabelecimentoId);
        }
    }

    private void exigirProprietario(UsuarioAutenticado usuario) {
        if (usuario.perfil() != PerfilAcesso.PROPRIETARIO) {
            throw new AccessDeniedException("Somente o proprietário pode alterar ou excluir vendas.");
        }
    }

    private List<ItemCalculado> validarECalcular(List<ItemEntrada> entradas) {
        if (entradas == null || entradas.isEmpty()) {
            throw new IllegalArgumentException("A venda deve conter pelo menos um item.");
        }
        List<ItemCalculado> itens = new ArrayList<>();
        for (ItemEntrada item : entradas) {
            if (item == null) throw new IllegalArgumentException("Item da venda inválido.");
            validarItem(item.produto(), item.quantidade(), item.valorUnitario());
            BigDecimal valorUnitario = item.valorUnitario().setScale(2, RoundingMode.UNNECESSARY);
            BigDecimal subtotal = valorUnitario.multiply(BigDecimal.valueOf(item.quantidade()))
                    .setScale(2, RoundingMode.UNNECESSARY);
            itens.add(new ItemCalculado(item.produto().trim(), item.quantidade(), valorUnitario, subtotal));
        }
        return itens;
    }

    private void validarItem(String produto, Integer quantidade, BigDecimal valorUnitario) {
        if (produto == null || produto.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do produto é obrigatório.");
        }
        if (produto.trim().length() > 100) {
            throw new IllegalArgumentException("O nome do produto deve ter no máximo 100 caracteres.");
        }
        if (quantidade == null || quantidade <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero.");
        }
        if (valorUnitario == null || valorUnitario.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor unitário deve ser maior que zero.");
        }
        if (valorUnitario.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException("O valor unitário deve ter no máximo duas casas decimais.");
        }
        if (valorUnitario.compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O valor unitário excede o limite permitido.");
        }
        if (valorUnitario.multiply(BigDecimal.valueOf(quantidade)).compareTo(VALOR_MAXIMO) > 0) {
            throw new IllegalArgumentException("O subtotal do item excede o limite permitido.");
        }
    }

    private String normalizarNome(String nome) {
        return nome.trim().replaceAll("\\s+", " ").toLowerCase(Locale.forLanguageTag("pt-BR"));
    }

    public record SugestaoItem(String produto, BigDecimal ultimoPreco) {}
    public record ItemEntrada(String produto, Integer quantidade, BigDecimal valorUnitario) {}
    private record ItemCalculado(String produto, Integer quantidade, BigDecimal valorUnitario, BigDecimal subtotal) {}
}
