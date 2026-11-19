package com.contia.repository;

import com.contia.model.Proprietario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProprietarioRepository extends JpaRepository<Proprietario, Long> {

    Optional<Proprietario> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Proprietario> findByTelefone(String telefone);
}