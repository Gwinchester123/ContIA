const SESSION_USER_KEY = "contia-session-user";
const USERS_KEY = "contia-users";
const STORAGE_EVENT = "contia-finance-updated";

export const createId = () =>
  globalThis.crypto?.randomUUID?.() ||
  `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
export const getSessionUser = () =>
  sessionStorage.getItem(SESSION_USER_KEY) || "";
export const setSessionUser = (userOrId) => {
  const userId = typeof userOrId === "object" ? userOrId?.id : userOrId;

  sessionStorage.setItem(SESSION_USER_KEY, String(userId || "").trim());

  if (typeof userOrId === "object" && userOrId?.nome) {
    sessionStorage.setItem("contia-session-name", userOrId.nome);
  }
};

export const getSessionName = () => {
  return sessionStorage.getItem("contia-session-name") || "Proprietário";
};
export const clearSessionUser = () => {
  sessionStorage.removeItem(SESSION_USER_KEY);
  sessionStorage.removeItem("contia-session-name");
};

export const getUsers = () => {
  try {
    return JSON.parse(localStorage.getItem(USERS_KEY) || "[]");
  } catch {
    return [];
  }
};

export const registerUser = ({ name, email, password }) => {
  const normalizedEmail = email.trim().toLowerCase();
  const accounts = getUsers();
  if (accounts.some((account) => account.email === normalizedEmail))
    return null;
  const account = {
    id: createId(),
    name: name.trim(),
    email: normalizedEmail,
    password,
  };
  localStorage.setItem(USERS_KEY, JSON.stringify([...accounts, account]));
  return account;
};

export const authenticateUser = ({ email, password }) => {
  const normalizedEmail = email.trim().toLowerCase();
  const existing = getUsers().find(
    (account) => account.email === normalizedEmail,
  );
  if (existing) return existing.password === password ? existing : null;

  const hasLegacyData =
    localStorage.getItem(legacyStorageKey("vendas", normalizedEmail)) !==
      null ||
    localStorage.getItem(legacyStorageKey("despesas", normalizedEmail)) !==
      null;
  if (!hasLegacyData) return null;
  return registerUser({
    name: normalizedEmail.split("@")[0],
    email: normalizedEmail,
    password,
  });
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

    const account = getUsers().find((user) => user.id === userId);
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
