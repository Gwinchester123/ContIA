import { useEffect, useMemo, useState } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { apiFetch, formatCurrency, getSessionUser } from "../data/financeStorage";
import {
  baixarRelatorioPdf,
  montarUrlRelatorio,
  obterEstabelecimentosProprietario,
  periodoAtual,
} from "../data/financialReports";

const opcoesPeriodo = [
  { valor: "diario", texto: "Hoje" },
  { valor: "semanal", texto: "Esta semana" },
  { valor: "mensal", texto: "Este mês" },
  { valor: "personalizado", texto: "Personalizado" },
];

const formatarDataCurta = (data) => data ? `${data.slice(8, 10)}/${data.slice(5, 7)}` : "";
const formatarMoedaCompacta = (valor) => new Intl.NumberFormat("pt-BR", {
  style: "currency",
  currency: "BRL",
  notation: "compact",
  maximumFractionDigits: 1,
}).format(Number(valor || 0));

function Icone({ tipo, className = "" }) {
  const caminhos = {
    vendas: <><path d="M4 18V9" /><path d="M10 18V5" /><path d="M16 18v-6" /><path d="M22 18V3" /></>,
    despesas: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18" /><path d="M16 15h2" /></>,
    resultado: <><path d="M4 17 9 12l4 3 7-8" /><path d="M15 7h5v5" /></>,
    alerta: <><path d="M12 3 2.8 20h18.4L12 3Z" /><path d="M12 9v4m0 3h.01" /></>,
    ideia: <><path d="M9 18h6m-5 3h4m-2-18a7 7 0 0 0-4 12.7c.7.5 1 1.1 1 2.3h6c0-1.2.3-1.8 1-2.3A7 7 0 0 0 12 3Z" /></>,
  };
  return <svg className={className} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{caminhos[tipo]}</svg>;
}

function variacao(atual, anterior) {
  const base = Number(anterior || 0);
  const valorAtual = Number(atual || 0);
  if (base === 0) return valorAtual === 0 ? "Sem movimentação" : "Novo movimento";
  const porcentagem = ((valorAtual - base) / Math.abs(base)) * 100;
  const seta = porcentagem > 0 ? "↑" : porcentagem < 0 ? "↓" : "→";
  return `${seta} ${Math.abs(porcentagem).toFixed(1).replace(".", ",")}%`;
}

function resumirObservacao(texto) {
  const gastoCategoria = texto.match(/Os gastos com (.+) aumentaram mais de 10%/i);
  if (gastoCategoria) return `${gastoCategoria[1]} teve aumento acima de 10%.`;
  if (/vendas caíram mais de 10%/i.test(texto)) return "Vendas caíram mais de 10% ante o período anterior.";
  if (/despesas aumentaram mais de 10%/i.test(texto)) return "Despesas aumentaram mais de 10% ante o período anterior.";
  const margem = texto.match(/A margem de resultado (subiu|caiu) ([\d,.]+) pontos percentuais/i);
  if (margem) return `Margem ${margem[1]} ${margem[2]} p.p.`;
  const categoria = texto.match(/Avalie os gastos registrados em (.+), que concentrou ([\d,.]+)%/i);
  if (categoria) return `${categoria[1]} concentra ${categoria[2]}% das despesas.`;
  if (/vendas registradas aumentaram/i.test(texto)) return "Vendas acima do período anterior.";
  return texto;
}

