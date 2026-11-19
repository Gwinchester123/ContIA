
package com.contia.service;

import com.contia.model.Proprietario;
import com.contia.repository.ProprietarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class ProprietarioService {

    private final ProprietarioRepository proprietarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public ProprietarioService(ProprietarioRepository proprietarioRepository) {
        this.proprietarioRepository = proprietarioRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Proprietario cadastrar(Proprietario proprietario) {

        if (proprietario.getNome() == null || proprietario.getNome().isBlank()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }

        if (proprietario.getEmail() == null || proprietario.getEmail().isBlank()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }

        if (proprietario.getSenha() == null || proprietario.getSenha().isBlank()) {
            throw new IllegalArgumentException("A senha é obrigatória.");
        }

        if (proprietario.getTelefone() == null || proprietario.getTelefone().isBlank()) {
            throw new IllegalArgumentException("O telefone é obrigatório.");
        }

        String email = proprietario.getEmail().trim().toLowerCase();

        if (proprietarioRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Este e-mail já está cadastrado.");
        }

        String telefone = proprietario.getTelefone().replaceAll("\\D", "");

        if (telefone.length() == 12 || telefone.length() == 13) {
            if (!telefone.startsWith("55")) {
                throw new IllegalArgumentException("Código do país inválido.");
            }

            telefone = telefone.substring(2);
        }

        if (telefone.length() != 10 && telefone.length() != 11) {
            throw new IllegalArgumentException("Informe um telefone válido com DDD.");
        }

        telefone = "+55" + telefone;

        if (proprietarioRepository.findByTelefone(telefone).isPresent()) {
            throw new IllegalArgumentException("Este telefone já está cadastrado.");
        }

        proprietario.setNome(proprietario.getNome().trim());
        proprietario.setEmail(email);
        proprietario.setTelefone(telefone);

        String senhaCriptografada = passwordEncoder.encode(proprietario.getSenha());

        proprietario.setSenha(senhaCriptografada);

        return proprietarioRepository.save(proprietario);
    }

    public Proprietario autenticar(String email, String senha) {

        String emailNormalizado = email.trim().toLowerCase();

        Proprietario proprietario = proprietarioRepository
                .findByEmail(emailNormalizado)
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou senha inválidos."));

        if (!passwordEncoder.matches(senha, proprietario.getSenha())) {
            throw new IllegalArgumentException("E-mail ou senha inválidos.");
        }

        return proprietario;
    }
}
