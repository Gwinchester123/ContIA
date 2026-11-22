let promptInstalacao = null;
let workerAguardando = null;
let atualizacaoSolicitada = false;
const assinantes = new Set();

const avisarMudanca = () => assinantes.forEach((assinante) => assinante(obterEstadoPwa()));

const estaInstalado = () => Boolean(
  window.matchMedia?.("(display-mode: standalone)").matches
  || window.navigator.standalone === true,
);

const ehIos = () => /iphone|ipad|ipod/i.test(window.navigator.userAgent)
  || (window.navigator.platform === "MacIntel" && window.navigator.maxTouchPoints > 1);

export function obterEstadoPwa() {
  const ios = ehIos();
  return {
    instalado: estaInstalado(),
    podeInstalar: Boolean(promptInstalacao) && !estaInstalado(),
    ajudaIos: ios && !estaInstalado(),
    atualizacaoDisponivel: Boolean(workerAguardando),
  };
}

export function assinarEstadoPwa(assinante) {
  assinantes.add(assinante);
  return () => assinantes.delete(assinante);
}

window.addEventListener("beforeinstallprompt", (event) => {
  event.preventDefault();
  promptInstalacao = event;
  avisarMudanca();
});

window.addEventListener("appinstalled", () => {
  promptInstalacao = null;
  avisarMudanca();
});

export async function solicitarInstalacao() {
  if (!promptInstalacao) return false;
  const prompt = promptInstalacao;
  promptInstalacao = null;
  avisarMudanca();
  await prompt.prompt();
  const escolha = await prompt.userChoice;
  if (escolha.outcome !== "accepted" && !estaInstalado()) {
    promptInstalacao = null;
  }
  avisarMudanca();
  return escolha.outcome === "accepted";
}

export function aplicarAtualizacaoPwa() {
  if (!workerAguardando) return;
  atualizacaoSolicitada = true;
  workerAguardando.postMessage("CONTIA_SKIP_WAITING");
}

export function registrarServiceWorker() {
  if (!import.meta.env.PROD || !("serviceWorker" in navigator)) return;

  navigator.serviceWorker.addEventListener("controllerchange", () => {
    if (atualizacaoSolicitada) window.location.reload();
  });

  navigator.serviceWorker.register("/sw.js", { scope: "/" })
    .then((registration) => {
      const conferirWorkerAguardando = () => {
        if (registration.waiting && navigator.serviceWorker.controller) {
          workerAguardando = registration.waiting;
          avisarMudanca();
        }
      };

      registration.addEventListener("updatefound", () => {
        const worker = registration.installing;
        worker?.addEventListener("statechange", () => {
          if (worker.state === "installed") conferirWorkerAguardando();
        });
      });

      conferirWorkerAguardando();
    })
    .catch((erro) => console.error("Não foi possível registrar o ContIA como PWA:", erro));
}
