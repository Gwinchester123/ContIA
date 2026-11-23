# ContIA

## Backend e bancos de dados

O backend usa o PostgreSQL local por padrão. Sem `SPRING_PROFILES_ACTIVE`, o Spring carrega `src/main/resources/application.properties`, que aponta para `jdbc:postgresql://localhost:5432/contia` e continua usando `DB_PASSWORD` como já fazia.

### PostgreSQL local

Defina a senha local no ambiente e inicie o backend:

```powershell
$env:DB_PASSWORD = "senha-do-postgres-local"
.\mvnw.cmd spring-boot:run
```

No IntelliJ ou outra IDE, configure `DB_PASSWORD` nas variáveis de ambiente da configuração de execução. Não é necessário configurar `DB_URL` ou `DB_USERNAME` para usar o perfil local.

### Neon

Configure as três variáveis no ambiente de execução. `DB_URL` deve ser uma URL JDBC sem usuário ou senha; o perfil aplica `sslmode=require` na conexão.

Exemplo no PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE = "neon"
$env:DB_URL = "jdbc:postgresql://seu-endpoint.neon.tech/nome_do_banco"
$env:DB_USERNAME = "seu_usuario"
$env:DB_PASSWORD = "sua_senha"
.\mvnw.cmd spring-boot:run
```

No IntelliJ, preencha `SPRING_PROFILES_ACTIVE=neon`, `DB_URL`, `DB_USERNAME` e `DB_PASSWORD` nas variáveis de ambiente da configuração de execução.

O perfil `neon` está definido em `src/main/resources/application-neon.properties`. Os dois perfis usam `spring.jpa.hibernate.ddl-auto=validate`: o Hibernate valida o schema existente sem criar ou atualizar tabelas. A aplicação não executa migrações automaticamente.

Arquivos `.env`, `.env.*`, `application-local.properties` e `application-*.local.properties` são ignorados pelo Git. O Spring Boot não carrega arquivos `.env` automaticamente; configure as variáveis no terminal, no ambiente de implantação ou na IDE. Não salve credenciais em arquivos versionados.
