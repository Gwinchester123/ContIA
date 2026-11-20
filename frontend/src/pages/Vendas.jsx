import { useEffect, useState } from "react";
import { formatCurrency, getSessionUser } from "../data/financeStorage";

function formatarNomeProduto(nome) {
  if (!nome) return "";

  const minusculas = ["de", "da", "do", "das", "dos", "e"];

  return nome
    .toLowerCase()
    .split(" ")
    .map((palavra, index) => {
      if (index > 0 && minusculas.includes(palavra)) {
        return palavra;
      }

      return palavra.charAt(0).toUpperCase() + palavra.slice(1);
    })
    .join(" ");
}

export default function Vendas() {
  const [vendas, setVendas] = useState([]);
  const [estabelecimentoId, setEstabelecimentoId] = useState(null);
  const [produto, setProduto] = useState("");
  const [quantidade, setQuantidade] = useState("");
  const [valorUnitario, setValorUnitario] = useState("");
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

    async function carregarVendas() {
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
          `http://localhost:8080/api/vendas/estabelecimento/${estabelecimentoAtual.id}`,
        );

        if (!resposta.ok) {
          throw new Error("Não foi possível carregar as vendas.");
        }

        const dados = await resposta.json();
        setVendas(dados);
      } catch (error) {
        console.error("Erro ao carregar vendas:", error);
        setErro(error.message);
      } finally {
        setCarregando(false);
      }
    }

    carregarVendas();
  }, []);

  const registrarVenda = async (event) => {
    event.preventDefault();
    setErro("");
    setSucesso("");
    setSalvando(true);

    try {
      const resposta = await fetch("http://localhost:8080/api/vendas", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          estabelecimentoId,
          produto: produto.trim(),
          quantidade: Number(quantidade),
          valorUnitario: Number(valorUnitario),
        }),
      });

      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "Não foi possível registrar a venda.");
      }

      const venda = await resposta.json();
      setVendas((atuais) => [venda, ...atuais]);
      setProduto("");
      setQuantidade("");
      setValorUnitario("");
      setSucesso("Venda registrada com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setSalvando(false);
    }
  };

  const iniciarEdicao = (venda) => {
    setEditandoId(venda.id);
    setProduto(venda.produto);
    setQuantidade(String(venda.quantidade));
    setValorUnitario(String(venda.valorUnitario));
    setErro("");
    setSucesso("");
  };

  const cancelarEdicao = () => {
    setEditandoId(null);
    setProduto("");
    setQuantidade("");
    setValorUnitario("");
  };

  const atualizarVenda = async (event) => {
    event.preventDefault();
    setErro("");
    setSucesso("");
    setSalvando(true);

    try {
      const resposta = await fetch(`http://localhost:8080/api/vendas/${editandoId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          estabelecimentoId,
          produto: produto.trim(),
          quantidade: Number(quantidade),
          valorUnitario: Number(valorUnitario),
        }),
      });
      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "Não foi possível atualizar a venda.");
      }
      const atualizada = await resposta.json();
      setVendas((atuais) =>
        atuais.map((venda) => venda.id === atualizada.id ? atualizada : venda),
      );
      cancelarEdicao();
      setSucesso("Venda atualizada com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setSalvando(false);
    }
  };

  const excluirVenda = async (venda) => {
    if (!window.confirm(`Deseja excluir a venda de ${venda.produto}?`)) return;
    setErro("");
    setSucesso("");
    try {
      const resposta = await fetch(
        `http://localhost:8080/api/vendas/${venda.id}?estabelecimentoId=${estabelecimentoId}`,
        { method: "DELETE" },
      );
      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "Não foi possível excluir a venda.");
      }
      setVendas((atuais) => atuais.filter((item) => item.id !== venda.id));
      if (editandoId === venda.id) cancelarEdicao();
      setSucesso("Venda excluída com sucesso.");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    }
  };

  return (
    <>
      <header>
        <div>
          <h1>Vendas</h1>
          <p>Registre e consulte as vendas do seu negócio.</p>
        </div>
      </header>

      <section className="panel page-panel">
        <div className="registration-panel">
          <h3>{editandoId ? "Editar venda" : "Registrar venda"}</h3>
          <form className="compact-form" onSubmit={editandoId ? atualizarVenda : registrarVenda}>
            <label>
              Produto
              <input value={produto} onChange={(event) => setProduto(event.target.value)} maxLength="100" required />
            </label>
            <label>
              Quantidade
              <input type="number" min="1" step="1" value={quantidade} onChange={(event) => setQuantidade(event.target.value)} required />
            </label>
            <label>
              Valor unitário
              <input type="number" min="0.01" step="0.01" value={valorUnitario} onChange={(event) => setValorUnitario(event.target.value)} required />
            </label>
            {Number(quantidade) > 0 && Number(valorUnitario) > 0 && (
              <div className="form-total">
                <span>Total</span>
                <strong>{formatCurrency(Number(quantidade) * Number(valorUnitario))}</strong>
              </div>
            )}
            <button className="primary-button" type="submit" disabled={salvando || !estabelecimentoId}>
              {salvando ? "Salvando..." : editandoId ? "Salvar alterações" : "Registrar venda"}
            </button>
            {editandoId && <button className="profile-back-button" type="button" onClick={cancelarEdicao}>Cancelar</button>}
          </form>
          {sucesso && <p role="status">{sucesso}</p>}
          {erro && <p className="error-message" role="alert">{erro}</p>}
        </div>

        <h3>Vendas registradas</h3>

        {carregando ? (
          <p>Carregando vendas...</p>
        ) : vendas.length === 0 ? (
          <p>Nenhuma venda registrada.</p>
        ) : (
          <div className="table-wrapper">
            <table className="responsive-table">
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Quantidade</th>
                  <th>Valor unitário</th>
                  <th>Total</th>
                  <th>Data e horário</th>
                  <th>Ações</th>
                </tr>
              </thead>

              <tbody>
                {vendas.map((venda) => (
                  <tr key={venda.id}>
                    <td data-label="Produto">{formatarNomeProduto(venda.produto)}</td>
                    <td data-label="Quantidade">{venda.quantidade}</td>
                    <td data-label="Valor unitário">{formatCurrency(venda.valorUnitario)}</td>
                    <td data-label="Total">{formatCurrency(venda.total)}</td>
                    <td data-label="Data e horário">{new Date(venda.dataHora).toLocaleString("pt-BR")}</td>
                    <td className="record-actions" data-label="Ações">
                      <button className="record-action" type="button" title="Editar venda" aria-label="Editar venda" onClick={() => iniciarEdicao(venda)}>
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m16 4 4 4M4 20l4-.8L19 8a2.8 2.8 0 0 0-4-4L4 15z" /></svg>
                      </button>
                      <button className="record-action danger" type="button" title="Excluir venda" aria-label="Excluir venda" onClick={() => excluirVenda(venda)}>
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
