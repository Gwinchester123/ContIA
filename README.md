# ContIA

O repositório está dividido em dois projetos independentes:

- [Frontend](./Frontend/README.md): React e Vite; configure a raiz do projeto Vercel como `Frontend`.
- `Backend`: API Spring Boot/Java 25; implante em um provedor que ofereça suporte a Java ou Docker. A Vercel não executa diretamente esta aplicação Java.

## Desenvolvimento local

Inicie o backend em um terminal PowerShell:

```powershell
Set-Location Backend
$env:DB_PASSWORD = "senha-do-postgres-local"
.\mvnw.cmd spring-boot:run
```

Inicie o frontend em outro terminal:

```powershell
Set-Location Frontend
npm ci
Copy-Item .env.example .env.local
npm run dev
```

O frontend usa `http://localhost:8080/api` por padrão em desenvolvimento. Ajuste `VITE_API_BASE_URL` no `Frontend/.env.local` se necessário.

## Implantação independente

### Frontend na Vercel

Crie um projeto Vercel apontando para este repositório e defina **Root Directory** como `Frontend`. O projeto usa `npm run build`, gera os arquivos em `dist/` e inclui a função `api/[...path].js` para encaminhar as chamadas de API.

Defina a variável `BACKEND_URL` nos ambientes Vercel necessários como a origem HTTPS do backend, sem `/api` no final (por exemplo, `https://contia-api.exemplo.com`). Não configure `VITE_API_BASE_URL` na Vercel.

### Backend em provedor com suporte a Java

Implante o projeto da pasta `Backend` em um serviço com Java 25 ou Docker. O [Dockerfile](./Backend/Dockerfile) pode ser usado com `Backend` como contexto de build. Configure:

- `SPRING_PROFILES_ACTIVE=neon`
- `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` para o PostgreSQL
- `CONTIA_JWT_SECRET` com pelo menos 32 caracteres aleatórios

O serviço lê a porta atribuída pelo provedor por meio de `PORT` (padrão `8080`). O exemplo das variáveis de banco está em `Backend/.env.example`; o Spring Boot não carrega arquivos `.env` automaticamente. Não salve credenciais em arquivos versionados.

O perfil `neon`, em `Backend/src/main/resources/application-neon.properties`, exige um schema já criado e usa `spring.jpa.hibernate.ddl-auto=validate`; a aplicação não executa migrações automaticamente. Os scripts SQL disponíveis estão em `Backend/db/migrations`.