function Comparacao({ analise }) {
  const indicadores = [
    { nome: "Vendas", atual: analise.faturamento, anterior: analise.faturamentoAnterior, favoravel: true, icone: "vendas" },
    { nome: "Despesas", atual: analise.despesas, anterior: analise.despesasAnteriores, favoravel: false, icone: "despesas" },
    { nome: "Resultado", atual: analise.resultado, anterior: analise.resultadoAnterior, favoravel: true, icone: "resultado" },
  ];
  return (
    <div className="if-comparison-grid">
      {indicadores.map(({ nome, atual, anterior, favoravel, icone }) => {
        const diferenca = Number(atual || 0) - Number(anterior || 0);
        const tom = diferenca === 0 ? "neutral" : (diferenca > 0) === favoravel ? "positive" : "negative";
        return (
          <article className="if-comparison-card" key={nome}>
            <div className="if-comparison-top"><span className={`if-icon-badge ${icone}`}><Icone tipo={icone} /></span><span className="if-comparison-label">{nome}</span>
              <span className={`if-change ${tom}`}>{analise.temComparacao ? variacao(atual, anterior) : "Sem histórico"}</span>
            </div>
            <strong>{formatCurrency(atual)}</strong>
            <span className="if-comparison-base">Anterior: {analise.temComparacao ? formatCurrency(anterior) : "sem dados"}</span>
          </article>
        );
      })}
    </div>
  );
}

