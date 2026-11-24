import assert from "node:assert/strict";
import test from "node:test";
import { getProxyTargetUrl } from "../../api/proxy.js";

test("rewrites nested API paths and preserves query parameters", () => {
  const target = getProxyTargetUrl(
    "/api/proxy?_proxyPath=funcionarios%2Flogin&source=web",
    "https://backend.example.com",
  );

  assert.equal(
    target.href,
    "https://backend.example.com/api/funcionarios/login?source=web",
  );
});

test("rejects requests without a proxy path or attempting to leave /api", () => {
  assert.equal(
    getProxyTargetUrl("/api/proxy", "https://backend.example.com"),
    null,
  );
  assert.equal(
    getProxyTargetUrl(
      "/api/proxy?_proxyPath=..%2Fadmin",
      "https://backend.example.com",
    ),
    null,
  );
});
