const configuredApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim();

export const API_BASE_URL = (
  import.meta.env.PROD
    ? "/api"
    : configuredApiBaseUrl || "http://localhost:8080/api"
).replace(/\/+$/, "");

export const apiUrl = (path = "") =>
  `${API_BASE_URL}/${String(path).replace(/^\/+/, "")}`;
