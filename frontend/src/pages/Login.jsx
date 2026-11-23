import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authenticateUser, migrateLegacyData, setSessionUser } from '../data/financeStorage'

export default function Login() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const entrar = async (event) => {
    event.preventDefault()
    setErro('')
    try {
      const account = await authenticateUser({ email, password: senha })
      setSessionUser(account)
      await migrateLegacyData(account.email)
      navigate('/dashboard')
    } catch (error) {
      setErro(error.message)
    }
  }

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={entrar}>
        <div className="brand auth-brand"><img src="/logo-contia.png" alt="ContIA" /></div>
        <h1>Bem-vindo de volta</h1>
        <p>Acesse o financeiro do seu negócio.</p>
        <label>E-mail<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="voce@email.com" required /></label>
        <label>Senha<input type="password" value={senha} onChange={(event) => setSenha(event.target.value)} placeholder="Sua senha" required /></label>
        {erro && <p className="auth-error" role="alert">{erro}</p>}
        <button className="primary-button" type="submit">Entrar</button>
        <div className="auth-links"><span>Não tem uma conta?</span> <Link to="/cadastro">Cadastre-se</Link></div>
      </form>
    </main>
  )
}
