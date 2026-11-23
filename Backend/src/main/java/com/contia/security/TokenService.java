package com.contia.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
	private static final long TOKEN_LIFETIME_MILLIS = 86_400_000L;
	private final byte[] secret;

	public TokenService(@Value("${contia.jwt.secret}") String secret) {
		this.secret = secret.getBytes(StandardCharsets.UTF_8);
		if (this.secret.length < 32) {
			throw new IllegalStateException("CONTIA_JWT_SECRET must contain at least 32 characters.");
		}
	}

	public String create(UUID accountId) {
		String payload = accountId + ":" + (System.currentTimeMillis() + TOKEN_LIFETIME_MILLIS);
		String encodedPayload = Base64.getUrlEncoder().withoutPadding()
				.encodeToString(payload.getBytes(StandardCharsets.UTF_8));
		return encodedPayload + "." + Base64.getUrlEncoder().withoutPadding()
				.encodeToString(sign(encodedPayload));
	}

	public UUID verify(String token) {
		String[] parts = token.split("\\.", -1);
		if (parts.length != 2 || !MessageDigest.isEqual(sign(parts[0]), decode(parts[1]))) {
			throw new IllegalArgumentException("Invalid bearer token.");
		}

		String payload = new String(decode(parts[0]), StandardCharsets.UTF_8);
		int separator = payload.lastIndexOf(':');
		if (separator < 0 || Long.parseLong(payload.substring(separator + 1)) < System.currentTimeMillis()) {
			throw new IllegalArgumentException("Expired bearer token.");
		}
		return UUID.fromString(payload.substring(0, separator));
	}

	private byte[] sign(String value) {
		try {
			Mac mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(secret, "HmacSHA256"));
			return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
		} catch (java.security.GeneralSecurityException exception) {
			throw new IllegalStateException("Could not sign bearer token.", exception);
		}
	}

	private byte[] decode(String value) {
		try {
			return Base64.getUrlDecoder().decode(value);
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException("Invalid bearer token.", exception);
		}
	}
}
