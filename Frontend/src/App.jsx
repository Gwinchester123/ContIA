import { lazy, Suspense, useEffect, useState } from "react";
import "./App.css";
import { BrowserRouter, Link, Navigate, Route, Routes } from "react-router-dom";

import Sidebar from "./components/Sidebar";
import AccountMenu from "./components/AccountMenu";
import PwaInstallFooter from "./components/PwaInstallFooter";
const Overview = lazy(() => import("./pages/Overview"));
const Vendas = lazy(() => import("./pages/Vendas"));
const Despesas = lazy(() => import("./pages/Despesas"));
const InteligenciaFinanceira = lazy(() => import("./pages/InteligenciaFinanceira"));
const Login = lazy(() => import("./pages/Login"));
const Cadastro = lazy(() => import("./pages/Cadastro"));
const CadastroEstabelecimento = lazy(() => import("./pages/CadastroEstabelecimento"));
const MeuPerfil = lazy(() => import("./pages/MeuPerfil"));
import { apiFetch, clearSessionUser, getSessionProfile, getSessionToken, getSessionName, setSessionUser } from "./data/financeStorage";
import { aplicarAtualizacaoPwa, assinarEstadoPwa, obterEstadoPwa } from "./pwa";
import { apiUrl } from "./data/api";
const Funcionarios = lazy(() => import("./pages/Funcionarios"));
const InicioFuncionario = lazy(() => import("./pages/InicioFuncionario"));

function DashboardLayout() {
  const [drawerAberto, setDrawerAberto] = useState(false);
  const [profile, setProfile] = useState(getSessionProfile);
  const [premium, setPremium] = useState(false);
  const [verificando, setVerificando] = useState(true);
  const [erroValidacao, setErroValidacao] = useState("");
  const isProprietario = profile === "PROPRIETARIO";

  useEffect(() => {
    let ativo = true;
    if (!getSessionToken()) {
      setVerificando(false);
      return undefined;
    }
    apiFetch(apiUrl("/auth/me"))
      .then(async (resposta) => {
        if (!resposta.ok) {
          if ([401, 403].includes(resposta.status)) clearSessionUser();
          throw new Error("Não foi possível validar a sessão.");
        }
        const usuario = await resposta.json();
        let temPremium = false;
        if (usuario.perfil === "PROPRIETARIO") {
          try {
            const respostaEstabelecimentos = await apiFetch(apiUrl(`/estabelecimentos/proprietario/${usuario.id}`));
            if (respostaEstabelecimentos.ok) {
              const estabelecimentos = await respostaEstabelecimentos.json();
              temPremium = estabelecimentos.some((estabelecimento) => estabelecimento.plano === "PREMIUM");
            }
          } catch { /* O backend continua protegendo cada recurso Premium. */ }
        }
        if (ativo) {
          setProfile(usuario.perfil || "");
          setPremium(temPremium);
          setErroValidacao("");
          setSessionUser({ ...usuario, nome: getSessionName(), token: getSessionToken() });
        }
      })
      .catch(() => {
        if (!ativo) return;
        if (getSessionToken()) {
          setProfile(getSessionProfile());
          setErroValidacao("Não foi possível validar sua sessão agora. Verifique sua conexão e tente novamente.");
        } else {
          setProfile("");
          setErroValidacao("");
        }
      })
      .finally(() => { if (ativo) setVerificando(false); });
    return () => { ativo = false; };
  }, []);

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

  if (verificando) return <main className="auth-page"><p>Verificando acesso...</p></main>;
  if (!getSessionToken() || !profile) return <Navigate to="/login" replace />;

  return (
    <div className={`container${drawerAberto ? " drawer-open" : ""}`}>
      <Sidebar profile={profile} premium={premium} onNavigate={() => setDrawerAberto(false)} />
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
          <Link className="mobile-brand" to="/dashboard" aria-label="Ir para a tela inicial">
            <img src="/logo-contia.png" alt="ContIA" />
          </Link>
        </div>

        <main className="main">
          <div className="dashboard-topbar"><AccountMenu profile={profile} /></div>
          {erroValidacao && <p className="auth-error" role="status">{erroValidacao}</p>}
          <Suspense fallback={<div className="route-loading" role="status">Carregando página...</div>}>
            <Routes>
              <Route index element={isProprietario ? <Overview /> : <InicioFuncionario />} />
              <Route path="vendas" element={<Vendas />} />
              {!isProprietario && <Route path="minhas-vendas" element={<Vendas somenteMinhas />} />}
              {isProprietario && <Route path="despesas" element={<Despesas />} />}
              {isProprietario && premium && <Route path="inteligencia-financeira" element={<InteligenciaFinanceira />} />}
              {isProprietario && <Route path="perfil" element={<MeuPerfil />} />}
              {isProprietario && <Route path="funcionarios" element={<Funcionarios />} />}
              <Route path="*" element={<Navigate to={isProprietario ? "/dashboard" : "/dashboard/vendas"} replace />} />
            </Routes>
          </Suspense>
        </main>
        <PwaInstallFooter />
      </div>
    </div>
  );
}

function AvisoAtualizacaoPwa() {
  const [pwa, setPwa] = useState(obterEstadoPwa);
  const [adiada, setAdiada] = useState(false);

  useEffect(() => assinarEstadoPwa((estado) => {
    setPwa(estado);
    if (estado.atualizacaoDisponivel) setAdiada(false);
  }), []);

  if (!pwa.atualizacaoDisponivel || adiada) return null;
  return (
    <aside className="pwa-update-banner" role="region" aria-label="Atualização do ContIA">
      <p aria-live="polite">Uma nova versão do ContIA está pronta. Atualize quando puder salvar suas alterações.</p>
      <div>
        <button type="button" className="pwa-update-dismiss" onClick={() => setAdiada(true)}>Depois</button>
        <button type="button" className="pwa-update-confirm" onClick={aplicarAtualizacaoPwa}>Atualizar</button>
      </div>
    </aside>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AvisoAtualizacaoPwa />
      <Suspense fallback={<main className="auth-page"><p>Carregando página...</p></main>}>
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
      </Suspense>
    </BrowserRouter>
  );
}

export default App;
