import { Link } from "react-router-dom";

export default function InicioFuncionario() {
  return (
    <section className="panel page-panel employee-home">
      <span className="employee-home-eyebrow">Área de trabalho</span>
      <h1>Olá! Pronto para registrar suas vendas?</h1>
      <p>Use os atalhos para iniciar uma venda ou consultar seu histórico.</p>
      <div className="employee-home-actions">
        <Link className="primary-button" to="/dashboard/vendas">Registrar vendas</Link>
        <Link className="profile-back-button" to="/dashboard/minhas-vendas">Minhas vendas</Link>
      </div>
    </section>
  );
}
