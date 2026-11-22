import { useEffect, useState } from "react";
import { assinarEstadoPwa, obterEstadoPwa, solicitarInstalacao } from "../pwa";

export default function PwaInstallFooter() {
  const [pwa, setPwa] = useState(obterEstadoPwa);

  useEffect(() => assinarEstadoPwa(setPwa), []);

  if (!pwa.podeInstalar || pwa.instalado) return null;

  return (
    <footer className="pwa-install-footer">
      <button className="pwa-install-button" type="button" onClick={solicitarInstalacao}>
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 3v11m0 0 4-4m-4 4-4-4M5 16v4h14v-4" />
        </svg>
        <span>Instalar ContIA</span>
      </button>
    </footer>
  );
}
