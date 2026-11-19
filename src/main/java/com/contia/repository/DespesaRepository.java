
package com.contia.repository;

import com.contia.model.Despesa;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DespesaRepository extends JpaRepository<Despesa, Long> {

    List<Despesa> findByEstabelecimentoId(Long estabelecimentoId);

}

