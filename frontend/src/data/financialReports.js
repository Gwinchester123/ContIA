import { apiFetch, getSessionUser } from "./financeStorage";
import { apiUrl } from "./api";

export const dataLocal = (data) => {
  const ano = data.getFullYear();
  const mes = String(data.getMonth() + 1).padStart(2, "0");
  const dia = String(data.getDate()).padStart(2, "0");
  return `${ano}-${mes}-${dia}`;
};

export const obterEstabelecimentosProprietario = async () => {
  const proprietarioId = getSessionUser();
  if (!proprietarioId) throw new Error("Usuário não identificado.");
  const resposta = await apiFetch(apiUrl(`/estabelecimentos/proprietario/${proprietarioId}`));
  if (!resposta.ok) throw new Error("Não foi possível localizar seus estabelecimentos.");
  const estabelecimentos = await resposta.json();
  if (!Array.isArray(estabelecimentos)) throw new Error("A resposta de estabelecimentos está inválida.");
  return { proprietarioId, estabelecimentos };
};

export const montarUrlRelatorio = (rota, proprietarioId, estabelecimentoId, inicio, fim) => {
  const params = new URLSearchParams({ proprietarioId, estabelecimentoId, inicio, fim });
  return `${apiUrl(`/relatorios-financeiros/${rota}`)}?${params}`;
};

export const baixarRelatorioPdf = async ({ proprietarioId, estabelecimentoId, inicio, fim }) => {
  const resposta = await apiFetch(montarUrlRelatorio("pdf", proprietarioId, estabelecimentoId, inicio, fim));
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
