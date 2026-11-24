import assert from "node:assert/strict";
import test from "node:test";
import {
  clearSessionUser,
  getSessionProfile,
  getSessionToken,
  setSessionUser,
} from "./financeStorage.js";

class MemoryStorage {
  values = new Map();

  getItem(key) {
    return this.values.get(key) ?? null;
  }

  setItem(key, value) {
    this.values.set(key, String(value));
  }

  removeItem(key) {
    this.values.delete(key);
  }
}

const withStorage = (run) => {
  const localStorageAnterior = globalThis.localStorage;
  const sessionStorageAnterior = globalThis.sessionStorage;
  globalThis.localStorage = new MemoryStorage();
  globalThis.sessionStorage = new MemoryStorage();
  try {
    run(globalThis.localStorage, globalThis.sessionStorage);
  } finally {
    globalThis.localStorage = localStorageAnterior;
    globalThis.sessionStorage = sessionStorageAnterior;
  }
};

test("keeps authenticated session in persistent storage", () => {
  withStorage((localStorage, sessionStorage) => {
    setSessionUser({ id: 42, nome: "Ana", token: "token-123", perfil: "PROPRIETARIO" });
    sessionStorage.values.clear();

    assert.equal(localStorage.getItem("contia-session-token"), "token-123");
    assert.equal(getSessionToken(), "token-123");
    assert.equal(getSessionProfile(), "PROPRIETARIO");
  });
});

test("migrates an existing session from session storage", () => {
  withStorage((localStorage, sessionStorage) => {
    sessionStorage.setItem("contia-session-token", "legacy-token");

    assert.equal(getSessionToken(), "legacy-token");
    assert.equal(localStorage.getItem("contia-session-token"), "legacy-token");
    assert.equal(sessionStorage.getItem("contia-session-token"), null);
  });
});

test("logout removes persistent and legacy session values", () => {
  withStorage((localStorage, sessionStorage) => {
    localStorage.setItem("contia-session-token", "current-token");
    sessionStorage.setItem("contia-session-token", "legacy-token");

    clearSessionUser();

    assert.equal(localStorage.getItem("contia-session-token"), null);
    assert.equal(sessionStorage.getItem("contia-session-token"), null);
  });
});
