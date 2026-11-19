
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { setSessionUser } from '../data/financeStorage'

export default function Login() {
  const navigate = useNavigate()

  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState('')
  const [carregando, setCarregando] = useState(false)

  const entrar = async (event) => {
    event.preventDefault()
    setErro('')
    setCarregando(true)

    try {
      const resposta = await fetch(
        'http://localhost:8080/api/proprietarios/login',
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify({ email, senha })
        }
      )

      if (!resposta.ok) {
        const mensagem = await resposta.text()
        throw new Error(mensagem || 'E-mail ou senha inválidos.')
      }

      const account = await resposta.json()

      setSessionUser(account)
      navigate('/dashboard')

    } catch (error) {
      setErro(error.message || 'Não foi possível conectar ao servidor.')
    } finally {
      setCarregando(false)
    }
  }

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={entrar}>
        <div className="brand auth-brand">
          <img src="/logo-contia.png" alt="ContIA" />
        </div>

        <h1>Bem-vindo de volta</h1>
        <p>Acesse o financeiro do seu negócio.</p>

        <label>
          E-mail
          <input
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="voce@email.com"
            required
          />
        </label>

        <label>
          Senha
          <input
            type="password"
            value={senha}
            onChange={(event) => setSenha(event.target.value)}
            placeholder="Sua senha"
            required
          />
        </label>

        {erro && <p className="auth-error">{erro}</p>}

        <button
          className="primary-button"
          type="submit"
          disabled={carregando}
        >
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>

        <div className="auth-links">
          <span>Não tem uma conta?</span>{' '}
          <Link to="/cadastro">Cadastre-se</Link>
        </div>
      </form>
    </main>
  )
}
