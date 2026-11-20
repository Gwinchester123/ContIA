
package com.contia.repository;

import com.contia.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDateTime;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    List<Venda> findByEstabelecimentoId(Long estabelecimentoId);

    List<Venda> findByEstabelecimentoIdAndDataHoraBetween(Long estabelecimentoId, LocalDateTime inicio, LocalDateTime fim);

}

