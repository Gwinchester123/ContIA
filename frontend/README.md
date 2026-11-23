# ContIA — Frontend

Frontend do ContIA, construído com React e Vite.

## Pré-requisitos

- Node.js (versão LTS atual) e npm.
- Backend ContIA em execução e acessível pelo navegador. Para desenvolvimento local, use a API em `http://localhost:8080/api`.

## Instalação e execução local

No PowerShell, a partir desta pasta:

```powershell
npm ci
Copy-Item .env.example .env.local
npm run dev
```

O Vite informa no terminal o endereço local (normalmente `http://localhost:5173`). O backend precisa permitir essa origem em CORS.

## URL da API

A aplicação lê `VITE_API_BASE_URL`. O valor padrão, também registrado em `.env.example`, é:

```ini
VITE_API_BASE_URL=http://localhost:8080/api
```

Para apontar para outro backend, defina o endereço base da API em `.env.local`, sem barra final. Exemplo:

```ini
VITE_API_BASE_URL=https://api.exemplo.com/api
```

O arquivo `.env.local` é local e não deve ser versionado. Essa variável contém somente a URL pública da API; não coloque senhas, tokens ou credenciais nela.

## Build de produção

Configure a URL pública da API antes de gerar o build. No PowerShell:

```powershell
$env:VITE_API_BASE_URL = "https://api.exemplo.com/api"
npm run build
```

Os arquivos prontos são gerados em `dist/`. Para pré-visualizar localmente o build:

```powershell
npm run preview
```

## Hospedagem

Na plataforma de hospedagem do frontend, configure `VITE_API_BASE_URL` com a URL HTTPS pública do backend, incluindo o prefixo `/api`, e gere um novo build/deploy. O valor é incorporado aos arquivos estáticos durante o build. A pessoa responsável pelo backend também deve incluir a origem HTTPS exata do frontend em `CONTIA_CORS_ALLOWED_ORIGINS`, separando múltiplas origens por vírgula. O backend mantém as origens locais `http://localhost:5173` e `http://localhost:5174` por padrão.

O ContIA usa PWA; em produção, publique o frontend em HTTPS e sirva a aplicação na raiz do domínio para manter o escopo atual do Service Worker e do Manifest.
