# Portfolio API

API REST para gerenciamento do portfólio de projetos de uma empresa, cobrindo o ciclo de vida completo: análise de viabilidade, planejamento, execução, encerramento, gerenciamento de equipe, orçamento e classificação de risco.

Desenvolvido como desafio técnico para vaga de Desenvolvedor Java.

## Stack

- **Java 21** + **Spring Boot 3.5.14**
- **PostgreSQL 16** (schema gerenciado via **Liquibase**)
- **Spring Data JPA** + **Hibernate 6**
- **Spring Security** (Basic Auth + usuário em memória)
- **MapStruct** para mapeamento entre entidades e DTOs
- **springdoc-openapi** para documentação OpenAPI/Swagger
- **JUnit 5** + **Mockito** + **AssertJ** + **JaCoCo**

## Pré-requisitos

- JDK 21 (o build verifica via `<java.version>21</java.version>` no `pom.xml`)
- Docker + Docker Compose (para o PostgreSQL)
- Maven Wrapper já incluído no repo (`mvnw.cmd` / `mvnw`)

## Subindo localmente

### 1. Subir o PostgreSQL

```bash
docker compose up -d
```

Sobe um container `postgres-portfolio` na porta `5432` com:
- DB: `portfolio`
- Usuário: `CodeGroup`
- Senha: `CodeGroup`

### 2. Rodar a aplicação

**Windows:**
```powershell
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`.

O Liquibase aplica os changesets automaticamente no startup (`db.changelog-0001` a `db.changelog-0003`).

## Autenticação

Toda a API exige **HTTP Basic Auth**. Usuário em memória configurado via `SecurityConfig`:

Usuário: `admin`

Senha: `admin@portfolio`

**Paths públicos** (sem auth): `/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**`, `/v3/api-docs.yaml`, `/error`.

## Documentação da API

Após subir a aplicação:

- **Swagger UI**: <http://localhost:8080/swagger-ui.html>
- **OpenAPI JSON**: <http://localhost:8080/v3/api-docs>

No Swagger UI, clique em **Authorize** no canto superior direito, preencha as credenciais acima e teste os endpoints diretamente pela UI.

## Decisões arquiteturais

### Mock de Member

O `Project` referencia o gerente apenas por `UUID managerId`, sem relcionamentos no banco, confome regra do desafio:
*"Deve ser disponibilizada uma API REST externa (mockada)"*, então eu desenvolvi `Member` como contexto externo, com:

- `MemberClient` (interface ACL — Anti-Corruption Layer) — interface que `ProjectService` e `ProjectAllocationService` consomem.
- `LocalMemberClient` — implementação local via `MemberRepository`. Em produção, pode ser substituída por um cliente HTTP.
- `MemberView` — record DTO interno do contrato do `MemberClient`.
- `MemberExternalController` + `MemberService` — simulam a API externa.

Vantagens: a entidade `Project` não fica acoplada à entidade `Member` no JPA, e a substituição da fake-implementation por um cliente HTTP real seria local ao `MemberClient`.

### Schema gerenciado por Liquibase, não Hibernate

`spring.jpa.hibernate.ddl-auto: none`. Toda mudança de schema deve ser um novo changeset em `src/main/resources/db/changelog/scripts/` registrado em `db.changelog.yaml`.

### Hierarquia de exceções tipadas

Subclasses de `ResponseException` (`Response400Exception`, `Response404Exception`, `Response422Exception`, `Response500Exception`) carregam um `messageKey` resolvido por i18n via `AppResourceBundle`, e são traduzidas em `ApiError` JSON pelo `RestExceptionHandler` (`@RestControllerAdvice`).

### UUID atribuído pela aplicação

Entidades estendem `AbstractBaseEntity` com `@Id UUID id`. JPA não auto-gera; o serviço atribui `UUID.randomUUID()` antes do `save`. Isso evita round-trips ao banco para gerar id e simplifica testes.

### Aritmética temporal portátil em HQL

O cálculo de duração média usa HQL `(p.endRealDate - p.initDate) by day`, sintaxe do Hibernate 6,sem SQL nativo.
Adotei essa estratégia pra evitar que o código ficasse acoplado ao PosgreSql, facilitando uma possível troca de banco de dados futura.

## Testes

### Rodar a suíte completa

```powershell
.\mvnw.cmd test
```

Configuração de cobertura (`pom.xml`) **exclui** `**/controller/**`, `**/repository/**` e `**/enums/**`, focando a métrica em **regras de negócio** (services), conforme exigência do desafio (≥70%).
