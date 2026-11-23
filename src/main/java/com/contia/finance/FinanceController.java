package com.contia.finance;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.contia.account.Account;
import com.contia.account.AccountRepository;

@RestController
@RequestMapping("/api/finance")
public class FinanceController {
	private final AccountRepository accounts;
	private final SaleRepository sales;
	private final ExpenseRepository expenses;

	public FinanceController(AccountRepository accounts, SaleRepository sales, ExpenseRepository expenses) {
		this.accounts = accounts;
		this.sales = sales;
		this.expenses = expenses;
	}

	@GetMapping("/sales")
	List<SaleResponse> getSales(@org.springframework.security.core.annotation.AuthenticationPrincipal String accountId) {
		return sales.findAllByAccount_IdOrderByCreatedAtDesc(UUID.fromString(accountId)).stream()
				.map(FinanceController::saleResponse).toList();
	}

	@PostMapping("/sales")
	@Transactional
	SaleResponse addSale(@org.springframework.security.core.annotation.AuthenticationPrincipal String accountId,
			@RequestBody SaleRequest request) {
		Account account = account(accountId);
		validateSale(request);
		BigDecimal total = request.unitValue().multiply(BigDecimal.valueOf(request.quantity()))
				.setScale(2, RoundingMode.HALF_UP);
		Sale sale = sales.save(new Sale(account, request.product().trim(), request.quantity(),
				request.unitValue().setScale(2, RoundingMode.HALF_UP), total,
				request.data() == null ? Instant.now() : parseInstant(request.data())));
		return saleResponse(sale);
	}

	@DeleteMapping("/sales/{id}")
	@Transactional
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteSale(@org.springframework.security.core.annotation.AuthenticationPrincipal String accountId,
			@PathVariable UUID id) {
		if (!sales.existsByIdAndAccount_Id(id, UUID.fromString(accountId))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venda não encontrada.");
		}
		sales.deleteById(id);
	}

	@GetMapping("/expenses")
	List<ExpenseResponse> getExpenses(
			@org.springframework.security.core.annotation.AuthenticationPrincipal String accountId) {
		return expenses.findAllByAccount_IdOrderByCreatedAtDesc(UUID.fromString(accountId)).stream()
				.map(FinanceController::expenseResponse).toList();
	}

	@PostMapping("/expenses")
	@Transactional
	ExpenseResponse addExpense(@org.springframework.security.core.annotation.AuthenticationPrincipal String accountId,
			@RequestBody ExpenseRequest request) {
		Account account = account(accountId);
		validateExpense(request);
		Expense expense = expenses.save(new Expense(account, request.descricao().trim(), request.categoria().trim(),
				request.valor().setScale(2, RoundingMode.HALF_UP), parseDate(request.data())));
		return expenseResponse(expense);
	}

