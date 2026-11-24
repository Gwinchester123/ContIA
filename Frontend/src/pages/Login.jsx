import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { apiFetch, clearSessionUser, setSessionUser } from "../data/financeStorage";
import { API_BASE_URL } from "../data/api";
import PasswordVisibilityButton from "../components/PasswordVisibilityButton";

const API = API_BASE_URL;

export default function Login() {
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState("");
  const [senha, setSenha] = useState("");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);
  const [mostrarSenha, setMostrarSenha] = useState(false);
  const mensagem = location.state?.mensagem || "";

  const entrar = async (event) => {
    event.preventDefault();
    setErro("");
    setCarregando(true);
    try {
      let resposta = await fetch(`${API}/proprietarios/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, senha }),
      });
      if (!resposta.ok && [400, 401, 403, 404].includes(resposta.status)) {
        resposta = await fetch(`${API}/funcionarios/login`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email, senha }),
        });
      }
      if (!resposta.ok) throw new Error((await resposta.text()) || "E-mail ou senha inválidos.");
      const account = await resposta.json();
      setSessionUser(account);
      const respostaUsuario = await apiFetch(`${API}/auth/me`);
      if (!respostaUsuario.ok) throw new Error("Não foi possível confirmar o perfil de acesso.");
      const usuario = await respostaUsuario.json();
      setSessionUser({ ...account, ...usuario, token: account.token });
      navigate(usuario.perfil === "FUNCIONARIO" ? "/dashboard/vendas" : "/dashboard");
    } catch (error) {
      clearSessionUser();
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setCarregando(false);
    }
  };

  return (
    <main className="auth-page login-page">
      <section className="login-form-side">
        <form className="auth-card login-card" onSubmit={entrar}>
          <div className="brand auth-brand"><img src="/logo-contia.png" alt="ContIA" /></div>
          <h1>Bem-vindo de volta</h1>
          <p>Acesse o financeiro do seu negócio.</p>
          <label>E-mail ou usuário<input type="text" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="voce@email.com" required /></label>
          <label>Senha<div className="login-password-wrap"><input type={mostrarSenha ? "text" : "password"} autoComplete="current-password" value={senha} onChange={(event) => setSenha(event.target.value)} placeholder="Sua senha" required /><PasswordVisibilityButton visible={mostrarSenha} onToggle={() => setMostrarSenha((atual) => !atual)} /></div></label>
          {erro && <p className="auth-error" role="alert">{erro}</p>}
          {mensagem && <p className="profile-success" role="status">{mensagem}</p>}
          <button className="primary-button" type="submit" disabled={carregando}>{carregando ? "Entrando..." : "Entrar"}</button>
          <div className="auth-links"><span>Não tem uma conta?</span>{" "}<Link to="/cadastro">Cadastre-se</Link></div>
        </form>
      </section>
      <aside className="login-presentation">
        <div className="login-presentation-content">
          <span className="login-presentation-mark">CONTROLE FINANCEIRO SIMPLES</span>
          <h2>Mais clareza para cuidar do seu negócio.</h2>
          <p>Organize vendas e despesas, acompanhe seus resultados e tome decisões com mais segurança.</p>
          <div className="login-presentation-decoration" aria-hidden="true"><span /><span /><span /></div>
        </div>
      </aside>
    </main>
  );
}
