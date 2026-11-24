import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch, clearSessionUser, getSessionUser } from "../data/financeStorage";
import { API_BASE_URL } from "../data/api";

const API = API_BASE_URL;

export default function MeuPerfil() {
  const navigate = useNavigate();
  const proprietarioId = getSessionUser();
  const [estabelecimento, setEstabelecimento] = useState(null);
  const [nomeEstabelecimento, setNomeEstabelecimento] = useState("");
  const [proprietario, setProprietario] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [mensagem, setMensagem] = useState("");
  const [erro, setErro] = useState("");
  const [senhaAtual, setSenhaAtual] = useState("");
  const [novaSenha, setNovaSenha] = useState("");
  const [confirmarSenha, setConfirmarSenha] = useState("");
  const [mostrarSenhas, setMostrarSenhas] = useState(false);
  const [salvandoSenha, setSalvandoSenha] = useState(false);
  const [erroSenha, setErroSenha] = useState("");

  useEffect(() => {
    if (!proprietarioId) {
      navigate("/login");
      return;
    }
    Promise.all([
      apiFetch(`${API}/estabelecimentos/proprietario/${proprietarioId}`),
      apiFetch(`${API}/proprietarios/me`),
    ])
      .then(async ([respostaEstabelecimentos, respostaConta]) => {
        if (!respostaEstabelecimentos.ok || !respostaConta.ok) throw new Error("Não foi possível carregar os dados da conta.");
        const [estabelecimentos, conta] = await Promise.all([respostaEstabelecimentos.json(), respostaConta.json()]);
        const estabelecimentoAtual = estabelecimentos[0];
        if (!estabelecimentoAtual) throw new Error("Nenhum estabelecimento encontrado.");
        setEstabelecimento(estabelecimentoAtual);
        setNomeEstabelecimento(estabelecimentoAtual.nome);
        setProprietario(conta);
      })
      .catch((error) => setErro(error.message))
      .finally(() => setCarregando(false));
  }, [proprietarioId, navigate]);

  const salvarEstabelecimento = async (event) => {
    event.preventDefault();
    setErro("");
    setMensagem("");
    if (!nomeEstabelecimento.trim()) return setErro("Informe o nome do estabelecimento.");
    setSalvando(true);
    try {
      const resposta = await apiFetch(`${API}/estabelecimentos/${estabelecimento.id}/nome`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ proprietarioId: Number(proprietarioId), nome: nomeEstabelecimento.trim() }),
      });
      if (!resposta.ok) throw new Error((await resposta.text()) || "Não foi possível atualizar o nome.");
      const atualizado = await resposta.json();
      setEstabelecimento(atualizado);
      setNomeEstabelecimento(atualizado.nome);
      setMensagem("Nome do estabelecimento atualizado com sucesso.");
    } catch (error) { setErro(error.message); }
    finally { setSalvando(false); }
  };

  const alterarSenha = async (event) => {
    event.preventDefault();
    setErroSenha("");
    if (novaSenha !== confirmarSenha) return setErroSenha("A confirmação não corresponde à nova senha.");
    if (novaSenha === senhaAtual) return setErroSenha("A nova senha deve ser diferente da senha atual.");
    if (novaSenha.length < 8) return setErroSenha("A nova senha deve ter pelo menos 8 caracteres.");
    setSalvandoSenha(true);
    try {
      const resposta = await apiFetch(`${API}/proprietarios/senha`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ senhaAtual, novaSenha }),
      });
      if (!resposta.ok) throw new Error((await resposta.text()) || "Não foi possível alterar a senha.");
      clearSessionUser();
      navigate("/login", { replace: true, state: { mensagem: "Senha alterada com sucesso. Entre novamente com sua nova senha." } });
    } catch (error) { setErroSenha(error.message || "Não foi possível conectar ao servidor."); }
    finally { setSalvandoSenha(false); }
  };

  if (carregando) return <p>Carregando dados da conta...</p>;

  return (
    <section className="profile-page account-page">
      <header className="profile-page-header"><div><h1>Minha conta</h1><p>Consulte seus dados de acesso e mantenha sua senha segura.</p></div></header>

      <section className="profile-form">
        <div className="profile-form-heading"><h2>Dados da conta</h2><p>Informações usadas para identificar seu acesso ao ContIA.</p></div>
        <div className="account-identity-grid">
          <div className="profile-field"><label>Nome do proprietário</label><input value={proprietario?.nome || ""} disabled /></div>
          <div className="profile-field"><label>E-mail de acesso</label><input type="email" value={proprietario?.email || ""} disabled /></div>
        </div>
      </section>

      <form className="profile-form" onSubmit={alterarSenha}>
        <div className="profile-form-heading"><h2>Alterar senha</h2><p>Use no mínimo 8 caracteres. Será necessário entrar novamente após a alteração.</p></div>
        <div className="account-password-fields">
          <div className="profile-field"><label htmlFor="senhaAtual">Senha atual</label><div className="password-input-wrap"><input id="senhaAtual" type={mostrarSenhas ? "text" : "password"} autoComplete="current-password" value={senhaAtual} onChange={(event) => setSenhaAtual(event.target.value)} required /><button type="button" className="password-visibility" onClick={() => setMostrarSenhas((atual) => !atual)} aria-label={mostrarSenhas ? "Ocultar senhas" : "Mostrar senhas"}><svg viewBox="0 0 24 24" aria-hidden="true"><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z"/><circle cx="12" cy="12" r="3"/>{mostrarSenhas && <path d="m4 4 16 16"/>}</svg></button></div></div>
          <div className="profile-field"><label htmlFor="novaSenha">Nova senha</label><input id="novaSenha" type={mostrarSenhas ? "text" : "password"} autoComplete="new-password" minLength="8" value={novaSenha} onChange={(event) => setNovaSenha(event.target.value)} required /></div>
          <div className="profile-field"><label htmlFor="confirmarSenha">Confirmar nova senha</label><input id="confirmarSenha" type={mostrarSenhas ? "text" : "password"} autoComplete="new-password" minLength="8" value={confirmarSenha} onChange={(event) => setConfirmarSenha(event.target.value)} required /></div>
        </div>
        {erroSenha && <p className="auth-error" role="alert">{erroSenha}</p>}
        <div className="profile-actions"><button className="primary-button" type="submit" disabled={salvandoSenha}>{salvandoSenha ? "Alterando..." : "Alterar senha"}</button></div>
      </form>

      <form className="profile-form" onSubmit={salvarEstabelecimento}>
        <div className="profile-form-heading"><h2>Dados do estabelecimento</h2><p>Atualize o nome do seu negócio.</p></div>
        <div className="profile-field"><label htmlFor="nomeEstabelecimento">Nome do estabelecimento</label><input id="nomeEstabelecimento" type="text" value={nomeEstabelecimento} onChange={(event) => setNomeEstabelecimento(event.target.value)} required /></div>
        {erro && <p className="auth-error" role="alert">{erro}</p>}
        {mensagem && <p className="profile-success" role="status">{mensagem}</p>}
        <div className="profile-actions"><button className="primary-button" type="submit" disabled={salvando}>{salvando ? "Salvando..." : "Salvar alterações"}</button><button className="profile-back-button" type="button" onClick={() => navigate("/dashboard")}>Voltar ao dashboard</button></div>
      </form>
    </section>
  );
}
