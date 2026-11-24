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

A aplicação usa `/api` em produção, portanto as chamadas vão para o mesmo domínio do frontend. Em desenvolvimento, o valor padrão, também registrado em `.env.example`, é:

```ini
VITE_API_BASE_URL=http://localhost:8080/api
```

Para apontar o ambiente local para outro backend, defina o endereço base da API em `.env.local`, sem barra final. Exemplo:

```ini
VITE_API_BASE_URL=https://api.exemplo.com/api
```

O arquivo `.env.local` é local e não deve ser versionado. Essa variável é usada no desenvolvimento; não coloque senhas, tokens ou credenciais nela. Em produção, a aplicação usa `/api` no mesmo domínio.

## Build de produção

Não é necessário configurar uma URL para o build de produção:

```powershell
npm run build
```

Os arquivos prontos são gerados em `dist/`. Para pré-visualizar localmente o build:

```powershell
npm run preview
```

## Hospedagem

Em produção, as chamadas usam caminhos relativos `/api`, como `https://cont-ia-mu.vercel.app/api`. A função em `api/[...path].js` encaminha essas chamadas para a origem definida em `BACKEND_URL`, sem expor o endereço do backend no navegador.

O backend Spring precisa ser implantado em um serviço que execute Java 25 ou Docker. A pasta `Backend` contém a aplicação e o `Backend/Dockerfile`. Configure no serviço do backend:

- `SPRING_PROFILES_ACTIVE=neon`
- `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` com as credenciais do PostgreSQL
- `CONTIA_JWT_SECRET` com pelo menos 32 caracteres aleatórios

No projeto do frontend no Vercel, configure `BACKEND_URL` como a origem HTTPS do backend, sem `/api` no final (por exemplo, `https://contia-api.exemplo.com`). Configure-a nos ambientes Production e Preview que for usar, depois faça um novo deploy. A raiz do projeto Vercel deve ser `Frontend` para que a função em `api/` seja detectada. Não configure `VITE_API_BASE_URL` no Vercel.

O `vercel.json` encaminha as rotas da aplicação para `index.html`, permitindo recarregar diretamente páginas como `/dashboard/vendas` sem receber uma página de erro do Vercel.

O backend escuta a porta informada pelo provedor por meio de `PORT`, usando `8080` como padrão. A função de proxy remove o cabeçalho `Origin`, então CORS não é necessário nessa comunicação. Se o frontend chamar o backend diretamente em outro domínio, configure `CONTIA_CORS_ALLOWED_ORIGINS` com a origem do frontend.

O ContIA usa PWA; em produção, publique o frontend em HTTPS e sirva a aplicação na raiz do domínio para manter o escopo atual do Service Worker e do Manifest.
