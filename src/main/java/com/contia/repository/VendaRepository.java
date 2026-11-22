
package com.contia.repository;

import com.contia.model.Venda;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.contia.model.VendaItem;
import java.util.List;
import java.time.LocalDateTime;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    @EntityGraph(attributePaths = { "itens", "funcionarioResponsavel", "proprietarioResponsavel" })
    List<Venda> findByEstabelecimentoId(Long estabelecimentoId);

    @EntityGraph(attributePaths = { "itens", "funcionarioResponsavel", "proprietarioResponsavel" })
    List<Venda> findByEstabelecimentoProprietarioIdOrderByDataHoraDesc(Long proprietarioId);

    @EntityGraph(attributePaths = { "itens", "funcionarioResponsavel", "proprietarioResponsavel" })
    List<Venda> findByEstabelecimentoIdAndFuncionarioResponsavelId(Long estabelecimentoId, Long funcionarioId);

    @EntityGraph(attributePaths = { "itens", "funcionarioResponsavel", "proprietarioResponsavel" })
    List<Venda> findByEstabelecimentoIdAndDataHoraBetween(Long estabelecimentoId, LocalDateTime inicio, LocalDateTime fim);

    @Query("select i from VendaItem i join fetch i.venda v "
            + "where v.estabelecimento.id = :estabelecimentoId "
            + "order by v.dataHora desc, v.id desc, i.ordem desc")
    List<VendaItem> buscarHistoricoItens(@Param("estabelecimentoId") Long estabelecimentoId);

}

