package com.contia.account;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.contia.security.TokenService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
	private final AccountRepository accounts;
	private final PasswordEncoder passwordEncoder;
	private final TokenService tokenService;

	public AuthController(AccountRepository accounts, PasswordEncoder passwordEncoder, TokenService tokenService) {
		this.accounts = accounts;
		this.passwordEncoder = passwordEncoder;
		this.tokenService = tokenService;
	}

	@PostMapping("/register")
	AuthResponse register(@RequestBody RegisterRequest request) {
		String name = clean(request.name());
		String email = normalizeEmail(request.email());
		String password = request.password() == null ? "" : request.password();
		if (name.isEmpty() || email.isEmpty() || password.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome, e-mail e senha são obrigatórios.");
		}
		if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter até 72 bytes UTF-8.");
		}
		if (name.length() > 100 || email.length() > 254 || !email.contains("@")) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome ou e-mail inválido.");
		}
		if (accounts.findByEmail(email).isPresent()) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
		}

		Account account;
		try {
			account = accounts.save(new Account(name, email, passwordEncoder.encode(password)));
		} catch (DataIntegrityViolationException exception) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
		}
		return response(account);
	}

	@PostMapping("/login")
	AuthResponse login(@RequestBody LoginRequest request) {
		String email = normalizeEmail(request.email());
		String password = request.password() == null ? "" : request.password();
		Account account = accounts.findByEmail(email)
				.filter(existing -> passwordEncoder.matches(password, existing.getPasswordHash()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos."));
		return response(account);
	}

	private AuthResponse response(Account account) {
		return new AuthResponse(tokenService.create(account.getId()), account.getId().toString(),
				account.getName(), account.getEmail());
	}

	private static String normalizeEmail(String email) {
		return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
	}

	private static String clean(String value) {
		return value == null ? "" : value.trim();
	}

	public record RegisterRequest(String name, String email, String password) {
	}

	public record LoginRequest(String email, String password) {
	}

	public record AuthResponse(String token, String id, String name, String email) {
	}
}
