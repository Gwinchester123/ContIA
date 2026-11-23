package com.contia.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
@Table(name = "expenses")
public class Expense {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "account_id", nullable = false)
	private Account account;

	@Column(nullable = false)
	private String description;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false)
	private LocalDate expenseDate;

	@Column(nullable = false)
	private Instant createdAt;

	protected Expense() {
	}

	public Expense(Account account, String description, String category, BigDecimal amount, LocalDate expenseDate) {
		this(account, description, category, amount, expenseDate, Instant.now());
	}

	public Expense(Account account, String description, String category, BigDecimal amount, LocalDate expenseDate,
			Instant createdAt) {
		this.account = account;
		this.description = description;
		this.category = category;
		this.amount = amount;
		this.expenseDate = expenseDate;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public String getDescription() {
		return description;
	}

	public String getCategory() {
		return category;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public LocalDate getExpenseDate() {
		return expenseDate;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
