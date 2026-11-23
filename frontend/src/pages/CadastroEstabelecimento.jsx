import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { apiFetch, getSessionUser } from "../data/financeStorage";
import { apiUrl } from "../data/api";

export default function CadastroEstabelecimento() {
  const navigate = useNavigate();

  const [nome, setNome] = useState("");
  const [erro, setErro] = useState("");
  const [carregando, setCarregando] = useState(false);

  const cadastrar = async (event) => {
    event.preventDefault();
    setErro("");

    const proprietarioId = getSessionUser();

    if (!proprietarioId) {
      setErro("Faça login novamente para continuar.");
      return;
    }

    setCarregando(true);

    try {
      const resposta = await apiFetch(
        apiUrl("/estabelecimentos"),
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            nome,
            tipo: "Alimentação",
            proprietarioId: Number(proprietarioId),
          }),
        },
      );

      if (!resposta.ok) {
        const mensagem = await resposta.text();
        throw new Error(
          mensagem || "Não foi possível cadastrar o estabelecimento.",
        );
      }

      navigate("/dashboard");
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

        <h1>Conte sobre seu negócio</h1>
        <p>
          Precisamos de algumas informações para começar a organizar suas
          finanças.
        </p>

        <label>
          Nome do estabelecimento
          <input
            type="text"
            value={nome}
            onChange={(event) => setNome(event.target.value)}
            placeholder="Ex.: Espetinho do João"
            required
          />
        </label>

        {erro && <p className="auth-error">{erro}</p>}

        <button className="primary-button" type="submit" disabled={carregando}>
          {carregando ? "Salvando..." : "Continuar"}
        </button>
      </form>
    </main>
  );
}
