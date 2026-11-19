import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { getSessionUser, getSessionName } from "../data/financeStorage";

export default function MeuPerfil() {
  const navigate = useNavigate();
  const proprietarioId = getSessionUser();

  const [estabelecimento, setEstabelecimento] = useState(null);
  const [nome, setNome] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [mensagem, setMensagem] = useState("");
  const [erro, setErro] = useState("");

  useEffect(() => {
    if (!proprietarioId) {
      navigate("/login");
      return;
    }

    fetch(
      `http://localhost:8080/api/estabelecimentos/proprietario/${proprietarioId}`,
    )
      .then((resposta) => {
        if (!resposta.ok)
          throw new Error("Não foi possível carregar o estabelecimento.");
        return resposta.json();
      })
      .then((dados) => {
        const negocio = dados[0];
        if (!negocio) throw new Error("Nenhum estabelecimento encontrado.");
        setEstabelecimento(negocio);
        setNome(negocio.nome);
      })
      .catch((error) => setErro(error.message))
      .finally(() => setCarregando(false));
  }, [proprietarioId, navigate]);

  const salvar = async (event) => {
    event.preventDefault();
    setErro("");
    setMensagem("");

    if (!nome.trim()) {
      setErro("Informe o nome do estabelecimento.");
      return;
    }

    setSalvando(true);

    try {
      const resposta = await fetch(
        `http://localhost:8080/api/estabelecimentos/${estabelecimento.id}/nome`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            proprietarioId: Number(proprietarioId),
            nome: nome.trim(),
          }),
        },
      );

      if (!resposta.ok) {
        const detalhe = await resposta.text();
        throw new Error(detalhe || "Não foi possível atualizar o nome.");
      }

      const atualizado = await resposta.json();
      setEstabelecimento(atualizado);
      setNome(atualizado.nome);
      setMensagem("Nome do estabelecimento atualizado com sucesso!");
    } catch (error) {
      setErro(error.message);
    } finally {
      setSalvando(false);
    }
  };

  if (carregando) return <p>Carregando perfil...</p>;

  return (
    <section className="profile-page">
      <header className="profile-page-header">
        <div>
          <h1>Meu perfil</h1>
          <p>Gerencie as informações do seu negócio.</p>
        </div>
      </header>

      <form className="profile-form" onSubmit={salvar}>
        <div className="profile-form-heading">
          <h2>Dados do estabelecimento</h2>
          <p>Confira e atualize as informações do seu negócio.</p>
        </div>

        <div className="profile-field">
          <label>Proprietário</label>
          <input type="text" value={getSessionName()} disabled />
        </div>

        <div className="profile-field">
          <label htmlFor="nomeEstabelecimento">Nome do estabelecimento</label>
          <input
            id="nomeEstabelecimento"
            type="text"
            value={nome}
            onChange={(event) => setNome(event.target.value)}
            placeholder="Digite o nome do seu negócio"
            required
          />
        </div>

        <div className="profile-field">
          <label>Segmento</label>
          <input type="text" value="Alimentação" disabled />
        </div>

        {erro && <p className="auth-error">{erro}</p>}
        {mensagem && <p className="profile-success">{mensagem}</p>}

        <div className="profile-actions">
          <button className="primary-button" type="submit" disabled={salvando}>
            {salvando ? "Salvando..." : "Salvar alterações"}
          </button>

          <button
            className="profile-back-button"
            type="button"
            onClick={() => navigate("/dashboard")}
          >
            Voltar ao dashboard
          </button>
        </div>
      </form>
    </section>
  );
}
