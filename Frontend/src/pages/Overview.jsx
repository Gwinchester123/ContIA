import { useEffect, useState } from "react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import {
  formatCurrency,
  apiFetch,
  getSessionUser,
  getExpenseBreakdown,
} from "../data/financeStorage";
import { apiUrl } from "../data/api";

const formatarMoeda = (valor) =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    maximumFractionDigits: 0,
  }).format(valor);

const Icon = ({ children, size = 18 }) => (
  <svg
    width={size}
    height={size}
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    strokeWidth="1.8"
    strokeLinecap="round"
    strokeLinejoin="round"
    aria-hidden="true"
  >
    {children}
  </svg>
);

export default function Overview() {
  const [estabelecimento, setEstabelecimento] = useState(null);

  const [vendas, setVendas] = useState([]);
  const [despesas, setDespesas] = useState([]);
  const [carregandoFinanceiro, setCarregandoFinanceiro] = useState(true);
  const [erroFinanceiro, setErroFinanceiro] = useState("");

  useEffect(() => {
    const proprietarioId = getSessionUser();

    if (!proprietarioId) {
      setCarregandoFinanceiro(false);
      return;
    }

    async function carregarDados() {
      try {
        const respostaEstabelecimento = await apiFetch(
          apiUrl(`/estabelecimentos/proprietario/${proprietarioId}`),
        );

        if (!respostaEstabelecimento.ok) {
          throw new Error("Não foi possível buscar o estabelecimento.");
        }

        const estabelecimentos = await respostaEstabelecimento.json();
        const estabelecimentoAtual = estabelecimentos[0] || null;

        setEstabelecimento(estabelecimentoAtual);

        if (!estabelecimentoAtual) {
          throw new Error("Nenhum estabelecimento encontrado.");
        }

        const id = estabelecimentoAtual.id;

        const [respostaVendas, respostaDespesas] = await Promise.all([
          apiFetch(apiUrl(`/vendas/estabelecimento/${id}`)),
          apiFetch(apiUrl(`/despesas/estabelecimento/${id}`)),
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
        console.error("Erro ao carregar dados financeiros:", erro);
        setErroFinanceiro(erro.message);
      } finally {
        setCarregandoFinanceiro(false);
      }
    }

    carregarDados();
  }, []);

  const { compras, outras } = getExpenseBreakdown(despesas);

  const faturamento = vendas.reduce(
    (total, venda) => total + Number(venda.total || 0),
    0,
  );

  const resumo = {
    faturamento,
    compras,
    outras,
    saldo: faturamento - compras - outras,
  };

  const faturamentoPorDia = new Map();
  vendas.forEach((venda) => {
    const chave = String(venda.dataHora).slice(0, 10);
    if (!/^\d{4}-\d{2}-\d{2}$/.test(chave)) return;
    faturamentoPorDia.set(
      chave,
      (faturamentoPorDia.get(chave) || 0) + Number(venda.total || 0),
    );
  });
  const faturamentoHistorico = [...faturamentoPorDia.entries()]
    .sort(([dataA], [dataB]) => dataA.localeCompare(dataB))
    .reduce(
      (historico, [chave, faturamentoDoDia]) => {
        const faturamentoAcumulado =
          historico.totalAcumulado + faturamentoDoDia;
        const data = new Date(`${chave}T12:00:00`);

        historico.pontos.push({
          dia: data.toLocaleDateString("pt-BR", {
            day: "2-digit",
            month: "2-digit",
          }),
          faturamento: faturamentoAcumulado,
        });
        historico.totalAcumulado = faturamentoAcumulado;
        return historico;
      },
      { pontos: [], totalAcumulado: 0 },
    ).pontos;
  const despesasRecentes = [...despesas]
    .sort((a, b) => String(b.dataHora).localeCompare(String(a.dataHora)))
    .slice(0, 6);

  return (
    <>
      <header>
        <div>
          <h1>Visão geral</h1>
          <p>
            {estabelecimento
              ? `Acompanhe os resultados de ${estabelecimento.nome}.`
              : "Acompanhe os resultados do seu negócio."}
          </p>
        </div>
      </header>
      <section className="cards">
        <div className="card">
          <div className="card-title">
            <span>Faturamento total</span>
            <div className="card-icon">
              <Icon size={16}>
                <path d="M12 2v20" />
                <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" />
              </Icon>
            </div>
          </div>
          <h2>{formatCurrency(resumo.faturamento)}</h2>
        </div>
        <div className="card">
          <div className="card-title">
            <span>Compras e custos</span>
            <div className="card-icon">
              <Icon size={16}>
                <path d="M20 12a8 8 0 1 1-2.34-5.66" />
                <path d="M20 4v8h-8" />
              </Icon>
            </div>
          </div>
          <h2>{formatCurrency(resumo.compras)}</h2>
        </div>
        <div className="card">
          <div className="card-title">
            <span>Outras despesas</span>
            <div className="card-icon">
              <Icon size={16}>
                <rect x="3" y="5" width="18" height="14" rx="2" />
                <path d="M3 10h18" />
              </Icon>
            </div>
          </div>
          <h2>{formatCurrency(resumo.outras)}</h2>
        </div>
        <div className="card">
          <div className="card-title">
            <span>Saldo financeiro estimado</span>
            <div className="card-icon">
              <Icon size={16}>
                <path d="m3 17 6-6 4 4 8-8" />
                <path d="M15 7h6v6" />
              </Icon>
            </div>
          </div>
          <h2>{formatCurrency(resumo.saldo)}</h2>
        </div>
      </section>
      <section className="content">
        <div className="panel sales-panel">
          <div className="panel-header">
            <div>
              <h3>Faturamento</h3>
              <p>Histórico acumulado por data</p>
            </div>
          </div>
          <div
            className="sales-chart"
            aria-label="Gráfico do histórico acumulado de faturamento por data"
          >
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart
                data={faturamentoHistorico}
                margin={{ top: 10, right: 8, left: -12, bottom: 0 }}
              >
                <defs>
                  <linearGradient
                    id="faturamentoGradient"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >
                    <stop offset="0%" stopColor="#6fa88e" stopOpacity={0.35} />
                    <stop
                      offset="100%"
                      stopColor="#6fa88e"
                      stopOpacity={0.03}
                    />
                  </linearGradient>
                </defs>
                <CartesianGrid
                  stroke="#edf0ed"
                  strokeDasharray="3 3"
                  vertical={false}
                />
                <XAxis
                  dataKey="dia"
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#858b86", fontSize: 12 }}
                  dy={8}
                />
                <YAxis
                  axisLine={false}
                  tickLine={false}
                  tick={{ fill: "#858b86", fontSize: 12 }}
                  tickFormatter={(valor) =>
                    valor >= 1000
                      ? `R$ ${(valor / 1000).toFixed(1)}k`
                      : `R$ ${Number(valor).toFixed(0)}`
                  }
                />
                <Tooltip
                  formatter={(valor) => [
                    formatarMoeda(valor),
                    "Faturamento acumulado",
                  ]}
                  labelFormatter={(dia) => `Dia ${dia}`}
                  contentStyle={{
                    border: "1px solid #e9ece8",
                    borderRadius: 8,
                    boxShadow: "0 4px 14px rgba(24, 60, 50, 0.08)",
                    fontSize: 12,
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="faturamento"
                  stroke="#2f715d"
                  strokeWidth={2.5}
                  fill="url(#faturamentoGradient)"
                  activeDot={{
                    r: 5,
                    fill: "#183c32",
                    stroke: "#fff",
                    strokeWidth: 2,
                  }}
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>
        <div className="panel recent-sales">
          <h3>Despesas recentes</h3>
          <div className="table-wrapper">
            <table className="responsive-table">
              <thead>
                <tr>
                  <th>Descrição</th>
                  <th>Categoria</th>
                  <th>Valor</th>
                  <th>Data</th>
                </tr>
              </thead>
              <tbody>
                {despesasRecentes.map((despesa) => (
                  <tr key={despesa.id}>
                    <td data-label="Descrição">{despesa.descricao}</td>
                    <td data-label="Categoria">{despesa.categoria}</td>
                    <td data-label="Valor">{formatCurrency(despesa.valor)}</td>
                    <td data-label="Data">
                      {new Date(despesa.dataHora).toLocaleDateString("pt-BR")}
                    </td>
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
