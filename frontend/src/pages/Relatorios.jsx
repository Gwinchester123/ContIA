import { useEffect, useState } from "react";
import { formatCurrency, getSessionUser, getExpenseBreakdown } from "../data/financeStorage";
import { baixarRelatorioPdf, periodoAtual } from "../data/financialReports";

export default function Relatorios() {
  const [vendas, setVendas] = useState([]);
  const [despesas, setDespesas] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [estabelecimento, setEstabelecimento] = useState(null);
  const [periodo, setPeriodo] = useState("mensal");
  const [datas, setDatas] = useState(() => periodoAtual("mensal"));
  const [baixando, setBaixando] = useState(false);

  useEffect(() => {
    const proprietarioId = getSessionUser();

    if (!proprietarioId) {
      setErro("Usuário não identificado.");
      setCarregando(false);
      return;
    }

    async function carregarDados() {
      try {
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

        setEstabelecimento(estabelecimentoAtual);

        const id = estabelecimentoAtual.id;

        const [respostaVendas, respostaDespesas] = await Promise.all([
          fetch(`http://localhost:8080/api/vendas/estabelecimento/${id}`),
          fetch(`http://localhost:8080/api/despesas/estabelecimento/${id}`),
        ]);

        if (!respostaVendas.ok || !respostaDespesas.ok) {
          throw new Error("Não foi possível carregar os dados financeiros.");
        }

        const [dadosVendas, dadosDespesas] = await Promise.all([
          respostaVendas.json(),
          respostaDespesas.json(),
        ]);

        setVendas(dadosVendas);
        setDespesas(dadosDespesas);
      } catch (erro) {
        console.error("Erro ao carregar relatórios:", erro);
        setErro(erro.message);
      } finally {
        setCarregando(false);
      }
    }

    carregarDados();
  }, []);

  const atualizarPeriodo = (tipo) => {
    setPeriodo(tipo);
    if (tipo !== "personalizado") setDatas(periodoAtual(tipo));
  };

  const baixarPdf = async (completo) => {
    if (!estabelecimento || !datas.inicio || !datas.fim) return;
    setBaixando(true);
    setErro("");
    try {
      await baixarRelatorioPdf({
        proprietarioId: getSessionUser(),
        estabelecimentoId: estabelecimento.id,
        inicio: datas.inicio,
        fim: datas.fim,
        completo,
      });
    } catch (erroDownload) {
      setErro(erroDownload.message);
    } finally {
      setBaixando(false);
    }
  };

  const { compras, outras } = getExpenseBreakdown(despesas);

  const faturamento = vendas.reduce(
    (total, venda) => total + Number(venda.total || 0),
    0,
  );

  const totalDespesas = despesas.reduce(
    (total, despesa) => total + Number(despesa.valor || 0),
    0,
  );

  const saldo = faturamento - totalDespesas;

  const categorias = despesas.reduce((agrupadas, despesa) => {
    const atual = agrupadas.find(
      (item) => item.categoria === despesa.categoria,
    );

    if (atual) {
      atual.valor += Number(despesa.valor || 0);
    } else {
      agrupadas.push({
        categoria: despesa.categoria,
        valor: Number(despesa.valor || 0),
      });
    }

    return agrupadas;
  }, []);

  if (carregando) {
    return <p>Carregando relatórios...</p>;
  }

  return (
    <>
      <header>
        <div>
          <h1>Relatórios</h1>
          <p>Resumo financeiro simples do seu negócio.</p>
        </div>
      </header>

      <section className="panel report-period-panel">
        <div className="report-controls">
          <label>Período do PDF
            <select value={periodo} onChange={(event) => atualizarPeriodo(event.target.value)}>
              <option value="diario">Diário</option>
              <option value="semanal">Semanal</option>
              <option value="mensal">Mensal</option>
              <option value="personalizado">Personalizado</option>
            </select>
          </label>
          {periodo === "personalizado" && <>
            <label>De<input type="date" value={datas.inicio} onChange={(event) => setDatas((atual) => ({ ...atual, inicio: event.target.value }))} /></label>
            <label>Até<input type="date" value={datas.fim} onChange={(event) => setDatas((atual) => ({ ...atual, fim: event.target.value }))} /></label>
          </>}
          <button className="primary-button" type="button" onClick={() => baixarPdf(false)} disabled={baixando || !estabelecimento || !datas.inicio || !datas.fim || datas.inicio > datas.fim}>
            {baixando ? "Gerando PDF..." : "Baixar PDF essencial"}
          </button>
          <button className="secondary-button" type="button" onClick={() => baixarPdf(true)} disabled={baixando || !estabelecimento || !datas.inicio || !datas.fim || datas.inicio > datas.fim}>
            Baixar PDF completo
          </button>
        </div>
      </section>

      {erro && <p className="error-message">{erro}</p>}

      <section className="cards reports-cards">
        <div className="card">
          <div className="card-title">
            <span>Total de vendas</span>
          </div>
          <h2>{vendas.length}</h2>
        </div>

        <div className="card">
          <div className="card-title">
            <span>Total de despesas</span>
          </div>
          <h2>{formatCurrency(totalDespesas)}</h2>
        </div>

        <div className="card">
          <div className="card-title">
            <span>Compras e custos</span>
          </div>
          <h2>{formatCurrency(compras)}</h2>
        </div>

        <div className="card">
          <div className="card-title">
            <span>Outras despesas</span>
          </div>
          <h2>{formatCurrency(outras)}</h2>
        </div>

        <div className="card">
          <div className="card-title">
            <span>Faturamento</span>
          </div>
          <h2>{formatCurrency(faturamento)}</h2>
        </div>

        <div className="card">
          <div className="card-title">
            <span>Saldo financeiro estimado</span>
          </div>
          <h2>{formatCurrency(saldo)}</h2>
        </div>
      </section>

      <section className="content reports-content">
        <div className="panel">
          <h3>Faturamento por período</h3>
          <div className="report-lines">
            <div>
              <span>Total registrado</span>
              <strong>{formatCurrency(faturamento)}</strong>
            </div>
          </div>
        </div>

        <div className="panel">
          <h3>Despesas por categoria</h3>
          <div className="table-wrapper">
            <table className="responsive-table">
              <thead>
                <tr>
                  <th>Categoria</th>
                  <th>Valor</th>
                </tr>
              </thead>
              <tbody>
                {categorias.map((item) => (
                  <tr key={item.categoria}>
                    <td data-label="Categoria">{item.categoria}</td>
                    <td data-label="Valor">{formatCurrency(item.valor)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </section>
    </>
  );
}
