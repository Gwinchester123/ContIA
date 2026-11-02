import { useEffect, useState } from 'react'
import { getExpenses, getSales, STORAGE_EVENT } from '../data/financeStorage'

export default function useFinanceData() {
  const [dados, setDados] = useState(() => ({ vendas: getSales(), despesas: getExpenses() }))

  useEffect(() => {
    const atualizar = () => setDados({ vendas: getSales(), despesas: getExpenses() })
    window.addEventListener(STORAGE_EVENT, atualizar)
    window.addEventListener('storage', atualizar)
    return () => {
      window.removeEventListener(STORAGE_EVENT, atualizar)
      window.removeEventListener('storage', atualizar)
    }
  }, [])

  return dados
}
