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
        throw new Error((await resposta.text()) || "NÃ£o foi possÃ­vel registrar a venda.");
      }

      const venda = await resposta.json();
      setVendas((atuais) => [venda, ...atuais]);
      setProduto("");
      setQuantidade("");
      setValorUnitario("");
      setSucesso("Venda registrada com sucesso.");
    } catch (error) {
      setErro(error.message || "NÃ£o foi possÃ­vel conectar ao servidor.");
    } finally {
      setSalvando(false);
    }
  };

  return (
    <>
      <header>
        <div>
          <h1>Vendas</h1>
          <p>Registre e consulte as vendas do seu negÃ³cio.</p>
        </div>
      </header>

      <section className="panel page-panel">
        <div className="registration-panel">
          <h3>Registrar venda</h3>
          <form className="compact-form" onSubmit={registrarVenda}>
            <label>
              Produto
              <input value={produto} onChange={(event) => setProduto(event.target.value)} required />
            </label>
            <label>
              Quantidade
              <input type="number" min="1" step="1" value={quantidade} onChange={(event) => setQuantidade(event.target.value)} required />
            </label>
            <label>
              Valor unitÃ¡rio
              <input type="number" min="0.01" step="0.01" value={valorUnitario} onChange={(event) => setValorUnitario(event.target.value)} required />
            </label>
            <button className="primary-button" type="submit" disabled={salvando || !estabelecimentoId}>
              {salvando ? "Salvando..." : "Registrar venda"}
            </button>
          </form>
          {sucesso && <p role="status">{sucesso}</p>}
        </div>

        <h3>Vendas registradas</h3>

        {erro && <p className="error-message">{erro}</p>}

        {carregando ? (
          <p>Carregando vendas...</p>
        ) : vendas.length === 0 ? (
          <p>Nenhuma venda registrada.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Produto</th>
                  <th>Quantidade</th>
                  <th>Valor unitário</th>
                  <th>Total</th>
                  <th>Data e horário</th>
                </tr>
              </thead>

              <tbody>
                {vendas.map((venda) => (
                  <tr key={venda.id}>
                    <td>{formatarNomeProduto(venda.produto)}</td>
                    <td>{venda.quantidade}</td>
                    <td>{formatCurrency(venda.valorUnitario)}</td>
                    <td>{formatCurrency(venda.total)}</td>
                    <td>{new Date(venda.dataHora).toLocaleString("pt-BR")}</td>
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
