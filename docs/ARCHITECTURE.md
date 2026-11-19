# Arquitetura do Backend — UniFlow (`uniflow-backend`)

## 1. Visão Geral

O **UniFlow Backend** é um **Monólito Modular** escrito em **Java 21 LTS** com **Spring Boot**, persistência em **PostgreSQL** via **Spring Data JPA / Hibernate**, versionamento de banco com **Flyway** e autenticação stateless via **Spring Security + JWT**.

Este repositório atende exclusivamente ao backend e expõe uma API REST JSON (`/api/v1`) consumida pelo repositório irmão `uniflow-frontend` (React + TypeScript).

---

## 2. Stack e Estado Atual do `pom.xml`

| Componente | Especificação Original | Estado Adotado no `pom.xml` |
| :--- | :--- | :--- |
| Linguagem | Java 21 LTS | Java 21 (`21.0.9 LTS` ativo) |
| Framework | Spring Boot 3.5.x | `spring-boot-starter-parent` **`4.1.2`** *(mantido por decisão do usuário em `2026-11-19`)* |
| Web / REST | Spring Web | `spring-boot-starter-webmvc` (Boot `4.1.2`) |
| Segurança | Spring Security + JWT + BCrypt | `spring-boot-starter-security` + `jjwt` (`0.13.0`) |
| Persistência | Spring Data JPA + Hibernate + PostgreSQL | `spring-boot-starter-data-jpa` + `postgresql` |
| Migrations | Flyway | `spring-boot-starter-flyway` + `flyway-database-postgresql` |
| Validação | Bean Validation | `spring-boot-starter-validation` |
| Documentação API | Springdoc OpenAPI | `springdoc-openapi-starter-webmvc-ui:3.2.0` (compatível Boot `4.x`) |
| Produtividade / Testes | Lombok + JUnit 5 + Mockito | `lombok` + `spring-boot-starter-*-test` + `spring-boot-testcontainers` (`testcontainers-postgresql`) |

> **Regra de estabilidade**: Não atualizar versões principais de dependências automaticamente sem avaliar compatibilidade e registrar em [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md).

---

## 3. Estrutura Alvo do Repositório e Pacotes

```text
uniflow-backend/ (c:\uniflow_api\api)
├── src/main/java/com/uniflow/
│   ├── auth/            # MOD-01 Autenticação (login, registro, JWT, recuperação)
│   ├── usuario/         # MOD-02 Usuários (perfil, preferências, exclusão)
│   ├── semestre/        # MOD-03 Semestres letivos
│   ├── disciplina/      # MOD-04 Disciplinas
│   ├── aula/            # MOD-05 Aulas recorrentes e grade semanal
│   ├── atividade/       # MOD-06 Atividades, provas e entregas
│   ├── calendario/      # MOD-07 Agregação mensal/semanal/diária + Dashboard
│   ├── trabalho/        # MOD-08 Trabalhos acadêmicos e etapas
│   ├── grupo/           # MOD-09 Grupos e convites (v1.1)
│   ├── arquivo/         # MOD-10 Metadados e Object Storage (v1.1)
│   ├── notificacao/     # MOD-11 Notificações internas e alertas
│   ├── planejamento/    # MOD-12 Sessões de estudo e histórico
│   ├── assinatura/      # MOD-13 Assinaturas (v2.0 — fora do MVP)
│   ├── config/          # SecurityConfig, OpenApiConfig, CorsConfig, Timezone/Jackson
│   └── common/          # Exceções globais, PageResponse, auditoria JPA, RequestId/MDC
├── src/main/resources/
│   ├── db/migration/    # Scripts Flyway (V1__*.sql, V2__*.sql, ...)
│   └── application.yml  # Configuração multiambiente (dev, test, prod)
└── docs/                # Documentação viva, backlog, status e relatórios
```

*Estado atual*: Entrypoint principal em [UniflowApplication.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/UniflowApplication.java) (`com.uniflow`), com todos os 15 pacotes modulares e subcamadas estruturados na `BE-005`.

### Organização Interna de Cada Módulo

```text
<modulo>/
├── controller/   # Endpoints REST, @Valid, OpenAPI, status HTTP
├── service/      # Regras de negócio, @Transactional, validação de ownership
├── repository/   # Interfaces Spring Data JPA
├── entity/       # Entidades JPA mapeadas ao schema Flyway
├── dto/          # Records de Request e Response
├── mapper/       # Conversões explícitas Entity <-> DTO
└── enums/        # Enumerações de domínio do módulo
```

### Política de Abstrações
- **Não criar interfaces desnecessárias** (`FooService` + `FooServiceImpl` quando existe apenas uma implementação).
- Use interfaces apenas para:
  1. Repositórios Spring Data JPA;
  2. Integrações externas intercambiáveis (`ObjectStorageService`, `PasswordResetNotifier` / envio de e-mail, gateway de pagamento futuro).

---

## 4. Fronteiras Entre Módulos e Fluxo de Dados

- **Isolamento de Ownership**: Todo módulo obtém o identificador do usuário autenticado a partir do `SecurityContext` (`CurrentUserService`), jamais confiando em `usuarioId` enviado no corpo ou query pelo cliente.
- **Calendário sem Duplicação (`MOD-07`)**: O módulo `calendario` lê projeções de `aula`, `atividade` e `trabalho` dinamicamente para o intervalo solicitado no fuso horário do usuário, sem manter tabela própria espelhada.
- **Comunicação Interna**: Módulos leitores/agregadores (`calendario`, `dashboard`, `notificacao`) consultam serviços ou repositórios de consulta sem criar dependências cíclicas entre módulos de domínio.

---

## 5. Decisões Transversais (RNF)

- **Banco e Migrations (`RNF-006`, `RNF-007`)**: PostgreSQL é a única fonte de verdade do schema; `hibernate.ddl-auto` deve operar em `validate`.
- **Tratamento de Erros (`RNF-009`, `BE-007`, `BE-008`)**: `@RestControllerAdvice` único em `com.uniflow.common.exception` utilizando padrão **Problem Details (RFC 7807 / `ProblemDetail`)**.
- **Paginação (`RNF-012`)**: Coleções que crescem ao longo dos semestres usam `Pageable` + `PageResponse<T>`.
- **Fusos Horários (`RNF-016`)**: Datas/horas de eventos pontuais (`prazo`, `concluidaEm`, `createdAt`) são persistidas em UTC (`Instant` / `TIMESTAMPTZ`), enquanto horários semanais de aula usam `DayOfWeek` + `LocalTime` interpretados na `ZoneId` informada pelo usuário nas consultas de calendário.
- **Segredos e Variáveis de Ambiente (`RNF-015`)**: Credenciais, segredo JWT, origens CORS e bucket S3 são injetados por variáveis de ambiente.
