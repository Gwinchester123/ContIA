import { useEffect, useState } from "react";
import { apiFetch, getSessionUser } from "../data/financeStorage";
import { API_BASE_URL } from "../data/api";

const API = API_BASE_URL;
const formularioInicial = { nome: "", email: "", senha: "", confirmarSenha: "", estabelecimentoId: "" };

export default function Funcionarios() {
  const [funcionarios, setFuncionarios] = useState([]);
  const [estabelecimentos, setEstabelecimentos] = useState([]);
  const [formulario, setFormulario] = useState(formularioInicial);
  const [editandoId, setEditandoId] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");

  const carregar = async () => {
    const [respostaFuncionarios, respostaEstabelecimentos] = await Promise.all([
      apiFetch(`${API}/funcionarios`),
      apiFetch(`${API}/estabelecimentos/proprietario/${getSessionUser()}`),
    ]);
    if (!respostaFuncionarios.ok || !respostaEstabelecimentos.ok) {
      throw new Error("Não foi possível carregar os funcionários e estabelecimentos.");
    }
    const [lista, lojas] = await Promise.all([respostaFuncionarios.json(), respostaEstabelecimentos.json()]);
    setFuncionarios(lista);
    setEstabelecimentos(lojas);
    setFormulario((atual) => atual.estabelecimentoId ? atual : { ...atual, estabelecimentoId: String(lojas[0]?.id || "") });
  };

  useEffect(() => {
    carregar().catch((error) => setErro(error.message)).finally(() => setCarregando(false));
  }, []);

  const atualizarCampo = (campo, valor) => setFormulario((atual) => ({ ...atual, [campo]: valor }));

  const cancelarEdicao = () => {
    setEditandoId(null);
    setFormulario({ ...formularioInicial, estabelecimentoId: String(estabelecimentos[0]?.id || "") });
  };

  const iniciarEdicao = async (funcionario) => {
    setErro("");
    try {
      const resposta = await apiFetch(`${API}/funcionarios/${funcionario.id}`);
      if (!resposta.ok) throw new Error((await resposta.text()) || "Não foi possível consultar o funcionário.");
      const dados = await resposta.json();
      setEditandoId(dados.id);
      setFormulario({ nome: dados.nome, email: dados.email, senha: "", confirmarSenha: "", estabelecimentoId: String(dados.estabelecimentoId) });
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) { setErro(error.message); }
  };

  const salvar = async (event) => {
    event.preventDefault();
    setErro("");
    setSucesso("");
    if (!editandoId && !formulario.estabelecimentoId) return setErro("Cadastre um estabelecimento antes de adicionar funcionários.");
    if (formulario.senha !== formulario.confirmarSenha) return setErro("A confirmação de senha não corresponde.");
    if (!editandoId && formulario.senha.length < 8) return setErro("A senha deve ter pelo menos 8 caracteres.");
    if (editandoId && formulario.senha && formulario.senha.length < 8) return setErro("A senha deve ter pelo menos 8 caracteres.");

    setSalvando(true);
    try {
      const payload = { nome: formulario.nome.trim(), email: formulario.email.trim() };
      if (formulario.senha) payload.senha = formulario.senha;
      if (!editandoId) {
        payload.senha = formulario.senha;
        payload.estabelecimentoId = Number(formulario.estabelecimentoId);
      }
      const resposta = await apiFetch(editandoId ? `${API}/funcionarios/${editandoId}` : `${API}/funcionarios`, {
        method: editandoId ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
      });
      if (!resposta.ok) throw new Error((await resposta.text()) || "Não foi possível salvar o funcionário.");
      await carregar();
      cancelarEdicao();
      setSucesso(editandoId ? "Funcionário atualizado." : "Funcionário cadastrado.");
    } catch (error) { setErro(error.message); }
    finally { setSalvando(false); }
  };

  const alternarSituacao = async (funcionario) => {
    setErro("");
    setSucesso("");
    try {
      const resposta = await apiFetch(`${API}/funcionarios/${funcionario.id}/situacao`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ ativo: !funcionario.ativo }),
      });
      if (!resposta.ok) throw new Error((await resposta.text()) || "Não foi possível alterar a situação.");
      await carregar();
      setSucesso(funcionario.ativo ? "Funcionário desativado." : "Funcionário ativado.");
    } catch (error) { setErro(error.message); }
  };

  const nomeEstabelecimento = (id) => estabelecimentos.find((item) => String(item.id) === String(id))?.nome || "Estabelecimento";

  return (
    <section className="panel page-panel employee-page">
      <header className="page-heading">
        <div><h1>Funcionários</h1><p>Gerencie quem pode registrar vendas nos seus estabelecimentos.</p></div>
      </header>
      <form className="employee-form" onSubmit={salvar}>
        <h2>{editandoId ? "Editar funcionário" : "Cadastrar funcionário"}</h2>
        <div className="employee-form-grid">
          <label>Nome<input value={formulario.nome} onChange={(event) => atualizarCampo("nome", event.target.value)} maxLength="100" required /></label>
          <label>Usuário (e-mail)<input type="email" value={formulario.email} onChange={(event) => atualizarCampo("email", event.target.value)} maxLength="150" required /></label>
          {!editandoId && <label>Estabelecimento<select value={formulario.estabelecimentoId} onChange={(event) => atualizarCampo("estabelecimentoId", event.target.value)} required><option value="">Selecione</option>{estabelecimentos.map((item) => <option key={item.id} value={item.id}>{item.nome}</option>)}</select></label>}
          <label>{editandoId ? "Nova senha (opcional)" : "Senha"}<input type="password" value={formulario.senha} onChange={(event) => atualizarCampo("senha", event.target.value)} minLength="8" autoComplete="new-password" required={!editandoId} /></label>
          <label>Confirmar senha<input type="password" value={formulario.confirmarSenha} onChange={(event) => atualizarCampo("confirmarSenha", event.target.value)} minLength="8" autoComplete="new-password" required={!editandoId || Boolean(formulario.senha)} /></label>
        </div>
        {erro && <p className="error-message" role="alert">{erro}</p>}
        {sucesso && <p role="status" className="employee-success">{sucesso}</p>}
        <div className="employee-form-actions"><button className="primary-button" type="submit" disabled={salvando}>{salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Cadastrar funcionário"}</button>{editandoId && <button className="profile-back-button" type="button" onClick={cancelarEdicao}>Cancelar</button>}</div>
      </form>

      <div className="employee-list-heading"><div><h2>Equipe</h2><p>{funcionarios.length} funcionário(s)</p></div></div>
      {carregando ? <p>Carregando funcionários...</p> : funcionarios.length === 0 ? <p>Nenhum funcionário cadastrado.</p> : (
        <div className="table-wrapper"><table className="responsive-table employee-table"><thead><tr><th>Nome</th><th>Usuário</th><th>Estabelecimento</th><th>Situação</th><th>Ações</th></tr></thead><tbody>
          {funcionarios.map((funcionario) => <tr key={funcionario.id}><td data-label="Nome">{funcionario.nome}</td><td data-label="Usuário">{funcionario.email}</td><td data-label="Estabelecimento">{nomeEstabelecimento(funcionario.estabelecimentoId)}</td><td data-label="Situação"><span className={`employee-status ${funcionario.ativo ? "active" : "inactive"}`}>{funcionario.ativo ? "Ativo" : "Inativo"}</span></td><td className="record-actions" data-label="Ações"><button type="button" className="profile-back-button" onClick={() => iniciarEdicao(funcionario)}>Editar</button><button type="button" className="profile-back-button" onClick={() => alternarSituacao(funcionario)}>{funcionario.ativo ? "Desativar" : "Ativar"}</button></td></tr>)}
        </tbody></table></div>
      )}
    </section>
  );
}
