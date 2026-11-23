package com.contia.service;

import com.contia.model.Estabelecimento;
import com.contia.model.Funcionario;
import com.contia.repository.EstabelecimentoRepository;
import com.contia.repository.FuncionarioRepository;
import com.contia.repository.ProprietarioRepository;
import com.contia.security.PerfilAcesso;
import com.contia.security.SessaoTokenService;
import com.contia.security.UsuarioAutenticado;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
public class FuncionarioService {
    private final FuncionarioRepository funcionarios;
    private final EstabelecimentoRepository estabelecimentos;
    private final ProprietarioRepository proprietarios;
    private final SessaoTokenService tokens;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public FuncionarioService(FuncionarioRepository funcionarios, EstabelecimentoRepository estabelecimentos,
            ProprietarioRepository proprietarios, SessaoTokenService tokens) {
        this.funcionarios = funcionarios;
        this.estabelecimentos = estabelecimentos;
        this.proprietarios = proprietarios;
        this.tokens = tokens;
    }

    public Funcionario cadastrar(Long proprietarioId, Long estabelecimentoId, String nome, String email, String senha) {
        validarNomeEmail(nome, email);
        validarSenha(senha);
        String emailNormalizado = normalizarEmail(email);
        validarEmailDisponivel(emailNormalizado, null);
        Estabelecimento estabelecimento = estabelecimentos
                .findByIdAndProprietarioId(estabelecimentoId, proprietarioId)
                .orElseThrow(() -> new AccessDeniedException("Estabelecimento não autorizado."));

        Funcionario funcionario = new Funcionario();
        funcionario.setNome(nome.trim());
        funcionario.setEmail(emailNormalizado);
        funcionario.setSenha(passwordEncoder.encode(senha));
        funcionario.setPerfil(PerfilAcesso.FUNCIONARIO);
        funcionario.setAtivo(true);
        funcionario.setEstabelecimento(estabelecimento);
        return funcionarios.save(funcionario);
    }

    public List<Funcionario> listar(Long proprietarioId) {
        return funcionarios.findByEstabelecimentoProprietarioIdOrderByNomeAsc(proprietarioId);
    }

    public Funcionario consultar(Long proprietarioId, Long funcionarioId) {
        return buscarDoProprietario(proprietarioId, funcionarioId);
    }

    public Funcionario atualizar(Long proprietarioId, Long funcionarioId, String nome, String email, String senha) {
        validarNomeEmail(nome, email);
        Funcionario funcionario = buscarDoProprietario(proprietarioId, funcionarioId);
        String emailNormalizado = normalizarEmail(email);
        validarEmailDisponivel(emailNormalizado, funcionarioId);
        funcionario.setNome(nome.trim());
        funcionario.setEmail(emailNormalizado);
        if (senha != null && !senha.isBlank()) {
            validarSenha(senha);
            funcionario.setSenha(passwordEncoder.encode(senha));
        }
        return funcionarios.save(funcionario);
    }

    public Funcionario alterarSituacao(Long proprietarioId, Long funcionarioId, boolean ativo) {
        Funcionario funcionario = buscarDoProprietario(proprietarioId, funcionarioId);
        funcionario.setAtivo(ativo);
        Funcionario salvo = funcionarios.save(funcionario);
        if (!ativo) tokens.revogar(PerfilAcesso.FUNCIONARIO, funcionarioId);
        return salvo;
    }

    public Funcionario autenticar(String email, String senha) {
        if (email == null || senha == null) throw credenciaisInvalidas();
        Funcionario funcionario = funcionarios.findByEmail(normalizarEmail(email)).orElseThrow(this::credenciaisInvalidas);
        if (!funcionario.isAtivo() || !passwordEncoder.matches(senha, funcionario.getSenha())) {
            throw credenciaisInvalidas();
        }
        return funcionario;
    }

    private Funcionario buscarDoProprietario(Long proprietarioId, Long funcionarioId) {
        return funcionarios.findByIdAndEstabelecimentoProprietarioId(funcionarioId, proprietarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Funcionário não encontrado."));
    }

    private void validarEmailDisponivel(String email, Long funcionarioIdAtual) {
        boolean usadoPorOutroFuncionario = funcionarios.findByEmail(email)
                .filter(existente -> !existente.getId().equals(funcionarioIdAtual))
                .isPresent();
        if (usadoPorOutroFuncionario || proprietarios.existsByEmail(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }
    }

    private void validarNomeEmail(String nome, String email) {
        if (nome == null || nome.isBlank()) throw new IllegalArgumentException("O nome é obrigatório.");
        if (nome.trim().length() > 100) throw new IllegalArgumentException("O nome deve ter no máximo 100 caracteres.");
        if (email == null || email.isBlank()) throw new IllegalArgumentException("O e-mail é obrigatório.");
        if (email.trim().length() > 150 || !email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.isBlank()) throw new IllegalArgumentException("A senha é obrigatória.");
        if (senha.length() < 8) throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres.");
    }

    private String normalizarEmail(String email) { return email.trim().toLowerCase(); }
    private IllegalArgumentException credenciaisInvalidas() {
        return new IllegalArgumentException("E-mail ou senha inválidos.");
    }
}
