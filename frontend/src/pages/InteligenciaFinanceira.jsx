import { useEffect, useMemo, useState } from "react";
import { formatCurrency, isPurchaseOrCost } from "../data/financeStorage";
import {
  montarUrlRelatorio,
  obterEstabelecimentoAtual,
  periodoAtual,
} from "../data/financialReports";

const opcoes = [
  { valor: "diario", texto: "Hoje" },
  { valor: "semanal", texto: "Esta semana" },
  { valor: "mensal", texto: "Este mês" },
  { valor: "personalizado", texto: "Personalizado" },
];

export default function InteligenciaFinanceira() {
  const [periodo, setPeriodo] = useState("mensal");
  const [datas, setDatas] = useState(() => periodoAtual("mensal"));
  const [analise, setAnalise] = useState(null);
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(true);

  useEffect(() => {
    if (!datas.inicio || !datas.fim) return;
    let ativo = true;
    obterEstabelecimentoAtual()
      .then(({ proprietarioId, estabelecimento }) => {
        const url = montarUrlRelatorio(
          "analise",
          proprietarioId,
          estabelecimento.id,
          datas.inicio,
          datas.fim,
        );
        return fetch(url);
      })
      .then(async (resposta) => {
        if (!resposta.ok) {
          throw new Error("Não foi possível calcular as análises deste período.");
        }
        return resposta.json();
      })
      .then((dados) => {
        if (ativo) {
          setAnalise(dados);
          setErro("");
        }
      })
      .catch((e) => {
        if (ativo) setErro(e.message);
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [datas]);

  const categorias = useMemo(
    () =>
      Object.entries(analise?.categorias || {}).sort(
        (a, b) => Number(b[1]) - Number(a[1]),
      ),
    [analise],
  );
  const atualizarTipo = (tipo) => {
    if (tipo !== "personalizado") setCarregando(true);
    setPeriodo(tipo);
    if (tipo !== "personalizado") setDatas(periodoAtual(tipo));
  };
  const observacoes = [
    ...(analise?.alertas || []).map((texto) => ({ texto, tipo: "alerta" })),
    ...(analise?.recomendacoes || []).map((texto) => ({ texto, tipo: "recomendacao" })),
  ];

  return (
    <div className="intelligence-page">
      <header className="intelligence-header">
        <div>
          <span className="eyebrow">VISÃO DO NEGÓCIO</span>
          <h1>Inteligência Financeira</h1>
          <p>Informações práticas com base nos lançamentos do seu estabelecimento.</p>
        </div>
        <span className="intelligence-live"><i /> Dados atualizados</span>
      </header>

      <section className="panel intelligence-period-panel" aria-label="Filtro do período">
        <div className="intelligence-period-copy">
          <strong>Período analisado</strong>
          <span>{datas.inicio} — {datas.fim}</span>
        </div>
        <div className="intelligence-period-controls">
          <label className="sr-only" htmlFor="intelligence-period">Selecionar período</label>
          <select id="intelligence-period" value={periodo} onChange={(e) => atualizarTipo(e.target.value)}>
            {opcoes.map((opcao) => <option key={opcao.valor} value={opcao.valor}>{opcao.texto}</option>)}
          </select>
          {periodo === "personalizado" && <>
            <label className="sr-only" htmlFor="intelligence-start">Data inicial</label>
            <input id="intelligence-start" type="date" value={datas.inicio} onChange={(e) => { setCarregando(Boolean(e.target.value && datas.fim)); setDatas((atual) => ({ ...atual, inicio: e.target.value })); }} />
            <span className="date-separator">até</span>
            <label className="sr-only" htmlFor="intelligence-end">Data final</label>
            <input id="intelligence-end" type="date" value={datas.fim} onChange={(e) => { setCarregando(Boolean(datas.inicio && e.target.value)); setDatas((atual) => ({ ...atual, fim: e.target.value })); }} />
          </>}
        </div>
      </section>

      {erro && <p className="error-message" role="alert">{erro}</p>}
      {carregando && <div className="intelligence-loading" role="status">Atualizando sua análise…</div>}

      {!carregando && analise && <>
        <section className="intelligence-kpis" aria-label="Resumo financeiro">
          <article className="intelligence-kpi kpi-sales">
            <div className="intelligence-kpi-heading"><span>Vendas</span><span className="kpi-symbol">↗</span></div>
            <strong>{formatCurrency(analise.faturamento)}</strong>
            <small>Total vendido no período</small>
          </article>
          <article className="intelligence-kpi kpi-expenses">
            <div className="intelligence-kpi-heading"><span>Despesas</span><span className="kpi-symbol">↘</span></div>
            <strong>{formatCurrency(analise.despesas)}</strong>
            <small>Custos diretos e despesas operacionais</small>
          </article>
          <article className="intelligence-kpi kpi-result">
            <div className="intelligence-kpi-heading"><span>Resultado financeiro</span><span className="kpi-symbol">＝</span></div>
            <strong>{formatCurrency(analise.resultado)}</strong>
            <small>Vendas menos despesas; não é lucro contábil</small>
          </article>
        </section>

        <section className="intelligence-primary-grid">
          <article className="panel intelligence-panel category-panel">
            <div className="intelligence-panel-heading">
              <div><span className="eyebrow">PARA ONDE VAI O DINHEIRO</span><h2>Despesas por categoria</h2></div>
              <span className="panel-total">{formatCurrency(analise.despesas)}</span>
            </div>
            {categorias.length ? <div className="intelligence-bars">
              {categorias.map(([categoria, valor]) => {
                const participacao = analise.despesas > 0 ? Number(valor) / Number(analise.despesas) * 100 : 0;
                return <div className="intelligence-bar-row" key={categoria}>
                  <div className="intelligence-bar-label">
                    <span><i className={isPurchaseOrCost({ categoria }) ? "category-dot direct-cost" : "category-dot operating-cost"} />{categoria}</span>
                    <strong>{formatCurrency(Number(valor))}</strong>
                  </div>
                  <div className="intelligence-bar-track"><span style={{ width: `${Math.min(participacao, 100)}%` }} /></div>
                  <div className="intelligence-bar-meta"><span>{isPurchaseOrCost({ categoria }) ? "Custo direto" : "Despesa operacional"}</span><span>{participacao.toFixed(1)}%</span></div>
                </div>;
              })}
            </div> : <div className="intelligence-empty">Nenhuma despesa registrada neste período.</div>}
            <div className="category-legend"><span><i className="category-dot direct-cost" /> Custos diretos</span><span><i className="category-dot operating-cost" /> Operacionais</span></div>
          </article>

          <article className="panel intelligence-panel comparison-panel">
            <div className="intelligence-panel-heading">
              <div><span className="eyebrow">EVOLUÇÃO</span><h2>Comparação de períodos</h2></div>
              <span className="comparison-mark">↗</span>
            </div>
            {analise.temComparacao ? <>
              <p className="comparison-caption">Período anterior com a mesma duração</p>
              <div className="comparison-values">
                <div><span>Vendas atuais</span><strong>{formatCurrency(analise.faturamento)}</strong></div>
                <div><span>Vendas anteriores</span><strong>{formatCurrency(analise.faturamentoAnterior)}</strong></div>
                <div><span>Despesas atuais</span><strong>{formatCurrency(analise.despesas)}</strong></div>
                <div><span>Despesas anteriores</span><strong>{formatCurrency(analise.despesasAnteriores)}</strong></div>
              </div>
            </> : <div className="intelligence-empty">Ainda não há lançamentos anteriores suficientes para comparar.</div>}
          </article>
        </section>

        <section className="intelligence-secondary-grid">
          <article className="panel intelligence-panel insights-panel">
            <div className="intelligence-panel-heading">
              <div><span className="eyebrow">LEITURA DOS DADOS</span><h2>Alertas e recomendações</h2></div>
              <span className="insight-count">{observacoes.length}</span>
            </div>
            {observacoes.length ? <div className="insight-list">
              {observacoes.map(({ texto, tipo }, index) => <div className={`insight-item ${tipo}`} key={`${tipo}-${index}`}>
                <span className="insight-marker">{tipo === "alerta" ? "!" : "✓"}</span>
                <p>{texto}</p>
              </div>)}
            </div> : <div className="intelligence-empty">Sem alertas no momento. Os dados do período não indicam variações relevantes.</div>}
          </article>

          <article className="panel intelligence-panel days-panel">
            <div className="intelligence-panel-heading">
              <div><span className="eyebrow">RITMO DE VENDAS</span><h2>Faturamento por dia</h2></div>
              <span className="comparison-mark">▥</span>
            </div>
            {analise.diaMaiorFaturamento ? <div className="day-highlights">
              <div className="day-highlight best-day"><span>Maior faturamento</span><strong>{analise.diaMaiorFaturamento.dia}</strong><b>{formatCurrency(analise.diaMaiorFaturamento.valor)}</b></div>
              <div className="day-highlight lowest-day"><span>Menor faturamento</span><strong>{analise.diaMenorFaturamento.dia}</strong><b>{formatCurrency(analise.diaMenorFaturamento.valor)}</b></div>
            </div> : <div className="intelligence-empty">Registre vendas em pelo menos dois dias para identificar os maiores e menores faturamentos.</div>}
          </article>
        </section>
      </>}
    </div>
  );
}
