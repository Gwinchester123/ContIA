# ContIA

## Conectar ao Neon

O backend usa o endpoint pooler da branch `production` mostrado na captura, com o banco `neondb`, usuário `neondb_owner` e SSL habilitado. A URL e o usuário já estão configurados como padrão; podem ser substituídos por variáveis de ambiente. Como a senha da captura está ocultada, configure-a localmente sem incluí-la no código ou em mensagens.

No PowerShell, defina as variáveis antes de iniciar o backend:

```powershell
$env:NEON_DATABASE_PASSWORD = "<senha-do-neon>"
$env:CONTIA_JWT_SECRET = "<chave-aleatoria-com-pelo-menos-32-caracteres>"
$env:CONTIA_FRONTEND_ORIGIN = "http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

Se o endpoint da sua branch for diferente, defina também `NEON_JDBC_URL` no formato `jdbc:postgresql://<endpoint>/neondb?sslmode=require&channel_binding=require`; se necessário, substitua `NEON_DATABASE_USERNAME`. O backend cria/atualiza as tabelas `accounts`, `sales` e `expenses` ao iniciar. As senhas das contas são armazenadas com hash BCrypt e cada registro financeiro fica associado à conta autenticada.

Em outro terminal, inicie a interface:

```powershell
$env:VITE_API_URL = "http://localhost:8080/api"
npm.cmd --prefix frontend run dev
```

Configure `NEON_JDBC_URL`, `NEON_DATABASE_USERNAME`, `NEON_DATABASE_PASSWORD` e `CONTIA_JWT_SECRET` também no ambiente do serviço onde o backend for publicado. Nunca use credenciais do Neon no frontend.

## Migração do armazenamento local

No primeiro login de uma conta antiga, use o mesmo e-mail e senha já cadastrados no navegador. A aplicação cria a conta no backend com senha protegida e importa vendas e despesas locais em uma transação. Os dados locais só são removidos depois que o Neon confirma a importação. Se a conta só tinha registros antigos associados ao e-mail, sem cadastro local, o primeiro login cria a conta usando a senha informada.

Contas novas e registros financeiros passam a ser gravados no backend/Neon. O token de sessão fica apenas no `sessionStorage` do navegador.
