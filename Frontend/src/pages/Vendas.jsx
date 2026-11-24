import { useEffect, useMemo, useState } from "react";
import {
  apiFetch,
  createId,
  formatCurrency,
  getSessionEstablishment,
  getSessionProfile,
  getSessionUser,
} from "../data/financeStorage";
import { API_BASE_URL } from "../data/api";

const API = API_BASE_URL;
const novoItem = () => ({ key: createId(), produto: "", quantidade: "", valorUnitario: "" });

function itensDaVenda(venda) {
  if (venda.itens?.length) return venda.itens;
  return [{ produto: venda.produto, quantidade: venda.quantidade, valorUnitario: venda.valorUnitario, subtotal: venda.total }];
}

function ItemVendaEditor({ item, estabelecimentoId, onChange, onRemove, podeRemover }) {
  const [sugestoes, setSugestoes] = useState([]);
  const [aberto, setAberto] = useState(false);

  useEffect(() => {
    const consulta = item.produto.trim();
    if (!estabelecimentoId || !consulta) {
      setSugestoes([]);
      return undefined;
    }
    setSugestoes([]);

    const controller = new AbortController();
    const timer = window.setTimeout(async () => {
      try {
        const params = new URLSearchParams({ estabelecimentoId, q: consulta });
        const resposta = await apiFetch(`${API}/vendas/sugestoes?${params}`, { signal: controller.signal });
        if (!resposta.ok) return;
        const dados = await resposta.json();
        setSugestoes(dados);
        setAberto(true);
      } catch (error) {
        if (error.name !== "AbortError") console.error("Erro ao carregar sugestões:", error);
      }
    }, 180);

    return () => {
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [item.produto, estabelecimentoId]);

  const selecionarSugestao = (sugestao) => {
    onChange({ produto: sugestao.produto, valorUnitario: String(sugestao.ultimoPreco) });
    setAberto(false);
  };

  return (
    <div className="sale-item-editor">
      <div className="sale-item-name-field">
        <label htmlFor={`produto-${item.key}`}>Nome do item</label>
        <input
          id={`produto-${item.key}`}
          value={item.produto}
          onChange={(event) => onChange({ produto: event.target.value })}
          onFocus={() => setAberto(true)}
          onBlur={() => window.setTimeout(() => setAberto(false), 120)}
          autoComplete="off"
          maxLength={100}
          placeholder="Ex.: Café coado"
          role="combobox"
          aria-autocomplete="list"
          aria-expanded={aberto && sugestoes.length > 0}
          required
        />
        {aberto && sugestoes.length > 0 && (
          <div className="sale-suggestions" role="listbox" aria-label="Itens já vendidos neste estabelecimento">
            {sugestoes.map((sugestao) => (
              <button
                type="button"
                role="option"
                key={`${sugestao.produto}-${sugestao.ultimoPreco}`}
                onMouseDown={(event) => event.preventDefault()}
                onClick={() => selecionarSugestao(sugestao)}
              >
                <span>{sugestao.produto}</span>
                <strong>{formatCurrency(Number(sugestao.ultimoPreco))}</strong>
              </button>
            ))}
          </div>
        )}
      </div>

      <label>
        Quantidade
        <input
          type="number"
          min="1"
          step="1"
          value={item.quantidade}
          onChange={(event) => onChange({ quantidade: event.target.value })}
          required
        />
      </label>

      <label>
        Valor unitário
        <input
          type="number"
          min="0.01"
          step="0.01"
          value={item.valorUnitario}
          onChange={(event) => onChange({ valorUnitario: event.target.value })}
          required
        />
      </label>

      <div className="sale-item-subtotal">
        <span>Subtotal</span>
        <strong>{formatCurrency((Number(item.quantidade) || 0) * (Number(item.valorUnitario) || 0))}</strong>
      </div>
      <button
        className="sale-item-remove"
        type="button"
        onClick={onRemove}
        disabled={!podeRemover}
        aria-label={`Remover ${item.produto || "item"}`}
        title="Remover item"
      >
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 6h18" />
          <path d="M8 6V4h8v2" />
          <path d="M19 6l-1 14H6L5 6" />
          <path d="M10 10v6M14 10v6" />
        </svg>
      </button>
    </div>
  );
}

export default function Vendas({ somenteMinhas = false }) {
  const perfil = getSessionProfile();
  const proprietario = perfil === "PROPRIETARIO";
  const [vendas, setVendas] = useState([]);
  const [estabelecimentos, setEstabelecimentos] = useState([]);
  const [estabelecimentoId, setEstabelecimentoId] = useState(getSessionEstablishment());
  const [itens, setItens] = useState([novoItem()]);
  const [editandoId, setEditandoId] = useState(null);
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const [sucesso, setSucesso] = useState("");

  useEffect(() => {
    let ativo = true;

    async function carregar() {
      try {
        setErro("");
        const usuarioId = getSessionUser();
        if (!usuarioId) throw new Error("Usuário não identificado. Entre novamente.");

        if (proprietario) {
          const respostaEstabelecimentos = await apiFetch(`${API}/estabelecimentos/proprietario/${usuarioId}`);
          if (!respostaEstabelecimentos.ok) throw new Error("Não foi possível carregar os estabelecimentos.");
          const dadosEstabelecimentos = await respostaEstabelecimentos.json();
          if (!ativo) return;
          setEstabelecimentos(dadosEstabelecimentos);
          setEstabelecimentoId((atual) => {
            const aindaExiste = dadosEstabelecimentos.some((estabelecimento) => String(estabelecimento.id) === String(atual));
            return aindaExiste ? atual : String(dadosEstabelecimentos[0]?.id || "");
          });
        } else {
          let id = getSessionEstablishment();
          if (!id) {
            const respostaUsuario = await apiFetch(`${API}/auth/me`);
            if (!respostaUsuario.ok) throw new Error("Não foi possível identificar o estabelecimento do funcionário.");
            const usuario = await respostaUsuario.json();
            id = String(usuario.estabelecimentoId || "");
          }
          if (!id) throw new Error("Nenhum estabelecimento está associado a este funcionário.");
          if (ativo) setEstabelecimentoId(String(id));
        }

        const respostaVendas = await apiFetch(`${API}/vendas`);
        if (!respostaVendas.ok) throw new Error("Não foi possível carregar as vendas.");
        const dadosVendas = await respostaVendas.json();
        if (ativo) setVendas(dadosVendas);
      } catch (error) {
        if (ativo) setErro(error.message || "Não foi possível conectar ao servidor.");
      } finally {
        if (ativo) setCarregando(false);
      }
    }

    carregar();
    return () => { ativo = false; };
  }, [proprietario]);

  const total = useMemo(() => itens.reduce(
    (soma, item) => soma + ((Number(item.quantidade) || 0) * (Number(item.valorUnitario) || 0)), 0,
  ), [itens]);

  const atualizarItem = (key, alteracoes) => {
    setItens((atuais) => atuais.map((item) => item.key === key ? { ...item, ...alteracoes } : item));
  };

  const preencherFormulario = (venda) => {
    setItens(itensDaVenda(venda).map((item) => ({
      key: createId(),
      produto: item.produto || "",
      quantidade: String(item.quantidade ?? ""),
      valorUnitario: String(item.valorUnitario ?? ""),
    })));
  };

  const limparFormulario = () => {
    setEditandoId(null);
    setItens([novoItem()]);
  };

  const montarItensRequest = () => itens.map((item) => ({
    produto: item.produto.trim(),
    quantidade: Number(item.quantidade),
    valorUnitario: Number(item.valorUnitario),
  }));

  const salvarVenda = async (event) => {
    event.preventDefault();
    setErro("");
    setSucesso("");
    if (!estabelecimentoId) {
      setErro("Selecione um estabelecimento antes de registrar a venda.");
      return;
    }
    setSalvando(true);

    try {
      const resposta = await apiFetch(editandoId ? `${API}/vendas/${editandoId}` : `${API}/vendas`, {
        method: editandoId ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ estabelecimentoId: Number(estabelecimentoId), itens: montarItensRequest() }),
      });
      if (!resposta.ok) throw new Error((await resposta.text()) || "Não foi possível salvar a venda.");

      const vendaSalva = await resposta.json();
      setVendas((atuais) => editandoId
        ? atuais.map((venda) => venda.id === vendaSalva.id ? vendaSalva : venda)
        : [vendaSalva, ...atuais]);
      limparFormulario();
      setSucesso(editandoId ? "Venda atualizada com sucesso." : "Venda registrada com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setSalvando(false);
    }
  };

  const iniciarEdicao = (venda) => {
    setEditandoId(venda.id);
    setEstabelecimentoId(String(venda.estabelecimentoId));
    preencherFormulario(venda);
    setErro("");
    setSucesso("");
    window.scrollTo({ top: 0, behavior: "smooth" });
  };

  const excluirVenda = async (venda) => {
    const descricao = itensDaVenda(venda).map((item) => item.produto).join(", ");
    if (!window.confirm(`Deseja excluir a venda de ${descricao}?`)) return;
    setErro("");
    setSucesso("");
    try {
      const resposta = await apiFetch(`${API}/vendas/${venda.id}?estabelecimentoId=${venda.estabelecimentoId}`, { method: "DELETE" });
      if (!resposta.ok) throw new Error((await resposta.text()) || "Não foi possível excluir a venda.");
      setVendas((atuais) => atuais.filter((item) => item.id !== venda.id));
      if (editandoId === venda.id) limparFormulario();
      setSucesso("Venda excluída com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    }
  };

  return (
    <>
      <header className="sales-page-header">
        <div>
          <h1>Vendas</h1>
          <p>{proprietario ? "Registre e consulte as vendas dos seus estabelecimentos." : "Registre vendas e consulte as que você realizou."}</p>
        </div>
      </header>

      <section className="panel page-panel">
        {!somenteMinhas && <div className="registration-panel">
          <h3>{editandoId ? "Editar venda" : "Registrar venda"}</h3>
          {proprietario && estabelecimentos.length > 1 && (
            <label className="sales-establishment-select">
              Estabelecimento
              <select value={estabelecimentoId} onChange={(event) => setEstabelecimentoId(event.target.value)} disabled={Boolean(editandoId)}>
                {estabelecimentos.map((estabelecimento) => <option key={estabelecimento.id} value={estabelecimento.id}>{estabelecimento.nome}</option>)}
              </select>
            </label>
          )}

          <form onSubmit={salvarVenda}>
            <div className="sale-items-editor">
              {itens.map((item, index) => (
                <ItemVendaEditor
                  key={item.key}
                  item={item}
                  estabelecimentoId={estabelecimentoId}
                  onChange={(alteracoes) => atualizarItem(item.key, alteracoes)}
                  onRemove={() => setItens((atuais) => atuais.filter((atual) => atual.key !== item.key))}
                  podeRemover={itens.length > 1}
                />
              ))}
            </div>

            <div className="sale-form-footer">
              <div className="sale-form-buttons">
                <button className="profile-back-button" type="button" onClick={() => setItens((atuais) => [...atuais, novoItem()])}>
                  Adicionar item
                </button>
                <button className="primary-button" type="submit" disabled={salvando || !estabelecimentoId || itens.length === 0}>
                  {salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Registrar venda"}
                </button>
              </div>
              <div className="sale-grand-total">
                <span>Total da venda</span>
                <strong>{formatCurrency(total)}</strong>
              </div>
              {editandoId && <button className="profile-back-button" type="button" onClick={limparFormulario}>Cancelar</button>}
            </div>
          </form>
          {sucesso && <p role="status">{sucesso}</p>}
          {erro && <p className="error-message" role="alert">{erro}</p>}
        </div>}

        <h3>{somenteMinhas ? "Minhas vendas" : proprietario ? "Vendas registradas" : "Minhas vendas"}</h3>
        {carregando ? (
          <p>Carregando vendas...</p>
        ) : vendas.length === 0 ? (
          <p>Nenhuma venda registrada.</p>
        ) : (
          <div className="table-wrapper">
            <table className="responsive-table sales-table">
              <thead>
                <tr>
                  <th>Itens</th>
                  <th>Total</th>
                  <th>Data e horário</th>
                  <th>Responsável</th>
                  {proprietario && <th>Ações</th>}
                </tr>
              </thead>
              <tbody>
                {vendas.map((venda) => (
                  <tr key={venda.id}>
                    <td data-label="Itens">
                      <ul className="sale-item-list">
                        {itensDaVenda(venda).map((item, index) => (
                          <li key={item.id || `${venda.id}-${index}`}>
                            <span>{item.produto} · {item.quantidade} × {formatCurrency(Number(item.valorUnitario))}</span>
                            <strong>{formatCurrency(Number(item.subtotal ?? item.total))}</strong>
                          </li>
                        ))}
                      </ul>
                    </td>
                    <td data-label="Total"><strong>{formatCurrency(Number(venda.total))}</strong></td>
                    <td data-label="Data e horário">{new Date(venda.dataHora).toLocaleString("pt-BR")}</td>
                    <td data-label="Responsável">{venda.responsavelNome ? `${venda.responsavelNome} (${venda.responsavelPerfil === "FUNCIONARIO" ? "Funcionário" : "Proprietário"})` : "Histórico anterior"}</td>
                    {proprietario && (
                      <td className="record-actions" data-label="Ações">
                        <button className="record-action" type="button" title="Editar venda" aria-label="Editar venda" onClick={() => iniciarEdicao(venda)}>
                          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m16 4 4 4M4 20l4-.8L19 8a2.8 2.8 0 0 0-4-4L4 15z" /></svg>
                        </button>
                        <button className="record-action danger" type="button" title="Excluir venda" aria-label="Excluir venda" onClick={() => excluirVenda(venda)}>
                          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 7h16M10 11v6m4-6v6M6 7l1 14h10l1-14M9 7V4h6v3" /></svg>
                        </button>
                      </td>
                    )}
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
