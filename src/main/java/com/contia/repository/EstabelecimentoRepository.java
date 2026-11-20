package com.contia.repository;

import com.contia.model.Estabelecimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, Long> {

    List<Estabelecimento> findByProprietarioId(Long proprietarioId);

    java.util.Optional<Estabelecimento> findByIdAndProprietarioId(Long id, Long proprietarioId);
}
