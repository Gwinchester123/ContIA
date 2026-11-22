import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router-dom";
import { clearSessionUser, getSessionName } from "../data/financeStorage";

export default function AccountMenu({ profile }) {
  const [aberto, setAberto] = useState(false);
  const menuRef = useRef(null);
  const navigate = useNavigate();
  const nome = getSessionName();
  const iniciais = nome.split(/\s+/).filter(Boolean).slice(0, 2).map((parte) => parte[0]).join("").toUpperCase() || "C";

  useEffect(() => {
    const fechar = (event) => { if (!menuRef.current?.contains(event.target)) setAberto(false); };
    const fecharEscape = (event) => { if (event.key === "Escape") setAberto(false); };
    document.addEventListener("pointerdown", fechar);
    document.addEventListener("keydown", fecharEscape);
    return () => {
      document.removeEventListener("pointerdown", fechar);
      document.removeEventListener("keydown", fecharEscape);
    };
  }, []);

  const sair = () => {
    clearSessionUser();
    navigate("/login", { replace: true });
  };

  return (
    <div className="account-menu" ref={menuRef}>
      <button className="account-menu-trigger" type="button" aria-haspopup="menu" aria-expanded={aberto} onClick={() => setAberto((atual) => !atual)}>
        <span className="account-avatar" aria-hidden="true">{iniciais}</span>
        <span className="account-user-name">{nome}</span>
        <svg className="account-chevron" viewBox="0 0 20 20" aria-hidden="true"><path d="m5 7.5 5 5 5-5" /></svg>
      </button>
      {aberto && <div className="account-menu-popover" role="menu">
        {profile === "PROPRIETARIO" && <button type="button" role="menuitem" onClick={() => { setAberto(false); navigate("/dashboard/perfil"); }}>Minha conta</button>}
        <button type="button" role="menuitem" onClick={sair}>Sair</button>
      </div>}
    </div>
  );
}
