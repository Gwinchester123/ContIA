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
]

export default function Sidebar() {
  return (
    <aside className="sidebar">
      <div className="brand"><img src="/logo-contia.png" alt="ContIA" /></div>
      <nav>
        {menuItems.map((item) => (
          <NavLink key={item.path} to={item.path} end={item.path === '/dashboard'} className={({ isActive }) => isActive ? 'active' : ''}>
            <Icon>{item.icon}</Icon>
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
