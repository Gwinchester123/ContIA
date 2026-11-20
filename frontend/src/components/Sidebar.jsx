import { NavLink } from 'react-router-dom'

const Icon = ({ children }) => (
  <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
    {children}
  </svg>
)

const menuItems = [
  { label: 'Visão geral', path: '/dashboard', icon: <><rect x="3" y="3" width="7" height="7" rx="1" /><rect x="14" y="3" width="7" height="7" rx="1" /><rect x="3" y="14" width="7" height="7" rx="1" /><rect x="14" y="14" width="7" height="7" rx="1" /></> },
  { label: 'Vendas', path: '/dashboard/vendas', icon: <><path d="M4 19V9" /><path d="M10 19V5" /><path d="M16 19v-7" /><path d="M22 19V3" /></> },
  { label: 'Despesas', path: '/dashboard/despesas', icon: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18" /><path d="M16 15h2" /></> },
  { label: 'Relatórios', path: '/dashboard/relatorios', icon: <><path d="M4 20V10" /><path d="M10 20V4" /><path d="M16 20v-7" /><path d="M22 20H2" /></> },
  { label: 'Inteligência Financeira', path: '/dashboard/inteligencia-financeira', icon: <><path d="M12 3v2" /><path d="M5.6 5.6 7 7" /><path d="M3 12h2" /><path d="M19 12h2" /><path d="M17 7l1.4-1.4" /><path d="M8 15a4 4 0 1 1 8 0c0 1.4-.8 2.1-1.5 3-.4.5-.5 1-.5 1h-4s-.1-.5-.5-1c-.7-.9-1.5-1.6-1.5-3Z" /><path d="M10 22h4" /></> },
]

export default function Sidebar({ onNavigate }) {
  return (
    <aside className="sidebar" id="dashboard-navigation" aria-label="Navegação principal">
      <div className="sidebar-heading">
        <div className="brand"><img src="/logo-contia.png" alt="ContIA" /></div>
        <button className="sidebar-close" type="button" aria-label="Fechar menu" onClick={onNavigate}>
          <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
        </button>
      </div>
      <nav aria-label="Seções do ContIA">
        {menuItems.map((item) => (
          <NavLink key={item.path} to={item.path} end={item.path === '/dashboard'} onClick={onNavigate} className={({ isActive }) => isActive ? 'active' : ''}>
            <Icon>{item.icon}</Icon>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
