import { useEffect, useState } from "react";
import { formatCurrency, getSessionUser } from "../data/financeStorage";

export default function Despesas() {
  const [despesas, setDespesas] = useState([]);
  const [estabelecimentoId, setEstabelecimentoId] = useState(null);
  const [descricao, setDescricao] = useState("");
  const [categoria, setCategoria] = useState("");
  const [complemento, setComplemento] = useState("");
  const [valor, setValor] = useState("");
  const [editandoId, setEditandoId] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");

  useEffect(() => {
    const proprietarioId = getSessionUser();

    if (!proprietarioId) {
      setErro("Usuário não identificado.");
      setCarregando(false);
      return;
    }

    async function carregarDespesas() {
      try {
        setErro("");

        const respostaEstabelecimento = await fetch(
          `http://localhost:8080/api/estabelecimentos/proprietario/${proprietarioId}`,
        );

        if (!respostaEstabelecimento.ok) {
          throw new Error("Não foi possível buscar o estabelecimento.");
        }

        const estabelecimentos = await respostaEstabelecimento.json();
        const estabelecimentoAtual = estabelecimentos[0] || null;

        if (!estabelecimentoAtual) {
          throw new Error("Nenhum estabelecimento encontrado.");
        }
        setEstabelecimentoId(estabelecimentoAtual.id);

        const resposta = await fetch(
          `http://localhost:8080/api/despesas/estabelecimento/${estabelecimentoAtual.id}`,
        );

        if (!resposta.ok) {
          throw new Error("Não foi possível carregar as despesas.");
        }

        const dados = await resposta.json();
        setDespesas(dados);
      } catch (error) {
        console.error("Erro ao carregar despesas:", error);
        setErro(error.message);
      } finally {
        setCarregando(false);
      }
    }

    carregarDespesas();
  }, []);

  const montarDescricao = () => {
    const detalhe = categoria === "Outros" ? complemento.trim() : "";
    return detalhe ? `${descricao.trim()} — ${detalhe}` : descricao.trim();
  };

  const registrarDespesa = async (event) => {
    event.preventDefault();
    setErro("");
    setSucesso("");
    setSalvando(true);

    try {
      const resposta = await fetch("http://localhost:8080/api/despesas", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          estabelecimentoId,
          descricao: montarDescricao(),
          categoria: categoria.trim(),
          valor: Number(valor),
        }),
      });

      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "Não foi possível registrar a despesa.");
      }

      const despesa = await resposta.json();
      setDespesas((atuais) => [despesa, ...atuais]);
      setDescricao("");
      setCategoria("");
      setComplemento("");
      setValor("");
      setSucesso("Despesa registrada com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setSalvando(false);
    }
  };

  const iniciarEdicao = (despesa) => {
    setEditandoId(despesa.id);
    setCategoria(despesa.categoria);
    const separador = despesa.categoria === "Outros"
      ? despesa.descricao.lastIndexOf(" — ")
      : -1;
    setDescricao(separador >= 0 ? despesa.descricao.slice(0, separador) : despesa.descricao);
    setComplemento(separador >= 0 ? despesa.descricao.slice(separador + 3) : "");
    setValor(String(despesa.valor));
    setErro("");
    setSucesso("");
  };

  const cancelarEdicao = () => {
    setEditandoId(null);
    setDescricao("");
    setCategoria("");
    setComplemento("");
    setValor("");
  };

  const atualizarDespesa = async (event) => {
    event.preventDefault();
    setErro("");
    setSucesso("");
    setSalvando(true);

    try {
      const resposta = await fetch(`http://localhost:8080/api/despesas/${editandoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          estabelecimentoId,
          descricao: montarDescricao(),
          categoria,
          valor: Number(valor),
        }),
      });
      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "Não foi possível atualizar a despesa.");
      }
      const atualizada = await resposta.json();
      setDespesas((atuais) =>
        atuais.map((despesa) => despesa.id === atualizada.id ? atualizada : despesa),
      );
      cancelarEdicao();
      setSucesso("Despesa atualizada com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setSalvando(false);
    }
  };

  const excluirDespesa = async (despesa) => {
    if (!window.confirm(`Deseja excluir a despesa “${despesa.descricao}”?`)) return;
    setErro("");
    setSucesso("");

    try {
      const resposta = await fetch(
        `http://localhost:8080/api/despesas/${despesa.id}?estabelecimentoId=${estabelecimentoId}`,
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
          <h3>{editandoId ? "Editar despesa" : "Registrar despesa"}</h3>
          <form className={`compact-form${categoria === "Outros" ? " expense-form-with-complement" : ""}`} onSubmit={editandoId ? atualizarDespesa : registrarDespesa}>
            <label>
              Descrição
              <input value={descricao} onChange={(event) => setDescricao(event.target.value)} maxLength="150" required />
            </label>
            <label>
              Categoria
              <select value={categoria} onChange={(event) => setCategoria(event.target.value)} required>
                <option value="" disabled>Selecione uma categoria</option>
                <optgroup label="Custos diretos — insumos e materiais">
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
            {categoria === "Outros" && (
              <label>
                Complemento (opcional)
                <input value={complemento} onChange={(event) => setComplemento(event.target.value)} maxLength={Math.max(0, 150 - descricao.length - 3)} />
              </label>
            )}
            <label>
              Valor
              <input type="number" min="0.01" step="0.01" value={valor} onChange={(event) => setValor(event.target.value)} required />
            </label>
            <button className="primary-button" type="submit" disabled={salvando || !estabelecimentoId}>
              {salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Registrar despesa"}
            </button>
            {editandoId && <button className="profile-back-button" type="button" onClick={cancelarEdicao}>Cancelar</button>}
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
                    <td data-label="Data e horário">
                      {new Date(despesa.dataHora).toLocaleString("pt-BR")}
                    </td>
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
