import { useEffect, useState } from "react";
import "./App.css";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import Sidebar from "./components/Sidebar";
import Overview from "./pages/Overview";
import Vendas from "./pages/Vendas";
import Despesas from "./pages/Despesas";
import Relatorios from "./pages/Relatorios";
import InteligenciaFinanceira from "./pages/InteligenciaFinanceira";
import Login from "./pages/Login";
import Cadastro from "./pages/Cadastro";
import CadastroEstabelecimento from "./pages/CadastroEstabelecimento";
import MeuPerfil from "./pages/MeuPerfil";

function DashboardLayout() {
  const [drawerAberto, setDrawerAberto] = useState(false);

  useEffect(() => {
    if (!drawerAberto) return undefined;

    const fecharComEscape = (event) => {
      if (event.key === "Escape") setDrawerAberto(false);
    };
    const fecharNoDesktop = () => {
      if (window.innerWidth > 1024) setDrawerAberto(false);
    };

    document.addEventListener("keydown", fecharComEscape);
    window.addEventListener("resize", fecharNoDesktop);
    return () => {
      document.removeEventListener("keydown", fecharComEscape);
      window.removeEventListener("resize", fecharNoDesktop);
    };
  }, [drawerAberto]);

  return (
    <div className={`container${drawerAberto ? " drawer-open" : ""}`}>
      <Sidebar onNavigate={() => setDrawerAberto(false)} />
      <button
        className="drawer-backdrop"
        type="button"
        aria-label="Fechar menu de navegação"
        tabIndex={drawerAberto ? 0 : -1}
        onClick={() => setDrawerAberto(false)}
      />

      <div className="dashboard-shell">
        <div className="mobile-toolbar">
          <button
            className="menu-toggle"
            type="button"
            aria-controls="dashboard-navigation"
            aria-expanded={drawerAberto}
            aria-label={drawerAberto ? "Fechar menu" : "Abrir menu"}
            onClick={() => setDrawerAberto((aberto) => !aberto)}
          >
            <svg viewBox="0 0 24 24" aria-hidden="true">
              {drawerAberto ? (
                <path d="m6 6 12 12M18 6 6 18" />
              ) : (
                <path d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
          <img src="/logo-contia.png" alt="ContIA" />
        </div>

        <main className="main">
          <Routes>
            <Route index element={<Overview />} />
            <Route path="vendas" element={<Vendas />} />
            <Route path="despesas" element={<Despesas />} />
            <Route path="relatorios" element={<Relatorios />} />
            <Route path="inteligencia-financeira" element={<InteligenciaFinanceira />} />
            <Route path="perfil" element={<MeuPerfil />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route
          path="/cadastro-estabelecimento"
          element={<CadastroEstabelecimento />}
        />
        <Route path="/dashboard/*" element={<DashboardLayout />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
