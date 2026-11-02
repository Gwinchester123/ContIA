import useFinanceData from '../hooks/useFinanceData'
import { formatCurrency, getFinancialSummary, getSalesTotal } from '../data/financeStorage'

export default function Relatorios() {
  const { vendas, despesas } = useFinanceData()
  const resumo = getFinancialSummary(vendas, despesas)
  const categorias = despesas.reduce((agrupadas, despesa) => {
    const atual = agrupadas.find((item) => item.categoria === despesa.categoria)
    if (atual) atual.valor += Number(despesa.valor)
    else agrupadas.push({ categoria: despesa.categoria, valor: Number(despesa.valor) })
    return agrupadas
  }, [])
  const faturamento = getSalesTotal(vendas)
  return (
    <>
      <header><div><h1>Relatórios</h1><p>Resumo financeiro simples do seu negócio.</p></div></header>
      <section className="cards reports-cards">
        <div className="card"><div className="card-title"><span>Total de vendas</span></div><h2>{vendas.length}</h2></div>
        <div className="card"><div className="card-title"><span>Total de despesas</span></div><h2>{formatCurrency(resumo.despesas)}</h2></div>
        <div className="card"><div className="card-title"><span>Compras e custos</span></div><h2>{formatCurrency(resumo.compras)}</h2></div>
        <div className="card"><div className="card-title"><span>Outras despesas</span></div><h2>{formatCurrency(resumo.outras)}</h2></div>
        <div className="card"><div className="card-title"><span>Faturamento</span></div><h2>{formatCurrency(faturamento)}</h2></div>
        <div className="card"><div className="card-title"><span>Saldo financeiro estimado</span></div><h2>{formatCurrency(resumo.saldo)}</h2></div>
      </section>
      <section className="content reports-content">
        <div className="panel"><h3>Faturamento por período</h3><div className="report-lines"><div><span>Total registrado</span><strong>{formatCurrency(faturamento)}</strong></div></div></div>
        <div className="panel"><h3>Despesas por categoria</h3><div className="table-wrapper"><table><thead><tr><th>Categoria</th><th>Valor</th></tr></thead><tbody>{categorias.map((item) => <tr key={item.categoria}><td>{item.categoria}</td><td>R$ {item.valor.toLocaleString('pt-BR', { minimumFractionDigits: 2 })}</td></tr>)}</tbody></table></div></div>
      </section>
    </>
  )
}
