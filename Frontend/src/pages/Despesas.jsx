import { useEffect, useMemo, useState } from "react";
import { apiFetch, formatCurrency, getSessionUser } from "../data/financeStorage";
import { API_BASE_URL } from "../data/api";

const API = API_BASE_URL;
const novoLancamento = () => ({
  chave: globalThis.crypto?.randomUUID?.() || `${Date.now()}-${Math.random()}`,
  descricao: "",
  categoria: "",
  complemento: "",
  valor: "",
});

function descricaoCompleta(lancamento) {
  const complemento = lancamento.categoria === "Outros" ? lancamento.complemento.trim() : "";
  return complemento
    ? `${lancamento.descricao.trim()} — ${complemento}`
    : lancamento.descricao.trim();
}

function separarDescricao(despesa) {
  const separador = despesa.categoria === "Outros"
    ? despesa.descricao.lastIndexOf(" — ")
    : -1;
  return {
    descricao: separador >= 0 ? despesa.descricao.slice(0, separador) : despesa.descricao,
    complemento: separador >= 0 ? despesa.descricao.slice(separador + 3) : "",
  };
}

export default function Despesas() {
  const proprietarioId = getSessionUser();
  const [despesas, setDespesas] = useState([]);
  const [estabelecimentoId, setEstabelecimentoId] = useState(null);
  const [lancamentos, setLancamentos] = useState(() => [novoLancamento()]);
  const [editandoId, setEditandoId] = useState(null);
  const [carregando, setCarregando] = useState(Boolean(proprietarioId));
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState(proprietarioId ? "" : "Usuário não identificado.");
  const [sucesso, setSucesso] = useState("");

  useEffect(() => {
    if (!proprietarioId) return undefined;

    async function carregarDespesas() {
      try {
        setErro("");
        const respostaEstabelecimento = await apiFetch(
          `${API}/estabelecimentos/proprietario/${proprietarioId}`,
        );
        if (!respostaEstabelecimento.ok) {
          throw new Error("Não foi possível buscar o estabelecimento.");
        }
        const estabelecimentos = await respostaEstabelecimento.json();
        const estabelecimentoAtual = estabelecimentos[0] || null;
        if (!estabelecimentoAtual) throw new Error("Nenhum estabelecimento encontrado.");
        setEstabelecimentoId(estabelecimentoAtual.id);

        const resposta = await apiFetch(
          `${API}/despesas/estabelecimento/${estabelecimentoAtual.id}`,
        );
        if (!resposta.ok) throw new Error("Não foi possível carregar as despesas.");
        setDespesas(await resposta.json());
      } catch (error) {
        console.error("Erro ao carregar despesas:", error);
        setErro(error.message);
      } finally {
        setCarregando(false);
      }
    }
    carregarDespesas();
  }, [proprietarioId]);

  const totalLancamentos = useMemo(
    () => lancamentos.reduce((total, item) => total + (Number(item.valor) || 0), 0),
    [lancamentos],
  );

  const atualizarLancamento = (chave, alteracoes) => {
    setLancamentos((atuais) => atuais.map((item) =>
      item.chave === chave ? { ...item, ...alteracoes } : item,
    ));
  };

  const cancelarEdicao = () => {
    setEditandoId(null);
    setLancamentos([novoLancamento()]);
  };

  const salvarLancamentos = async (event) => {
    event.preventDefault();
    setErro("");
    setSucesso("");
    setSalvando(true);

    try {
      if (editandoId) {
        const item = lancamentos[0];
        const resposta = await apiFetch(`${API}/despesas/${editandoId}`, {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            estabelecimentoId,
            descricao: descricaoCompleta(item),
            categoria: item.categoria,
            valor: Number(item.valor),
          }),
        });
        if (!resposta.ok) {
          throw new Error((await resposta.text()) || "Não foi possível atualizar a despesa.");
        }
        const atualizada = await resposta.json();
        setDespesas((atuais) => atuais.map((despesa) =>
          despesa.id === atualizada.id ? atualizada : despesa,
        ));
        cancelarEdicao();
        setSucesso("Despesa atualizada com sucesso.");
        return;
      }

      const resposta = await apiFetch(`${API}/despesas/lote`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          estabelecimentoId,
          despesas: lancamentos.map((item) => ({
            descricao: descricaoCompleta(item),
            categoria: item.categoria,
            valor: Number(item.valor),
          })),
        }),
      });
      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "Não foi possível registrar as despesas.");
      }
      const salvas = await resposta.json();
      setDespesas((atuais) => [...salvas.slice().reverse(), ...atuais]);
      setLancamentos([novoLancamento()]);
      setSucesso(`${salvas.length} ${salvas.length === 1 ? "despesa registrada" : "despesas registradas"} com sucesso.`);
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setSalvando(false);
    }
  };

  const iniciarEdicao = (despesa) => {
    const partes = separarDescricao(despesa);
    setEditandoId(despesa.id);
    setLancamentos([{
      chave: String(despesa.id),
      ...partes,
      categoria: despesa.categoria,
      valor: String(despesa.valor),
    }]);
    setErro("");
    setSucesso("");
  };

  const excluirDespesa = async (despesa) => {
    if (!window.confirm(`Deseja excluir a despesa “${despesa.descricao}”?`)) return;
    setErro("");
    setSucesso("");
    try {
      const resposta = await apiFetch(
        `${API}/despesas/${despesa.id}?estabelecimentoId=${estabelecimentoId}`,
        { method: "DELETE" },
      );
      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "Não foi possível excluir a despesa.");
      }
      setDespesas((atuais) => atuais.filter((item) => item.id !== despesa.id));
      if (editandoId === despesa.id) cancelarEdicao();
      setSucesso("Despesa excluída com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    }
  };

  return (
    <>
      <header>
        <div>
          <h1>Despesas</h1>
          <p>Registre e consulte os gastos do seu negócio.</p>
        </div>
      </header>

      <section className="panel page-panel">
        <div className="registration-panel">
          <h3>{editandoId ? "Editar despesa" : "Registrar despesas"}</h3>
          <p className="expense-time-note">Data e horário serão registrados automaticamente ao salvar.</p>
          <form onSubmit={salvarLancamentos}>
            <div className="expense-batch-list">
              {lancamentos.map((item, index) => (
                <fieldset className="expense-entry" key={item.chave}>
                  <legend>{editandoId ? "Lançamento" : `Lançamento ${index + 1}`}</legend>
                  <label>
                    Descrição
                    <input
                      value={item.descricao}
                      onChange={(event) => atualizarLancamento(item.chave, { descricao: event.target.value })}
                      maxLength="150"
                      required
                    />
                  </label>
                  <label>
                    Categoria
                    <select
                      value={item.categoria}
                      onChange={(event) => atualizarLancamento(item.chave, { categoria: event.target.value })}
                      required
                    >
                      <option value="" disabled>Selecione uma categoria</option>
                      <optgroup label="Custos com insumos">
                        <option>Ingredientes</option>
                        <option>Materiais</option>
                        <option>Insumos</option>
                        <option>Embalagens</option>
                      </optgroup>
                      <optgroup label="Despesas operacionais">
                        <option>Transporte</option>
                        <option>Funcionários</option>
                        <option>Aluguel</option>
                        <option>Água e energia</option>
                        <option>Manutenção</option>
                        <option>Outros</option>
                      </optgroup>
                    </select>
                  </label>
                  {item.categoria === "Outros" && (
                    <label>
                      Complemento (opcional)
                      <input
                        value={item.complemento}
                        onChange={(event) => atualizarLancamento(item.chave, { complemento: event.target.value })}
                        maxLength={Math.max(0, 150 - item.descricao.length - 3)}
                      />
                    </label>
                  )}
                  <label>
                    Valor
                    <input
                      type="number"
                      min="0.01"
                      step="0.01"
                      value={item.valor}
                      onChange={(event) => atualizarLancamento(item.chave, { valor: event.target.value })}
                      required
                    />
                  </label>
                  <div className="expense-entry-subtotal">
                    <span>Subtotal deste lançamento</span>
                    <strong>{formatCurrency(Number(item.valor) || 0)}</strong>
                  </div>
                  {!editandoId && lancamentos.length > 1 && (
                    <button
                      className="profile-back-button expense-remove-button"
                      type="button"
                      onClick={() => setLancamentos((atuais) => atuais.filter((entry) => entry.chave !== item.chave))}
                      aria-label={`Remover lançamento ${index + 1}`}
                    >
                      <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="M3 6h18" />
                        <path d="M8 6V4h8v2" />
                        <path d="M19 6l-1 14H6L5 6" />
                        <path d="M10 10v6M14 10v6" />
                      </svg>
                    </button>
                  )}
                </fieldset>
              ))}
            </div>

            {!editandoId && (
              <div className="expense-batch-actions">
                <button className="profile-back-button" type="button" onClick={() => setLancamentos((atuais) => [...atuais, novoLancamento()])}>
                  Adicionar lançamento
                </button>
                <div className="expense-batch-total">
                  <span>Total das despesas</span>
                  <strong>{formatCurrency(totalLancamentos)}</strong>
                </div>
              </div>
            )}

            <div className="expense-form-actions">
              <button className="primary-button" type="submit" disabled={salvando || !estabelecimentoId}>
                {salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Salvar lançamentos"}
              </button>
              {editandoId && <button className="profile-back-button" type="button" onClick={cancelarEdicao}>Cancelar</button>}
            </div>
          </form>
          {sucesso && <p role="status">{sucesso}</p>}
          {erro && <p className="error-message" role="alert">{erro}</p>}
        </div>

        <h3>Despesas registradas</h3>
        {carregando ? (
          <p>Carregando despesas...</p>
        ) : despesas.length === 0 ? (
          <p>Nenhuma despesa registrada.</p>
        ) : (
          <div className="table-wrapper">
            <table className="responsive-table">
              <thead>
                <tr>
                  <th>Descrição</th>
                  <th>Categoria</th>
                  <th>Valor</th>
                  <th>Data e horário</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {despesas.map((despesa) => (
                  <tr key={despesa.id}>
                    <td data-label="Descrição">{despesa.descricao}</td>
                    <td data-label="Categoria">{despesa.categoria}</td>
                    <td data-label="Valor">{formatCurrency(despesa.valor)}</td>
                    <td data-label="Data e horário">{new Date(despesa.dataHora).toLocaleString("pt-BR", { timeZone: "America/Fortaleza" })}</td>
                    <td className="record-actions" data-label="Ações">
                      <button className="record-action" type="button" title="Editar despesa" aria-label="Editar despesa" onClick={() => iniciarEdicao(despesa)}>
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m16 4 4 4M4 20l4-.8L19 8a2.8 2.8 0 0 0-4-4L4 15z" /></svg>
                      </button>
                      <button className="record-action danger" type="button" title="Excluir despesa" aria-label="Excluir despesa" onClick={() => excluirDespesa(despesa)}>
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M10 11v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3" /></svg>
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  );
}