export default function InteligenciaFinanceira() {
  const proprietarioId = getSessionUser();
  const [periodo, setPeriodo] = useState("mensal");
  const [datas, setDatas] = useState(() => periodoAtual("mensal"));
  const [estabelecimentos, setEstabelecimentos] = useState([]);
  const [estabelecimentoId, setEstabelecimentoId] = useState("");
  const [resultadoCarregado, setResultadoCarregado] = useState(null);
  const [erro, setErro] = useState("");
  const [carregandoEstabelecimentos, setCarregandoEstabelecimentos] = useState(true);
  const [baixandoPdf, setBaixandoPdf] = useState(false);
  const chaveAnalise = `${estabelecimentoId}|${datas.inicio}|${datas.fim}`;
  const analise = resultadoCarregado?.chave === chaveAnalise ? resultadoCarregado.dados : null;
  const periodoValido = Boolean(datas.inicio && datas.fim && datas.inicio <= datas.fim);

  useEffect(() => {
    let ativo = true;
    obterEstabelecimentosProprietario()
      .then(({ estabelecimentos: dados }) => {
        if (!ativo) return;
        const premium = dados.filter((item) => item.plano === "PREMIUM");
        setEstabelecimentos(premium);
        if (premium.length) setEstabelecimentoId(String(premium[0].id));
        else setErro("A Inteligência Financeira está disponível apenas no plano Premium.");
      })
      .catch((error) => { if (ativo) setErro(error.message); })
      .finally(() => { if (ativo) setCarregandoEstabelecimentos(false); });
    return () => { ativo = false; };
  }, []);

  useEffect(() => {
    if (!estabelecimentoId || !periodoValido) return undefined;
    let ativo = true;
    const url = montarUrlRelatorio("analise", proprietarioId, estabelecimentoId, datas.inicio, datas.fim);
    apiFetch(url)
      .then(async (resposta) => {
        if (!resposta.ok) throw new Error("Não foi possível carregar esta análise.");
        return resposta.json();
      })
      .then((dados) => { if (ativo) setResultadoCarregado({ chave: chaveAnalise, dados }); })
      .catch((error) => { if (ativo) setErro(error.message); });
    return () => { ativo = false; };
  }, [chaveAnalise, datas.fim, datas.inicio, estabelecimentoId, periodoValido, proprietarioId]);

  const observacoes = useMemo(() => {
    const alertas = (analise?.alertas || []).map((texto) => ({ texto: resumirObservacao(texto), origem: "alerta" }));
    const recomendacoes = (analise?.recomendacoes || []).map((texto) => ({ texto: resumirObservacao(texto), origem: "recomendacao" }));
    const unicas = [...new Map([...alertas, ...recomendacoes].map((item) => [item.texto, item])).values()];
    return unicas.slice(0, 3).map((item) => ({ ...item, tom: item.origem === "alerta" || /caiu|aumentaram|concentra/i.test(item.texto) ? "warning" : "positive" }));
  }, [analise]);

  const atualizarPeriodo = (tipo) => {
    setErro("");
    setPeriodo(tipo);
    if (tipo !== "personalizado") setDatas(periodoAtual(tipo));
  };

  const baixarPdf = async () => {
    if (!estabelecimentoId || !periodoValido) return;
    setBaixandoPdf(true);
    setErro("");
    try {
      await baixarRelatorioPdf({ proprietarioId, estabelecimentoId, inicio: datas.inicio, fim: datas.fim });
    } catch (error) {
      setErro(error.message);
    } finally {
      setBaixandoPdf(false);
    }
  };

  const carregando = carregandoEstabelecimentos || Boolean(estabelecimentoId && periodoValido && !analise && !erro);
  const estabelecimentoAtual = estabelecimentos.find((item) => String(item.id) === estabelecimentoId);
  const compararGrafico = Boolean(analise?.evolucao?.length);

  return (
    <div className="intelligence-page if-page">
      <header className="if-header">
        <div className="if-heading-copy"><span className="if-eyebrow">ANÁLISE DO NEGÓCIO</span><h1>Inteligência Financeira</h1>
          <span className="if-establishment-name">{estabelecimentoAtual?.nome || "Selecione um estabelecimento"}</span>
        </div>
        <div className="if-toolbar">
          {estabelecimentos.length > 1 && <label className="if-select-field"><span>Estabelecimento</span><select value={estabelecimentoId} onChange={(event) => { setErro(""); setEstabelecimentoId(event.target.value); }} aria-label="Selecionar estabelecimento">
            {estabelecimentos.map((item) => <option key={item.id} value={item.id}>{item.nome}</option>)}
          </select></label>}
          <label className="if-select-field"><span>Período</span><select value={periodo} onChange={(event) => atualizarPeriodo(event.target.value)} aria-label="Selecionar período">
            {opcoesPeriodo.map((opcao) => <option key={opcao.valor} value={opcao.valor}>{opcao.texto}</option>)}
          </select></label>
          {periodo === "personalizado" && <div className="if-date-fields">
            <label><span>De</span><input type="date" value={datas.inicio} onChange={(event) => { setErro(""); setDatas((atual) => ({ ...atual, inicio: event.target.value })); }} /></label>
            <label><span>Até</span><input type="date" value={datas.fim} onChange={(event) => { setErro(""); setDatas((atual) => ({ ...atual, fim: event.target.value })); }} /></label>
          </div>}
          <button className="if-pdf-button" type="button" onClick={baixarPdf} disabled={baixandoPdf || carregando || !periodoValido || !estabelecimentoId}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 3v12m0 0 4-4m-4 4-4-4M5 17v3h14v-3" /></svg>
            {baixandoPdf ? "Gerando PDF..." : "Baixar relatório PDF"}
          </button>
        </div>
      </header>

      {!periodoValido && <p className="error-message" role="alert">Selecione um intervalo válido de datas.</p>}
      {erro && <p className="error-message" role="alert">{erro}</p>}
      {carregando && <div className="if-loading" role="status"><span className="if-loading-dot" />Atualizando a análise...</div>}

      {!carregando && analise && <>
        <section className="if-kpis" aria-label="Indicadores principais">
          <article className="if-kpi if-kpi-sales"><div className="if-kpi-top"><span>Total de vendas</span><span className="if-icon-badge sales"><Icone tipo="vendas" /></span></div><strong>{formatCurrency(analise.faturamento)}</strong><small>No período selecionado</small></article>
          <article className="if-kpi if-kpi-expenses"><div className="if-kpi-top"><span>Total de despesas</span><span className="if-icon-badge expenses"><Icone tipo="despesas" /></span></div><strong>{formatCurrency(analise.despesas)}</strong><small>No período selecionado</small></article>
          <article className="if-kpi if-kpi-result"><div className="if-kpi-top"><span>Resultado estimado</span><span className="if-icon-badge result"><Icone tipo="resultado" /></span></div><strong>{formatCurrency(analise.resultado)}</strong><small>Vendas menos despesas registradas; não é lucro líquido contábil.</small></article>
        </section>

        <section className="if-main-grid">
          <article className="if-card if-chart-card">
            <div className="if-card-heading"><div><span className="if-eyebrow">EVOLUÇÃO NO PERÍODO</span><h2>Vendas e despesas</h2></div><span className="if-period-chip">{datas.inicio} – {datas.fim}</span></div>
            {compararGrafico ? <div className="if-chart">
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={analise.evolucao} margin={{ top: 12, right: 8, left: 2, bottom: 0 }}>
                  <defs>
                    <linearGradient id="ifSalesFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#1e6646" stopOpacity={0.2} /><stop offset="100%" stopColor="#1e6646" stopOpacity={0.015} /></linearGradient>
                    <linearGradient id="ifExpensesFill" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#d5867d" stopOpacity={0.16} /><stop offset="100%" stopColor="#d5867d" stopOpacity={0.015} /></linearGradient>
                  </defs>
                  <CartesianGrid vertical={false} stroke="#edf1ed" strokeDasharray="4 5" />
                  <XAxis dataKey="data" tickFormatter={formatarDataCurta} tickLine={false} axisLine={false} minTickGap={18} tick={{ fill: "#7e8a81", fontSize: 11 }} />
                  <YAxis tickFormatter={formatarMoedaCompacta} tickLine={false} axisLine={false} width={78} tick={{ fill: "#7e8a81", fontSize: 10 }} />
                  <Tooltip labelFormatter={(label) => `Data: ${formatarDataCurta(String(label))}`} formatter={(value) => formatCurrency(Number(value))} contentStyle={{ borderRadius: 10, border: "1px solid #e5ebe6", boxShadow: "0 8px 24px rgba(24,60,50,.09)" }} />
                  <Legend verticalAlign="top" align="right" iconType="circle" iconSize={8} wrapperStyle={{ paddingBottom: 12, fontSize: 12, color: "#526257" }} />
                  <Area type="monotone" dataKey="vendas" name="Vendas" stroke="#1e6646" strokeWidth={2.5} fill="url(#ifSalesFill)" activeDot={{ r: 4, strokeWidth: 0 }} />
                  <Area type="monotone" dataKey="despesas" name="Despesas" stroke="#c97469" strokeWidth={2} fill="url(#ifExpensesFill)" activeDot={{ r: 4, strokeWidth: 0 }} />
                </AreaChart>
              </ResponsiveContainer>
            </div> : <div className="if-empty-state">Sem movimentações neste período.</div>}
          </article>

          <article className="if-card if-comparison-card-panel">
            <div className="if-card-heading"><div><span className="if-eyebrow">DESEMPENHO</span><h2>Comparação anterior</h2></div></div>
            {analise.temComparacao ? <Comparacao analise={analise} /> : <div className="if-empty-state">Ainda não há dados suficientes do período anterior para comparar.</div>}
          </article>
        </section>

        <section className="if-card if-insights-card">
          <div className="if-card-heading"><div><span className="if-eyebrow">LEITURA DOS REGISTROS</span><h2>Alertas e recomendações</h2></div>
            {observacoes.length > 0 && <span className="if-insight-count">{observacoes.length} {observacoes.length === 1 ? "aviso" : "avisos"}</span>}
          </div>
          {observacoes.length > 0 ? <div className="if-insights-grid">
            {observacoes.map((item, index) => <article className={`if-insight ${item.tom}`} key={`${index}-${item.texto}`}>
              <span className="if-insight-icon"><Icone tipo={item.tom === "warning" ? "alerta" : "ideia"} /></span>
              <div><strong>{item.tom === "warning" ? "Atenção" : "Recomendação"}</strong><p>{item.texto}</p></div>
            </article>)}
          </div> : <div className="if-insights-empty"><span className="if-icon-badge result"><Icone tipo="ideia" /></span><p>{analise.quantidadeVendas + analise.quantidadeDespesas || analise.temComparacao ? "Nenhuma mudança relevante identificada nos dados registrados." : "Ainda não há dados suficientes para recomendações."}</p></div>}
        </section>
      </>}
    </div>
  );
}
