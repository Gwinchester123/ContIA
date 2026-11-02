import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { registerUser, setSessionUser } from '../data/financeStorage'

export default function Cadastro() {
  const navigate = useNavigate()
  const [nome, setNome] = useState('')
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const cadastrar = (event) => {
    event.preventDefault()
    const account = registerUser({ name: nome, email, password: senha })
    if (!account) { setErro('Já existe uma conta com este e-mail.'); return }
    setSessionUser(account.id)
    navigate('/dashboard')
  }

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={cadastrar}>
        <div className="brand auth-brand"><span>Cont</span><strong>IA</strong></div>
        <h1>Crie sua conta</h1>
        <p>Comece a acompanhar suas vendas e despesas.</p>
        <label>Nome<input type="text" value={nome} onChange={(event) => setNome(event.target.value)} placeholder="Seu nome" required /></label>
        <label>E-mail<input type="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="voce@email.com" required /></label>
        <label>Senha<input type="password" value={senha} onChange={(event) => setSenha(event.target.value)} placeholder="Crie uma senha" required /></label>
        {erro && <p className="auth-error">{erro}</p>}
        <button className="primary-button" type="submit">Cadastrar</button>
        <div className="auth-links"><span>Já tem uma conta?</span> <Link to="/">Entrar</Link></div>
      </form>
    </main>
  )
}
