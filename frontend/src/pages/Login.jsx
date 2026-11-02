import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authenticateUser, setSessionUser } from '../data/financeStorage'

export default function Login() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
    const [erro, setErro] = useState('')
  const entrar = (event) => {
    event.preventDefault()
    const account = authenticateUser({ email, password: senha })
    if (!account) { setErro('E-mail ou senha inválidos.'); return }
    setSessionUser(account.id)
    navigate('/dashboard')
  }

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={entrar}>
        <div className="brand auth-brand"><span>Cont</span><strong>IA</strong></div>
        <h1>Bem-vindo de volta</h1>
        <p>Acesse o financeiro do seu negócio.</p>
        <label>E-mail<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="voce@email.com" required /></label>
        <label>Senha<input type="password" placeholder="Sua senha" required /></label>
        <button className="primary-button" type="submit">Entrar</button>
        <div className="auth-links"><span>Não tem uma conta?</span> <Link to="/cadastro">Cadastre-se</Link></div>
      </form>
    </main>
  )
}
