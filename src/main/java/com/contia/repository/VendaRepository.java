
package com.contia.repository;

import com.contia.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    List<Venda> findByEstabelecimentoId(Long estabelecimentoId);

}

