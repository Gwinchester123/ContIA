import { NavLink } from 'react-router-dom'

const Icon = ({ children }) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{children}</svg>
)

const menuBase = [
  { label: 'Visão geral', path: '/dashboard', icon: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></> },
  { label: 'Vendas', path: '/dashboard/vendas', icon: <><path d="M4 19V9" /><path d="M10 19V5" /><path d="M16 19v-7" /><path d="M22 19V3" /></> },
  { label: 'Despesas', path: '/dashboard/despesas', icon: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18" /><path d="M16 15h2" /></> },
]
const intelligenceItem = { label: 'Inteligência Financeira', path: '/dashboard/inteligencia-financeira', icon: <><path d="M12 3v2" /><path d="M5.6 5.6 7 7" /><path d="M3 12h2" /><path d="M19 12h2" /><path d="M17 7l1.4-1.4" /><path d="M8 15a4 4 0 1 1 8 0c0 1.4-.8 2.1-1.5 3-.4.5-.5 1-.5 1h-4s-.1-.5-.5-1c-.7-.9-1.5-1.6-1.5-3Z" /><path d="M10 22h4" /></> }
const ownerItems = [
  { label: 'Funcionários', path: '/dashboard/funcionarios', icon: <><circle cx="9" cy="8" r="3" /><path d="M3 20v-1a6 6 0 0 1 12 0v1" /><path d="M16 5.5a3 3 0 0 1 0 5.8M18 14a5 5 0 0 1 3 4.6V20" /></> },
]
const employeeItems = [
  { label: 'Início', path: '/dashboard', icon: menuBase[0].icon },
  { label: 'Registrar vendas', path: '/dashboard/vendas', icon: menuBase[1].icon },
  { label: 'Minhas vendas', path: '/dashboard/minhas-vendas', icon: <><path d="M4 5h16M4 10h16M4 15h10" /><path d="m16 18 2 2 4-5" /></> },
]

export default function Sidebar({ onNavigate, profile, premium }) {
  const items = profile === 'PROPRIETARIO'
    ? [...menuBase, ...(premium ? [intelligenceItem] : []), ...ownerItems]
    : employeeItems
  return (
    <aside className="sidebar" id="dashboard-navigation" aria-label="Navegação principal">
      <div className="sidebar-heading">
        <NavLink className="brand brand-link" to="/dashboard" onClick={onNavigate} aria-label="Ir para a tela inicial">
          <img src="/logo-contia.png" alt="ContIA" />
        </NavLink>
        <button className="sidebar-close" type="button" aria-label="Fechar menu" onClick={onNavigate}>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
        </button>
      </div>
      <nav aria-label="Seções do ContIA">
        {items.map((item) => (
          <NavLink key={item.path} to={item.path} end={item.path === '/dashboard'} onClick={onNavigate} title={item.label} className={({ isActive }) => isActive ? 'active' : ''}>
            <Icon>{item.icon}</Icon><span className="sidebar-label">{item.label}</span>
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
