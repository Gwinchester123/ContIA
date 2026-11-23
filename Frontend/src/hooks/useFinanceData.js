import { useEffect, useState } from 'react'
import { getExpenses, getSales, STORAGE_EVENT } from '../data/financeStorage'

export default function useFinanceData() {
  const [dados, setDados] = useState({ vendas: [], despesas: [] })
  const [erro, setErro] = useState('')

  useEffect(() => {
    let active = true
    const atualizar = async () => {
      try {
        const [vendas, despesas] = await Promise.all([getSales(), getExpenses()])
        if (active) {
          setDados({ vendas, despesas })
          setErro('')
        }
      } catch (error) {
        if (active) setErro(error.message)
      }
    }

    atualizar()
    window.addEventListener(STORAGE_EVENT, atualizar)
    return () => {
      active = false
      window.removeEventListener(STORAGE_EVENT, atualizar)
    }
  }, [])

  return { ...dados, erro }
}
