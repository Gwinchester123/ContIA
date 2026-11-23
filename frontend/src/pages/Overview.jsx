import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import useFinanceData from '../hooks/useFinanceData'
import { formatCurrency, getFinancialSummary, clearSessionUser } from '../data/financeStorage'

const formatarMoeda = (valor) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', maximumFractionDigits: 0 }).format(valor)

const Icon = ({ children, size = 18 }) => <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>

export default function Overview() {
  const { vendas, despesas, erro } = useFinanceData()
  const resumo = getFinancialSummary(vendas, despesas)
  const faturamentoSemanal = Array.from({ length: 7 }, (_, index) => {
    const data = new Date()
    data.setHours(12, 0, 0, 0)
    data.setDate(data.getDate() - (6 - index))
    const chave = data.toISOString().slice(0, 10)
    return {
      dia: data.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }),
      faturamento: vendas.filter((venda) => String(venda.data).slice(0, 10) === chave).reduce((total, venda) => total + Number(venda.total || 0), 0),
    }
  })
  const despesasRecentes = [...despesas].sort((a, b) => String(b.createdAt || b.data).localeCompare(String(a.createdAt || a.data))).slice(0, 6)
  const [menuAberto, setMenuAberto] = useState(false)
  const perfilRef = useRef(null)
  const navigate = useNavigate()

  useEffect(() => {
    const fecharAoClicarFora = (event) => {
      if (perfilRef.current && !perfilRef.current.contains(event.target)) setMenuAberto(false)
    }
    const fecharComEscape = (event) => {
      if (event.key === 'Escape') setMenuAberto(false)
    }
    document.addEventListener('mousedown', fecharAoClicarFora)
    document.addEventListener('keydown', fecharComEscape)
    return () => {
      document.removeEventListener('mousedown', fecharAoClicarFora)
      document.removeEventListener('keydown', fecharComEscape)
    }
  }, [])

  const sairDaConta = () => {
    clearSessionUser()
    navigate('/login')
  }

  return <>
    <header>
      <div><h1>Visão geral</h1><p>Acompanhe os resultados do seu negócio.</p></div>
      <div className="profile-wrapper" ref={perfilRef}>
        <button className="profile" type="button" aria-expanded={menuAberto} aria-haspopup="menu" onClick={() => setMenuAberto((aberto) => !aberto)}>
          <div className="profile-avatar">MC</div><div className="profile-info"><strong>Maria Costa</strong><span>Administrador</span></div>
          <svg className={`profile-chevron${menuAberto ? ' open' : ''}`} width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden="true"><path d="m6 9 6 6 6-6" /></svg>
        </button>
        {menuAberto && (
          <div className="profile-menu" role="menu">
            <button type="button" role="menuitem" onClick={() => setMenuAberto(false)}><Icon size={16}><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></Icon>Meu perfil</button>
            <button type="button" role="menuitem" className="logout-action" onClick={sairDaConta}><Icon size={16}><path d="M10 17l5-5-5-5" /><path d="M15 12H3" /><path d="M14 4h5a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-5" /></Icon>Sair da conta</button>
          </div>
        )}
      </div>
    </header>
    {erro && <p className="data-error" role="alert">{erro}</p>}
    <section className="cards">
      <div className="card"><div className="card-title"><span>Faturamento total</span><div className="card-icon"><Icon size={16}><path d="M12 2v20" /><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6" /></Icon></div></div><h2>{formatCurrency(resumo.faturamento)}</h2></div>
            <div className="card"><div className="card-title"><span>Compras e custos</span><div className="card-icon"><Icon size={16}><path d="M20 12a8 8 0 1 1-2.34-5.66" /><path d="M20 4v8h-8" /></Icon></div></div><h2>{formatCurrency(resumo.compras)}</h2></div>
                  <div className="card"><div className="card-title"><span>Outras despesas</span><div className="card-icon"><Icon size={16}><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18" /></Icon></div></div><h2>{formatCurrency(resumo.outras)}</h2></div>
                  <div className="card"><div className="card-title"><span>Saldo financeiro estimado</span><div className="card-icon"><Icon size={16}><path d="m3 17 6-6 4 4 8-8" /><path d="M15 7h6v6" /></Icon></div></div><h2>{formatCurrency(resumo.saldo)}</h2></div>
    </section>
    <section className="content">
      <div className="panel sales-panel">
        <div className="panel-header"><div><h3>Faturamento</h3><p>Últimos 7 dias</p></div></div>
        <div className="sales-chart" aria-label="Gráfico de faturamento dos últimos 7 dias">
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={faturamentoSemanal} margin={{ top: 10, right: 8, left: -12, bottom: 0 }}>
              <defs><linearGradient id="faturamentoGradient" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="#6fa88e" stopOpacity={0.35} /><stop offset="100%" stopColor="#6fa88e" stopOpacity={0.03} /></linearGradient></defs>
              <CartesianGrid stroke="#edf0ed" strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="dia" axisLine={false} tickLine={false} tick={{ fill: '#858b86', fontSize: 12 }} dy={8} />
              <YAxis axisLine={false} tickLine={false} tick={{ fill: '#858b86', fontSize: 12 }} tickFormatter={(valor) => `R$ ${(valor / 1000).toFixed(0)}k`} />
              <Tooltip formatter={(valor) => [formatarMoeda(valor), 'Faturamento']} labelFormatter={(dia) => `Dia ${dia}`} contentStyle={{ border: '1px solid #e9ece8', borderRadius: 8, boxShadow: '0 4px 14px rgba(24, 60, 50, 0.08)', fontSize: 12 }} />
              <Area type="monotone" dataKey="faturamento" stroke="#2f715d" strokeWidth={2.5} fill="url(#faturamentoGradient)" activeDot={{ r: 5, fill: '#183c32', stroke: '#fff', strokeWidth: 2 }} />
            </AreaChart>
          </ResponsiveContainer>
        </div>
      </div>
      <div className="panel recent-sales">
        <h3>Despesas recentes</h3>
        <div className="table-wrapper"><table><thead><tr><th>Descrição</th><th>Categoria</th><th>Valor</th><th>Data</th></tr></thead><tbody>{despesasRecentes.map((despesa) => <tr key={despesa.id}><td>{despesa.descricao}</td><td>{despesa.categoria}</td><td>{formatCurrency(despesa.valor)}</td><td>{new Date(`${despesa.data}T12:00:00`).toLocaleDateString('pt-BR')}</td></tr>)}</tbody></table></div>
      </div>
    </section>
  </>
}
