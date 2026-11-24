const SESSION_USER_KEY = "contia-session-user";
const STORAGE_EVENT = "contia-finance-updated";

const getSessionValue = (key) => {
  const value = localStorage.getItem(key);
  if (value !== null) return value;
  const legacyValue = sessionStorage.getItem(key);
  if (legacyValue !== null) {
    localStorage.setItem(key, legacyValue);
    sessionStorage.removeItem(key);
  }
  return legacyValue || "";
};

export const createId = () =>
  globalThis.crypto?.randomUUID?.() ||
  `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
export const getSessionUser = () =>
  getSessionValue(SESSION_USER_KEY);
export const getSessionEstablishment = () =>
  getSessionValue("contia-session-establishment-id");
export const setSessionUser = (userOrId) => {
  const userId = typeof userOrId === "object" ? userOrId?.id : userOrId;

  localStorage.setItem(SESSION_USER_KEY, String(userId || "").trim());

  if (typeof userOrId === "object" && userOrId?.nome) {
    localStorage.setItem("contia-session-name", userOrId.nome);
  }
  if (typeof userOrId === "object" && userOrId?.token) {
    localStorage.setItem("contia-session-token", userOrId.token);
    localStorage.setItem("contia-session-profile", userOrId.perfil || "PROPRIETARIO");
    if (userOrId.estabelecimentoId) {
      localStorage.setItem("contia-session-establishment-id", String(userOrId.estabelecimentoId));
    } else {
      localStorage.removeItem("contia-session-establishment-id");
    }
  }
};

export const getSessionToken = () => getSessionValue("contia-session-token");
export const getSessionProfile = () => getSessionValue("contia-session-profile");

export const apiFetch = (url, options = {}) => {
  const headers = new Headers(options.headers || {});
  const token = getSessionToken();
  if (token) headers.set("Authorization", `Bearer ${token}`);
  return fetch(url, { ...options, headers });
};

export const getSessionName = () => {
  return getSessionValue("contia-session-name") || "Proprietário";
};
export const clearSessionUser = () => {
  for (const key of [
    SESSION_USER_KEY,
    "contia-session-name",
    "contia-session-token",
    "contia-session-profile",
    "contia-session-establishment-id",
  ]) {
    localStorage.removeItem(key);
    sessionStorage.removeItem(key);
  }
};

const storageKey = (collection, userId = getSessionUser()) =>
  `contia:${collection}:${userId}`;
const legacyStorageKey = (collection, email) => `contia:${collection}:${email}`;
export const notifyFinanceChanged = () =>
  window.dispatchEvent(new Event(STORAGE_EVENT));

const getStoredCollection = (collection, userId = getSessionUser()) => {
  if (!userId) return [];
  try {
    const current = localStorage.getItem(storageKey(collection, userId));
    if (current !== null) return JSON.parse(current);

    const accounts = JSON.parse(localStorage.getItem("contia-users") || "[]");
    const account = accounts.find((user) => user.id === userId);
    if (!account) return [];
    const legacy = localStorage.getItem(
      legacyStorageKey(collection, account.email),
    );
    if (!legacy) return [];
    const records = JSON.parse(legacy);
    localStorage.setItem(
      storageKey(collection, userId),
      JSON.stringify(records),
    );
    return records;
  } catch {
    return [];
  }
};

export const getSales = (userId) => getStoredCollection("vendas", userId);
export const getExpenses = (userId) => getStoredCollection("despesas", userId);
export const saveSales = (sales, userId) => {
  localStorage.setItem(storageKey("vendas", userId), JSON.stringify(sales));
  notifyFinanceChanged();
};
export const saveExpenses = (expenses, userId) => {
  localStorage.setItem(
    storageKey("despesas", userId),
    JSON.stringify(expenses),
  );
  notifyFinanceChanged();
};
export const addSale = (sale) =>
  saveSales([
    { ...sale, id: createId(), createdAt: new Date().toISOString() },
    ...getSales(),
  ]);
export const removeSale = (id) =>
  saveSales(getSales().filter((sale) => sale.id !== id));
export const addExpense = (expense) =>
  saveExpenses([
    { ...expense, id: createId(), createdAt: new Date().toISOString() },
    ...getExpenses(),
  ]);
export const removeExpense = (id) =>
  saveExpenses(getExpenses().filter((expense) => expense.id !== id));

const CATEGORIAS_CUSTO_DIRETO = new Set([
  "ingredientes",
  "materiais",
  "insumos",
  "embalagens",
]);

export const normalizarCategoriaDespesa = (categoria) =>
  String(categoria || "").trim().toLocaleLowerCase("pt-BR");

export const isPurchaseOrCost = (expense) =>
  CATEGORIAS_CUSTO_DIRETO.has(normalizarCategoriaDespesa(expense?.categoria));

export const getExpenseBreakdown = (expenses) =>
  expenses.reduce(
    (totais, expense) => {
      const chave = isPurchaseOrCost(expense) ? "compras" : "outras";
      totais[chave] += Number(expense.valor || 0);
      return totais;
    },
    { compras: 0, outras: 0 },
  );
export const getSalesTotal = (sales) =>
  sales.reduce((total, sale) => total + Number(sale.total || 0), 0);
export const getExpensesTotal = (expenses) =>
  expenses.reduce((total, expense) => total + Number(expense.valor || 0), 0);
export const getFinancialSummary = (sales, expenses) => {
  const faturamento = getSalesTotal(sales);
  const { compras, outras } = getExpenseBreakdown(expenses);
  return {
    faturamento,
    compras,
    outras,
    despesas: compras + outras,
    saldo: faturamento - compras - outras,
  };
};
export const formatCurrency = (value) =>
  new Intl.NumberFormat("pt-BR", {
    style: "currency",
    currency: "BRL",
    minimumFractionDigits: 2,
  }).format(value);
export { STORAGE_EVENT };
