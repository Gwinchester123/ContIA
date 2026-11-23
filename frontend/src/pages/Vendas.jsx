import { useState } from 'react'
import useFinanceData from '../hooks/useFinanceData'
import { addSale, formatCurrency, removeSale } from '../data/financeStorage'

export default function Vendas() {
  const { vendas, erro } = useFinanceData()
  const [erroOperacao, setErroOperacao] = useState('')
  const [produto, setProduto] = useState('')
  const [quantidade, setQuantidade] = useState('')
  const [valorUnitario, setValorUnitario] = useState('')
  const total = (Number(quantidade) || 0) * (Number(valorUnitario) || 0)

  const registrarVenda = async (event) => {
    event.preventDefault()
    const qtd = Number(quantidade)
    const valor = Number(valorUnitario)
    if (!produto.trim() || !Number.isInteger(qtd) || qtd <= 0 || !Number.isFinite(valor) || valor <= 0) return
    setErroOperacao('')
    try {
      await addSale({ produto: produto.trim(), quantidade: qtd, valorUnitario: valor, total: qtd * valor, data: new Date().toISOString() })
      setProduto(''); setQuantidade(''); setValorUnitario('')
    } catch (error) {
      setErroOperacao(error.message)
    }
  }

  const excluirVenda = async (venda) => {
    if (!window.confirm(`Excluir a venda de ${venda.produto}?`)) return
    setErroOperacao('')
    try {
      await removeSale(venda.id)
    } catch (error) {
      setErroOperacao(error.message)
    }
  }

  return <>
    <header><div><h1>Vendas</h1><p>Registre vendas informando apenas os dados da transação.</p></div></header>
    {(erro || erroOperacao) && <p className="data-error" role="alert">{erro || erroOperacao}</p>}
    <section className="panel registration-panel">
      <h3>Nova venda</h3>
      <form className="compact-form" onSubmit={registrarVenda}>
        <label>Produto<input value={produto} onChange={(e) => setProduto(e.target.value)} placeholder="Ex.: Espetinho de carne" required /></label>
        <label>Quantidade<input type="number" min="1" step="1" value={quantidade} onChange={(e) => setQuantidade(e.target.value)} required /></label>
        <label>Valor unitário (R$)<input type="number" min="0.01" step="0.01" value={valorUnitario} onChange={(e) => setValorUnitario(e.target.value)} required /></label>
        <div className="form-total"><span>Total da venda</span><strong>{formatCurrency(total)}</strong></div>
        <button className="primary-button" type="submit">Registrar venda</button>
      </form>
    </section>
    <section className="panel page-panel">
      <h3>Vendas registradas</h3>
      <div className="table-wrapper"><table><thead><tr><th>Produto</th><th>Quantidade</th><th>Valor unitário</th><th>Total</th><th>Data e horário</th><th></th></tr></thead><tbody>{vendas.map((venda) => <tr key={venda.id}><td>{venda.produto}</td><td>{venda.quantidade}</td><td>{formatCurrency(venda.valorUnitario)}</td><td>{formatCurrency(venda.total)}</td><td>{new Date(venda.data).toLocaleString('pt-BR')}</td><td><button className="delete-button" type="button" onClick={() => excluirVenda(venda)}>Excluir</button></td></tr>)}</tbody></table></div>
    </section>
  </>
}
