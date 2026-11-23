const APP_VERSION = "__APP_VERSION__";
const CACHE_PREFIX = "contia-";
const SHELL_CACHE = `${CACHE_PREFIX}shell-${APP_VERSION}`;
const ASSET_CACHE = `${CACHE_PREFIX}static-${APP_VERSION}`;
const SHELL_URLS = [
  "/",
  "/index.html",
  "/manifest.webmanifest",
  "/logo-contia.png",
  "/favicon-contia.png",
  "/pwa-icons/contia-180.png",
  "/pwa-icons/contia-192.png",
  "/pwa-icons/contia-512.png",
  "/pwa-icons/contia-maskable-512.png",
];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(SHELL_CACHE).then((cache) => cache.addAll(SHELL_URLS)));
});

self.addEventListener("activate", (event) => {
  event.waitUntil(caches.keys()
    .then((keys) => Promise.all(keys
      .filter((key) => key.startsWith(CACHE_PREFIX) && key !== SHELL_CACHE && key !== ASSET_CACHE)
      .map((key) => caches.delete(key))))
    .then(() => self.clients.claim()));
});

self.addEventListener("message", (event) => {
  if (event.data === "CONTIA_SKIP_WAITING") self.skipWaiting();
});

self.addEventListener("fetch", (event) => {
  const request = event.request;
  const url = new URL(request.url);

  if (request.method !== "GET" || url.origin !== self.location.origin) return;
  if (url.pathname === "/api" || url.pathname.startsWith("/api/")) return;
  if (request.headers.has("Authorization")) return;

  if (request.mode === "navigate") {
    event.respondWith(fetch(request).catch(async () =>
      (await caches.match("/index.html")) || Response.error()));
    return;
  }

  const recursoEstatico = ["script", "style", "image", "font", "manifest"].includes(request.destination)
    || url.pathname.startsWith("/assets/")
    || url.pathname.startsWith("/pwa-icons/");
  if (!recursoEstatico) return;

  event.respondWith(caches.open(ASSET_CACHE).then(async (cache) => {
    const salvo = await cache.match(request);
    if (salvo) return salvo;
    const resposta = await fetch(request);
    if (resposta.ok) await cache.put(request, resposta.clone());
    return resposta;
  }));
});
