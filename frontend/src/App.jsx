import "./App.css";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";

import Sidebar from "./components/Sidebar";
import Overview from "./pages/Overview";
import Vendas from "./pages/Vendas";
import Despesas from "./pages/Despesas";
import Relatorios from "./pages/Relatorios";
import Login from "./pages/Login";
import Cadastro from "./pages/Cadastro";
import CadastroEstabelecimento from "./pages/CadastroEstabelecimento";
import MeuPerfil from "./pages/MeuPerfil";

function DashboardLayout() {
  return (
    <div className="container">
      <Sidebar />

      <main className="main">
        <Routes>
          <Route index element={<Overview />} />
          <Route path="vendas" element={<Vendas />} />
          <Route path="despesas" element={<Despesas />} />
          <Route path="relatorios" element={<Relatorios />} />
          <Route path="perfil" element={<MeuPerfil />} />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </main>
    </div>
  );
}

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/login" element={<Login />} />
        <Route path="/cadastro" element={<Cadastro />} />
        <Route
          path="/cadastro-estabelecimento"
          element={<CadastroEstabelecimento />}
        />
        <Route path="/dashboard/*" element={<DashboardLayout />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
