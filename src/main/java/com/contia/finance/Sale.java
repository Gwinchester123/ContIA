package com.contia.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.contia.account.Account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "sales")
public class Sale {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(nullable = false)
	private String product;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal unitValue;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal total;

	@Column(nullable = false)
	private Instant saleDate;

	@Column(nullable = false)
	private Instant createdAt;

	protected Sale() {
	}

	public Sale(Account account, String product, int quantity, BigDecimal unitValue, BigDecimal total, Instant saleDate) {
		this(account, product, quantity, unitValue, total, saleDate, Instant.now());
	}

	public Sale(Account account, String product, int quantity, BigDecimal unitValue, BigDecimal total, Instant saleDate,
			Instant createdAt) {
		this.account = account;
		this.product = product;
		this.quantity = quantity;
		this.unitValue = unitValue;
		this.total = total;
		this.saleDate = saleDate;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getProduct() {
		return product;
	}

	public int getQuantity() {
		return quantity;
	}

	public BigDecimal getUnitValue() {
		return unitValue;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public Instant getSaleDate() {
		return saleDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