	@DeleteMapping("/expenses/{id}")
	@Transactional
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteExpense(@org.springframework.security.core.annotation.AuthenticationPrincipal String accountId,
			@PathVariable UUID id) {
		if (!expenses.existsByIdAndAccount_Id(id, UUID.fromString(accountId))) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Despesa não encontrada.");
		}
		expenses.deleteById(id);
	}

	@PostMapping("/legacy-import")
	@Transactional
	ImportResponse importLegacyData(
			@org.springframework.security.core.annotation.AuthenticationPrincipal String accountId,
			@RequestBody LegacyImportRequest request) {
		Account account = accounts.findByIdForUpdate(UUID.fromString(accountId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
		if (account.isLegacyImported()) {
			return new ImportResponse(true, 0, 0);
		}

		List<LegacySale> legacySales = request.vendas() == null ? List.of() : request.vendas();
		List<LegacyExpense> legacyExpenses = request.despesas() == null ? List.of() : request.despesas();
		List<Sale> importedSales = legacySales.stream().map(sale -> {
			validateLegacySale(sale);
			BigDecimal total = sale.total() == null
					? sale.valorUnitario().multiply(BigDecimal.valueOf(sale.quantidade()))
					: sale.total();
			return new Sale(account, sale.produto().trim(), sale.quantidade(),
					sale.valorUnitario().setScale(2, RoundingMode.HALF_UP),
					total.setScale(2, RoundingMode.HALF_UP),
					sale.data() == null ? Instant.now() : parseInstant(sale.data()),
					sale.createdAt() == null ? Instant.now() : parseInstant(sale.createdAt()));
		}).toList();
		List<Expense> importedExpenses = legacyExpenses.stream().map(expense -> {
			validateLegacyExpense(expense);
			return new Expense(account, expense.descricao().trim(), expense.categoria().trim(),
					expense.valor().setScale(2, RoundingMode.HALF_UP), parseDate(expense.data()),
					expense.createdAt() == null ? Instant.now() : parseInstant(expense.createdAt()));
		}).toList();

		sales.saveAll(importedSales);
		expenses.saveAll(importedExpenses);
		account.setLegacyImported(true);
		accounts.save(account);
		return new ImportResponse(false, importedSales.size(), importedExpenses.size());
	}

	private Account account(String accountId) {
		return accounts.findById(UUID.fromString(accountId))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessão inválida."));
	}

	private static void validateSale(SaleRequest request) {
		if (request.product() == null || request.product().isBlank() || request.product().length() > 200
				|| request.quantity() <= 0 || !validAmount(request.unitValue())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da venda inválidos.");
		}
	}

	private static void validateExpense(ExpenseRequest request) {
		if (request.descricao() == null || request.descricao().isBlank() || request.descricao().length() > 200
				|| request.categoria() == null || request.categoria().isBlank() || request.categoria().length() > 100
				|| !validAmount(request.valor()) || request.data() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados da despesa inválidos.");
		}
	}

	private static void validateLegacySale(LegacySale sale) {
		if (sale == null || sale.produto() == null || sale.produto().isBlank() || sale.produto().length() > 200
				|| sale.quantidade() <= 0 || !validAmount(sale.valorUnitario())
				|| (sale.total() != null && !validAmount(sale.total()))) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registro local de venda inválido.");
		}
	}

	private static void validateLegacyExpense(LegacyExpense expense) {
		if (expense == null || expense.descricao() == null || expense.descricao().isBlank() || expense.descricao().length() > 200
				|| expense.categoria() == null || expense.categoria().isBlank() || expense.categoria().length() > 100
				|| !validAmount(expense.valor()) || expense.data() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registro local de despesa inválido.");
		}
	}

	private static boolean validAmount(BigDecimal amount) {
		return amount != null && amount.signum() > 0 && amount.precision() <= 12 && amount.scale() <= 2;
	}

	private static Instant parseInstant(String value) {
		try {
			return Instant.parse(value);
		} catch (RuntimeException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data da venda inválida.");
		}
	}

	private static LocalDate parseDate(String value) {
		try {
			return LocalDate.parse(value);
		} catch (RuntimeException exception) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Data da despesa inválida.");
		}
	}

	private static SaleResponse saleResponse(Sale sale) {
		return new SaleResponse(sale.getId().toString(), sale.getProduct(), sale.getQuantity(), sale.getUnitValue(),
				sale.getTotal(), sale.getSaleDate(), sale.getCreatedAt());
	}

	private static ExpenseResponse expenseResponse(Expense expense) {
		return new ExpenseResponse(expense.getId().toString(), expense.getDescription(), expense.getCategory(),
				expense.getAmount(), expense.getExpenseDate(), expense.getCreatedAt());
	}

	public record SaleRequest(String produto, int quantidade, BigDecimal valorUnitario, String data) {
	}

	public record ExpenseRequest(String descricao, String categoria, BigDecimal valor, String data) {
	}

	public record LegacySale(String produto, int quantidade, BigDecimal valorUnitario, BigDecimal total, String data,
			String createdAt) {
	}

	public record LegacyExpense(String descricao, String categoria, BigDecimal valor, String data, String createdAt) {
	}

	public record LegacyImportRequest(List<LegacySale> vendas, List<LegacyExpense> despesas) {
	}

	public record ImportResponse(boolean alreadyImported, int importedSales, int importedExpenses) {
	}

	public record SaleResponse(String id, String produto, int quantidade, BigDecimal valorUnitario, BigDecimal total,
			Instant data, Instant createdAt) {
	}

	public record ExpenseResponse(String id, String descricao, String categoria, BigDecimal valor, LocalDate data,
			Instant createdAt) {
	}
}
