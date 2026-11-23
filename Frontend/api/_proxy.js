import { Readable } from "node:stream";
import { pipeline } from "node:stream/promises";

const hopByHopHeaders = new Set([
  "connection",
  "keep-alive",
  "proxy-authenticate",
  "proxy-authorization",
  "te",
  "trailer",
  "transfer-encoding",
  "upgrade",
]);

function sendJson(res, status, error) {
  res.statusCode = status;
  res.setHeader("Content-Type", "application/json; charset=utf-8");
  res.end(JSON.stringify({ error }));
}

function getBackendOrigin(value) {
  const url = new URL(value);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    url.pathname !== "/" ||
    url.search ||
    url.hash
  ) {
    throw new Error("BACKEND_URL must be an HTTP(S) origin without a path.");
  }
  return url.origin;
}

function getRequestBody(req) {
  if (req.body !== undefined) {
    if (
      typeof req.body === "string" ||
      Buffer.isBuffer(req.body) ||
      req.body instanceof Uint8Array
    ) {
      return req.body;
    }
    return Buffer.from(JSON.stringify(req.body));
  }
  return Readable.toWeb(req);
}

export function getProxyTargetUrl(requestUrl, backendOrigin) {
  const incomingUrl = new URL(requestUrl, backendOrigin);
  const proxyPath = incomingUrl.searchParams.get("_proxyPath");
  if (proxyPath === null) return null;

  const encodedPath = proxyPath
    .split("/")
    .map((segment) => encodeURIComponent(segment))
    .join("/");
  const targetUrl = new URL(`/api/${encodedPath}`, backendOrigin);
  const isApiPath =
    targetUrl.pathname === "/api" || targetUrl.pathname.startsWith("/api/");
  if (targetUrl.origin !== backendOrigin || !isApiPath) return null;

  for (const [name, value] of incomingUrl.searchParams) {
    if (name !== "_proxyPath") targetUrl.searchParams.append(name, value);
  }
  return targetUrl;
}

export default async function handler(req, res) {
  const backendUrl = process.env.BACKEND_URL?.trim();
  if (!backendUrl) {
    sendJson(res, 503, "Backend proxy is not configured. Set BACKEND_URL.");
    return;
  }

  let backendOrigin;
  try {
    backendOrigin = getBackendOrigin(backendUrl);
  } catch (error) {
    console.error("Invalid BACKEND_URL configuration:", error);
    sendJson(res, 500, "BACKEND_URL must be an HTTP(S) origin without a path.");
    return;
  }

  const method = (req.method || "GET").toUpperCase();
  const targetUrl = getProxyTargetUrl(
    req.url || "/api/_proxy",
    backendOrigin,
  );
  if (!targetUrl) {
    sendJson(res, 404, "Not found.");
    return;
  }

  const headers = new Headers();
  for (const [name, value] of Object.entries(req.headers)) {
    if (value !== undefined) {
      headers.set(name, Array.isArray(value) ? value.join(", ") : value);
    }
  }
  const connectionHeaders = headers.get("connection")?.split(",") || [];
  for (const name of connectionHeaders) {
    headers.delete(name.trim());
  }
  for (const name of hopByHopHeaders) {
    headers.delete(name);
  }
  headers.delete("host");
  headers.delete("origin");
  headers.delete("content-length");

  const hasBody = method !== "GET" && method !== "HEAD";
  const body = hasBody ? getRequestBody(req) : undefined;

  try {
    const upstream = await fetch(targetUrl, {
      method,
      headers,
      body,
      ...(body instanceof ReadableStream ? { duplex: "half" } : {}),
      redirect: "manual",
    });

    const responseHeaders = {};
    upstream.headers.forEach((value, name) => {
      if (
        !hopByHopHeaders.has(name.toLowerCase()) &&
        name !== "content-length" &&
        name !== "content-encoding" &&
        name !== "set-cookie"
      ) {
        responseHeaders[name] = value;
      }
    });
    const cookies = upstream.headers.getSetCookie();
    if (cookies.length) {
      responseHeaders["set-cookie"] = cookies;
    }

    res.writeHead(upstream.status, responseHeaders);
    if (method === "HEAD" || !upstream.body) {
      res.end();
      return;
    }
    await pipeline(Readable.fromWeb(upstream.body), res);
  } catch (error) {
    console.error("API proxy request failed:", error);
    if (res.headersSent) {
      res.destroy(error);
      return;
    }
    sendJson(res, 502, "The backend API is unavailable.");
  }
}
