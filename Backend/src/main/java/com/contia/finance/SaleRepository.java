package com.contia.finance;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleRepository extends JpaRepository<Sale, UUID> {
	List<Sale> findAllByAccount_IdOrderByCreatedAtDesc(UUID accountId);
	boolean existsByIdAndAccount_Id(UUID id, UUID accountId);
}
