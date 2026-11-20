import { getSessionUser } from "./financeStorage";

const API = "http://localhost:8080/api";

export const dataLocal = (data) => {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
};

export const obterEstabelecimentoAtual = async () => {
  const proprietarioId = getSessionUser();
  if (!proprietarioId) throw new Error("Usuário não identificado.");
  const resposta = await fetch(`${API}/estabelecimentos/proprietario/${proprietarioId}`);
  if (!resposta.ok) throw new Error("Não foi possível localizar seu estabelecimento.");
  const estabelecimentos = await resposta.json();
  const estabelecimento = estabelecimentos[0];
  if (!estabelecimento) throw new Error("Nenhum estabelecimento encontrado.");
  return { proprietarioId, estabelecimento };
};

export const montarUrlRelatorio = (rota, proprietarioId, estabelecimentoId, inicio, fim, completo = false) => {
  const params = new URLSearchParams({
    proprietarioId,
    estabelecimentoId,
    inicio,
    fim,
  });
  if (rota === "pdf") params.set("completo", String(completo));
  return `${API}/relatorios-financeiros/${rota}?${params}`;
};

export const baixarRelatorioPdf = async ({ proprietarioId, estabelecimentoId, inicio, fim, completo = false }) => {
  const resposta = await fetch(montarUrlRelatorio("pdf", proprietarioId, estabelecimentoId, inicio, fim, completo));
  if (!resposta.ok) throw new Error("Não foi possível gerar o PDF para o período selecionado.");
  const arquivo = URL.createObjectURL(await resposta.blob());
  const link = document.createElement("a");
  link.href = arquivo;
  link.download = `contia-relatorio-${inicio}-${fim}.pdf`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(arquivo);
};

export const periodoAtual = (tipo) => {
  const hoje = new Date();
  const inicio = new Date(hoje);
  const fim = new Date(hoje);
  if (tipo === "semanal") {
    inicio.setDate(hoje.getDate() - ((hoje.getDay() + 6) % 7));
    fim.setDate(inicio.getDate() + 6);
  } else if (tipo === "mensal") {
    inicio.setDate(1);
    fim.setMonth(hoje.getMonth() + 1, 0);
  }
  return { inicio: dataLocal(inicio), fim: dataLocal(fim) };
};
