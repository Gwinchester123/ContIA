import { useEffect, useState } from "react";
import { formatCurrency, getSessionUser } from "../data/financeStorage";

export default function Despesas() {
  const [despesas, setDespesas] = useState([]);
  const [estabelecimentoId, setEstabelecimentoId] = useState(null);
  const [descricao, setDescricao] = useState("");
  const [categoria, setCategoria] = useState("");
  const [valor, setValor] = useState("");
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
          descricao: descricao.trim(),
          categoria: categoria.trim(),
          valor: Number(valor),
        }),
      });

      if (!resposta.ok) {
        throw new Error((await resposta.text()) || "NÃ£o foi possÃ­vel registrar a despesa.");
      }

      const despesa = await resposta.json();
      setDespesas((atuais) => [despesa, ...atuais]);
      setDescricao("");
      setCategoria("");
      setValor("");
      setSucesso("Despesa registrada com sucesso.");
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
          <h1>Despesas</h1>
          <p>Registre e consulte os gastos do seu negÃ³cio.</p>
        </div>
      </header>

      <section className="panel page-panel">
        <div className="registration-panel">
          <h3>Registrar despesa</h3>
          <form className="compact-form" onSubmit={registrarDespesa}>
            <label>
              DescriÃ§Ã£o
              <input value={descricao} onChange={(event) => setDescricao(event.target.value)} required />
            </label>
            <label>
              Categoria
              <input value={categoria} onChange={(event) => setCategoria(event.target.value)} required />
            </label>
            <label>
              Valor
              <input type="number" min="0.01" step="0.01" value={valor} onChange={(event) => setValor(event.target.value)} required />
            </label>
            <button className="primary-button" type="submit" disabled={salvando || !estabelecimentoId}>
              {salvando ? "Salvando..." : "Registrar despesa"}
            </button>
          </form>
          {sucesso && <p role="status">{sucesso}</p>}
        </div>

        <h3>Despesas registradas</h3>

        {erro && <p className="error-message">{erro}</p>}

        {carregando ? (
          <p>Carregando despesas...</p>
        ) : despesas.length === 0 ? (
          <p>Nenhuma despesa registrada.</p>
        ) : (
          <div className="table-wrapper">
            <table>
              <thead>
                <tr>
                  <th>Descrição</th>
                  <th>Categoria</th>
                  <th>Valor</th>
                  <th>Data e horário</th>
                </tr>
              </thead>

              <tbody>
                {despesas.map((despesa) => (
                  <tr key={despesa.id}>
                    <td>{despesa.descricao}</td>
                    <td>{despesa.categoria}</td>
                    <td>{formatCurrency(despesa.valor)}</td>
                    <td>
                      {new Date(despesa.dataHora).toLocaleString("pt-BR")}
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
