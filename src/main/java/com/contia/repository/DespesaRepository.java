
package com.contia.repository;

import com.contia.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findByEstabelecimentoId(Long estabelecimentoId);

    List<Despesa> findByEstabelecimentoIdAndDataHoraBetween(Long estabelecimentoId, LocalDateTime inicio, LocalDateTime fim);

}

