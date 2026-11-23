const SESSION_USER_KEY = 'contia-session-user'
const SESSION_TOKEN_KEY = 'contia-session-token'
const USERS_KEY = 'contia-users'
const STORAGE_EVENT = 'contia-finance-updated'
const API_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/$/, '')

export const getAuthToken = () => sessionStorage.getItem(SESSION_TOKEN_KEY) || ''
export const getSessionUser = () => getAuthToken() ? sessionStorage.getItem(SESSION_USER_KEY) || '' : ''
export const setSessionUser = (account) => {
  sessionStorage.setItem(SESSION_USER_KEY, account.id)
  sessionStorage.setItem(SESSION_TOKEN_KEY, account.token)
}
export const clearSessionUser = () => {
  sessionStorage.removeItem(SESSION_USER_KEY)
  sessionStorage.removeItem(SESSION_TOKEN_KEY)
}

const request = async (path, options = {}) => {
  const headers = { ...options.headers }
  if (options.body) headers['Content-Type'] = 'application/json'
  const token = getAuthToken()
  if (token) headers.Authorization = `Bearer ${token}`

  const response = await fetch(`${API_URL}${path}`, { ...options, headers })
  const contentType = response.headers.get('content-type') || ''
  const body = contentType.includes('json') ? await response.json() : null
  if (!response.ok) {
    const message = body?.message || (response.status === 401
      ? 'Sessão inválida ou expirada. Entre novamente.'
      : 'Não foi possível concluir a solicitação.')
    const error = new Error(message)
    error.status = response.status
    throw error
  }
  if (response.status !== 204 && !contentType.includes('json')) {
    throw new Error('O servidor retornou uma resposta inválida.')
  }
  return body
}

const getLegacyUsers = () => {
  const stored = localStorage.getItem(USERS_KEY)
  if (stored === null) return []
  const users = JSON.parse(stored)
  if (!Array.isArray(users)) throw new Error('Os dados locais das contas estão em um formato inválido.')
  return users
}

const getLegacyRecords = (collection, account, email) => {
  const keys = account ? [`contia:${collection}:${account.id}`] : []
  keys.push(`contia:${collection}:${email}`)
  const key = keys.find((candidate) => localStorage.getItem(candidate) !== null)
  if (!key) return []
  const records = JSON.parse(localStorage.getItem(key))
  if (!Array.isArray(records)) throw new Error('Os dados financeiros locais estão em um formato inválido.')
  return records
}

const legacyUserForEmail = (email) => {
  const normalizedEmail = email.trim().toLowerCase()
  return getLegacyUsers().find((user) => user.email === normalizedEmail) || null
}

export const registerUser = ({ name, email, password }) => request('/auth/register', {
  method: 'POST',
  body: JSON.stringify({ name, email, password }),
})

export const authenticateUser = async ({ email, password }) => {
  try {
    return await request('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    })
  } catch (error) {
    if (error.status !== 401) throw error

    const normalizedEmail = email.trim().toLowerCase()
    const legacyUser = legacyUserForEmail(normalizedEmail)
    const hasLegacyRecords = localStorage.getItem(`contia:vendas:${normalizedEmail}`) !== null
      || localStorage.getItem(`contia:despesas:${normalizedEmail}`) !== null
    if ((!legacyUser || legacyUser.password !== password) && !(hasLegacyRecords && !legacyUser)) {
      throw new Error('E-mail ou senha inválidos.')
    }

    try {
      return await registerUser({
        name: legacyUser?.name || normalizedEmail.split('@')[0],
        email: normalizedEmail,
        password,
      })
    } catch (registrationError) {
      if (registrationError.status === 409) throw new Error('E-mail ou senha inválidos.')
      throw registrationError
    }
  }
}

export const migrateLegacyData = async (email) => {
  const normalizedEmail = email.trim().toLowerCase()
  const users = getLegacyUsers()
  const account = users.find((user) => user.email === normalizedEmail) || null
  const vendas = getLegacyRecords('vendas', account, normalizedEmail)
  const despesas = getLegacyRecords('despesas', account, normalizedEmail)

  await request('/finance/legacy-import', {
    method: 'POST',
    body: JSON.stringify({ vendas, despesas }),
  })

  const remainingUsers = users.filter((user) => user.email !== normalizedEmail)
  if (remainingUsers.length) localStorage.setItem(USERS_KEY, JSON.stringify(remainingUsers))
  else localStorage.removeItem(USERS_KEY)
  if (account) {
    localStorage.removeItem(`contia:vendas:${account.id}`)
    localStorage.removeItem(`contia:despesas:${account.id}`)
  }
  localStorage.removeItem(`contia:vendas:${normalizedEmail}`)
  localStorage.removeItem(`contia:despesas:${normalizedEmail}`)
}

export const notifyFinanceChanged = () => window.dispatchEvent(new Event(STORAGE_EVENT))

export const getSales = () => request('/finance/sales')
export const getExpenses = () => request('/finance/expenses')

export const addSale = async (sale) => {
  const created = await request('/finance/sales', {
    method: 'POST',
    body: JSON.stringify(sale),
  })
  notifyFinanceChanged()
  return created
}

export const removeSale = async (id) => {
  await request(`/finance/sales/${encodeURIComponent(id)}`, { method: 'DELETE' })
  notifyFinanceChanged()
}

export const addExpense = async (expense) => {
  const created = await request('/finance/expenses', {
    method: 'POST',
    body: JSON.stringify(expense),
  })
  notifyFinanceChanged()
  return created
}

export const removeExpense = async (id) => {
  await request(`/finance/expenses/${encodeURIComponent(id)}`, { method: 'DELETE' })
  notifyFinanceChanged()
}

export const isPurchaseOrCost = (expense) => ['Ingredientes', 'Materiais', 'Insumos'].includes(expense.categoria)
export const getSalesTotal = (sales) => sales.reduce((total, sale) => total + Number(sale.total || 0), 0)
export const getExpensesTotal = (expenses) => expenses.reduce((total, expense) => total + Number(expense.valor || 0), 0)
export const getFinancialSummary = (sales, expenses) => {
  const faturamento = getSalesTotal(sales)
  const compras = expenses.filter(isPurchaseOrCost).reduce((total, expense) => total + Number(expense.valor || 0), 0)
  const outras = expenses.filter((expense) => !isPurchaseOrCost(expense)).reduce((total, expense) => total + Number(expense.valor || 0), 0)
  return { faturamento, compras, outras, despesas: compras + outras, saldo: faturamento - compras - outras }
}
export const formatCurrency = (value) => new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL', minimumFractionDigits: 2 }).format(value)
export { STORAGE_EVENT }
