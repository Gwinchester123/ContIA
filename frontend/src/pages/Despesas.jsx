import { useState } from 'react'
import useFinanceData from '../hooks/useFinanceData'
import { addExpense, formatCurrency, removeExpense } from '../data/financeStorage'

export default function Despesas() {
  const { despesas } = useFinanceData()
  const [descricao, setDescricao] = useState('')
  const [categoria, setCategoria] = useState('Ingredientes')
  const [valor, setValor] = useState('')
  const [data, setData] = useState(new Date().toISOString().slice(0, 10))

  const registrarDespesa = (event) => {
    event.preventDefault()
    const valorDespesa = Number(valor)
    if (!descricao.trim() || !data || !Number.isFinite(valorDespesa) || valorDespesa <= 0) return
    addExpense({ descricao: descricao.trim(), categoria, valor: valorDespesa, data })
    setDescricao(''); setValor(''); setCategoria('Ingredientes'); setData(new Date().toISOString().slice(0, 10))
  }

  const excluirDespesa = (despesa) => {
    if (window.confirm(`Excluir a despesa ${despesa.descricao}?`)) removeExpense(despesa.id)
  }

  return <>
    <header><div><h1>Despesas</h1><p>Registre os gastos e consulte seu histórico por categoria.</p></div></header>
    <section className="panel registration-panel"><h3>Nova despesa</h3>
      <form className="compact-form" onSubmit={registrarDespesa}>
        <label>Descrição<input value={descricao} onChange={(e) => setDescricao(e.target.value)} placeholder="Ex.: Compra de carne" required /></label>
        <label>Categoria<select value={categoria} onChange={(e) => setCategoria(e.target.value)}><optgroup label="Compras e custos"><option>Ingredientes</option><option>Materiais</option><option>Insumos</option></optgroup><optgroup label="Outras despesas"><option>Gás</option><option>Transporte</option><option>Energia</option><option>Outros</option></optgroup></select></label>
        <label>Valor (R$)<input type="number" min="0.01" step="0.01" value={valor} onChange={(e) => setValor(e.target.value)} required /></label>
        <label>Data<input type="date" value={data} onChange={(e) => setData(e.target.value)} required /></label>
        <button className="primary-button" type="submit">Registrar despesa</button>
      </form>
    </section>
    <section className="panel page-panel"><h3>Despesas cadastradas</h3><div className="table-wrapper"><table><thead><tr><th>Descrição</th><th>Categoria</th><th>Valor</th><th>Data</th><th></th></tr></thead><tbody>{despesas.map((despesa) => <tr key={despesa.id}><td>{despesa.descricao}</td><td>{despesa.categoria}</td><td>{formatCurrency(despesa.valor)}</td><td>{new Date(`${despesa.data}T12:00:00`).toLocaleDateString('pt-BR')}</td><td><button className="delete-button" type="button" onClick={() => excluirDespesa(despesa)}>Excluir</button></td></tr>)}</tbody></table></div></section>
  </>
}
