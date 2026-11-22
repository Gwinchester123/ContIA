import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { setSessionUser } from "../data/financeStorage";
import { apiUrl } from "../data/api";

function formatarTelefone(valor) {
  const numeros = valor.replace(/\D/g, "").slice(0, 11);

  if (numeros.length <= 2) {
    return numeros.length ? `(${numeros}` : "";
  }

  if (numeros.length <= 7) {
    return `(${numeros.slice(0, 2)}) ${numeros.slice(2)}`;
  }

  return `(${numeros.slice(0, 2)}) ${numeros.slice(2, 7)}-${numeros.slice(7)}`;
}

export default function Cadastro() {
  const navigate = useNavigate();

  const [nome, setNome] = useState("");
  const [email, setEmail] = useState("");
  const [telefone, setTelefone] = useState("");
  const [senha, setSenha] = useState("");
  const [confirmarSenha, setConfirmarSenha] = useState("");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);

  const cadastrar = async (event) => {
    event.preventDefault();
    setErro("");

    if (senha !== confirmarSenha) {
      setErro("As senhas não coincidem.");
      return;
    }

    if (senha.length < 8) {
      setErro("A senha deve ter pelo menos 8 caracteres.");
      return;
    }

    setCarregando(true);

    try {
      const resposta = await fetch(apiUrl("/proprietarios"), {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          nome,
          email,
          telefone,
          senha,
        }),
      });

      if (!resposta.ok) {
        const mensagem = await resposta.text();
        throw new Error(mensagem || "Não foi possível criar sua conta.");
      }

      const account = await resposta.json();

      setSessionUser(account);
      navigate("/cadastro-estabelecimento");
    } catch (error) {
      setErro(error.message || "Não foi possível conectar ao servidor.");
    } finally {
      setCarregando(false);
    }
  };

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={cadastrar}>
        <div className="brand auth-brand">
          <img src="/logo-contia.png" alt="ContIA" />
        </div>

        <h1>Crie sua conta</h1>
        <p>Comece a acompanhar suas vendas e despesas.</p>

        <label>
          Nome completo
          <input
            type="text"
            value={nome}
            onChange={(event) => setNome(event.target.value)}
            placeholder="Seu nome"
            required
          />
        </label>

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
          Telefone
          <input
            type="tel"
            value={telefone}
            onChange={(event) =>
              setTelefone(formatarTelefone(event.target.value))
            }
            placeholder="(88) 99999-9999"
            required
          />
        </label>

        <label>
          Senha
          <input
            type="password"
            value={senha}
            onChange={(event) => setSenha(event.target.value)}
            placeholder="Crie uma senha"
            minLength={8}
            required
          />
        </label>

        <label>
          Confirmar senha
          <input
            type="password"
            value={confirmarSenha}
            onChange={(event) => setConfirmarSenha(event.target.value)}
            placeholder="Digite sua senha novamente"
            required
          />
        </label>

        {erro && <p className="auth-error">{erro}</p>}

        <button className="primary-button" type="submit" disabled={carregando}>
          {carregando ? "Criando conta..." : "Criar conta"}
        </button>

        <div className="auth-links">
          <span>Já tem uma conta?</span> <Link to="/">Entrar</Link>
        </div>
      </form>
    </main>
  );
}
