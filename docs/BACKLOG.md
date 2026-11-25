# Backlog Oficial do Backend — UniFlow (`uniflow-backend`)

> **Repositório**: `uniflow-backend` (`c:\uniflow_api\api`) | **Stack**: Java 21, Spring Boot `4.1.2` *(mantido por decisão do usuário)*, Maven, PostgreSQL, Spring Data JPA, Flyway
> **Como executar qualquer item**: use `/run <ID>` (execução direta quando desbloqueada, ex.: `/run BE-004` ou `/run back-022`) ou `/executar <ID>` (com revisão do plano antes de codar).

## Convenções de Status
- `[ ] PENDENTE`: Ainda não iniciado.
- `[ ] EM_ANDAMENTO`: Em execução ativa após aprovação.
- `[x] CONCLUIDO`: Implementado, validado, build + testes passando e documentação atualizada.
- `[ ] BLOQUEADO`: Aguardando pré-requisito técnico/dependência.
- `[ ] FORA_DO_MVP`: Tarefa de Versão 2.0 / Futura (bloqueada até validação do MVP 1.0).

## Visão Geral dos 12 Épicos (`158` Tarefas)

| Épico | Título | Versão | Faixa de IDs | Qtd. | Pacote(s) |
| :---: | :--- | :--- | :--- | :---: | :--- |
| **1** | Configuração e estrutura do projeto | `MVP 1.0` | `BE-001`..`BE-012` | 12 | `config`, `common` |
| **2** | Autenticação e usuários | `MVP 1.0` | `BE-013`..`BE-031` | 19 | `auth`, `usuario` |
| **3** | Semestres acadêmicos | `MVP 1.0` | `BE-032`..`BE-045` | 14 | `semestre` |
| **4** | Disciplinas | `MVP 1.0` | `BE-046`..`BE-058` | 13 | `disciplina` |
| **5** | Aulas e horários | `MVP 1.0` | `BE-059`..`BE-070` | 12 | `aula` |
| **6** | Atividades acadêmicas | `MVP 1.0` | `BE-071`..`BE-089` | 19 | `atividade` |
| **7** | Trabalhos e etapas | `MVP 1.0` | `BE-090`..`BE-107` | 18 | `trabalho` |
| **8** | Dashboard e calendário | `MVP 1.0` | `BE-108`..`BE-119` | 12 | `calendario` |
| **9** | Grupos e colaboração | `Versão 1.1` | `BE-120`..`BE-132` | 13 | `grupo`, `trabalho` |
| **10** | Arquivos e notificações | `Versão 1.1` | `BE-133`..`BE-144` | 12 | `arquivo`, `notificacao` |
| **11** | Planejamento inteligente e IA | `Versão 2.0` | `BE-145`..`BE-151` | 7 | `planejamento` |
| **12** | Monetização | `Versão Futura` | `BE-152`..`BE-158` | 7 | `assinatura` |
| **Total** | **Todos os épicos** | **MVP 1.0 (119) + v1.1 (25) + v2.0/Futura (14)** | `BE-001`..`BE-158` | **158** | Monólito Modular |

---

## Épico 1: Configuração e estrutura do projeto (`MVP 1.0`)

> **Prioridade inicial**: Concluir a estrutura inicial antes de implementar qualquer módulo funcional.

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `CONCLUIDO` | `BE-001` | Criar projeto Spring Boot com Maven e Java 21 | Alta | — | Projeto inicial Spring Boot (`4.1.2` mantido por decisão do usuário em `2026-11-19`) + Maven + Java 21 compilando via `.\mvnw.cmd test-compile` |
| `CONCLUIDO` | `BE-002` | Configurar dependências do projeto | Alta | `BE-001` | `pom.xml` completo com Web MVC, JPA, Security, Flyway, PostgreSQL, Bean Validation, Lombok, Springdoc (`3.2.0`), JWT (`jjwt:0.13.0`) e JUnit/Mockito/Testcontainers (`DependenciesSmokeTest` 2/2 passando) |
| `CONCLUIDO` | `BE-003` | Configurar PostgreSQL e variáveis de ambiente | Alta | `BE-002` | `compose.yaml` (PostgreSQL 16) + `.env.example` + `.gitignore` (`.env`) + `application.properties` parametrizado via env vars (`DatabaseConfigPropertiesTest` 2/2 passando) |
| `CONCLUIDO` | `BE-004` | Configurar Flyway | Alta | `BE-003` | `spring.flyway.*` em `application.yml` (`enabled=true`, `validate-on-migrate=true`, `clean-disabled=true`), `ddl-auto=validate` e `V1__init_baseline.sql` criada/validada (`FlywayConfigTest` 2/2; aplicação em DB aguarda PostgreSQL/Docker ativo) |
| `CONCLUIDO` | `BE-005` | Criar estrutura de pacotes por domínio | Alta | `BE-001` | Entrypoint `com.uniflow.UniflowApplication` e 15 pacotes modulares `com.uniflow.*` estruturados por responsabilidade (`ModularStructureTest` 2/2 passando) |
| `CONCLUIDO` | `BE-006` | Configurar application.yml para ambientes | Alta | `BE-003` | `application.yml`, `application-dev.yml`, `application-test.yml`, `application-prod.yml` com timezone UTC, `open-in-view: false`, `ddl-auto: validate` e env vars (`DatabaseConfigPropertiesTest` 3/3 passando) |
| `CONCLUIDO` | `BE-007` | Configurar tratamento global de exceções | Alta | `BE-005` | Hierarquia `UniflowException` + `GlobalExceptionHandler` (`@RestControllerAdvice`) em `common.exception` interceptando erros HTTP/domínio (`GlobalExceptionHandlerWebMvcTest` 6/6 passando) |
| `CONCLUIDO` | `BE-008` | Criar padrão de respostas de erro com Problem Details | Alta | `BE-007` | `FieldErrorDetail`, `ProblemDetailsFactory` e integração com `GlobalExceptionHandler` retornando RFC 7807 completo (`type`, `title`, `status`, `detail`, `instance`, `code`, `timestamp` UTC, `fieldErrors`); `GlobalExceptionHandlerWebMvcTest` 7/7 + `ProblemDetailsFactoryTest` 2/2 |
| `CONCLUIDO` | `BE-009` | Configurar validação com Bean Validation | Alta | `BE-002`, `BE-008` | `MethodArgumentNotValidException`, `ConstraintViolationException` e `HandlerMethodValidationException` mapeadas no `GlobalExceptionHandler` para `ProblemDetail` `400 Bad Request` (`VALIDATION_ERROR`) com `fieldErrors` detalhados (`BeanValidationWebMvcTest` 4/4 passando) |
| `CONCLUIDO` | `BE-010` | Configurar documentação Swagger/OpenAPI | Média | `BE-002`, `BE-006` | `OpenApiConfig` com metadados da API `/api/v1`, servidor base `/`, esquema JWT `bearerAuth` e `springdoc.swagger-ui` configurado por perfil (`/v3/api-docs` e `/swagger-ui.html` validados em `OpenApiConfigTest` 1/1 + `OpenApiWebMvcTest` 2/2) |
| `CONCLUIDO` | `BE-011` | Configurar logs da aplicação | Média | `BE-006` | `RequestLoggingFilter` (`OncePerRequestFilter`, `HIGHEST_PRECEDENCE`) com `requestId` em MDC e header `X-Request-Id`, `SensitiveDataMasker` + `SensitiveDataMaskingConverter` e `logback-spring.xml` (UTC, `!prod`/`prod` estruturado sem vazar senhas/tokens); `SensitiveDataMaskerTest` 3/3 + `LogbackLoggingConfigTest` 2/2 + `RequestLoggingFilterWebMvcTest` 2/2 |
| `CONCLUIDO` | `BE-012` | Configurar testes com JUnit e Mockito | Alta | `BE-004`, `BE-006` | `TestcontainersConfiguration` (`postgres:16-alpine` + `@ServiceConnection`), `AbstractIntegrationTest`, `UniflowApplicationTests` (`PostgreSQL 16.15` + Flyway `V1` + Hibernate `validate`) e `RequestLoggingFilterMockitoTest` (2/2); `.\mvnw.cmd test` completo com `35/35` passando (`BUILD SUCCESS`) |

---

## Épico 2: Autenticação e usuários (`MVP 1.0` — `MOD-01`, `MOD-02`)

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `CONCLUIDO` | `BE-013` | Criar entidade Usuario | Alta | `BE-005`, `BE-014` | Entidade JPA `Usuario` em `usuario.entity` (`id UUID`, `nome`, `email` normalizado lowercase + unique `uk_usuarios_email`, `senhaHash` omitido do `toString`, `status` `VARCHAR(40)`, auditoria UTC via `@PrePersist`/`@PreUpdate`; `UsuarioTest` 3/3 e suíte filtrada 39/39 passando) |
| `CONCLUIDO` | `BE-014` | Criar enum de status do usuário | Alta | `BE-005` | Enum `StatusUsuario` (`ATIVO`, `INATIVO`, `BLOQUEADO`, `EXCLUIDO`) em `usuario.enums` com regra `permiteAutenticacao()` (`StatusUsuarioTest` 2/2 e `.\mvnw.cmd test` 37/37 passando) |
| `CONCLUIDO` | `BE-015` | Criar migration da tabela usuario | Alta | `BE-004`, `BE-013` | `V2__create_usuarios_table.sql` (`usuarios` e `refresh_tokens`) aplicada no PostgreSQL `16.15` Testcontainers e validada pelo Hibernate (`ddl-auto: validate`) em `UniflowApplicationTests` (`.\mvnw.cmd clean test` `40/40` passando) |
| `CONCLUIDO` | `BE-016` | Criar UsuarioRepository | Alta | `BE-013`, `BE-015` | `UsuarioRepository` em `usuario.repository` estendendo `JpaRepository<Usuario, UUID>` com `findByEmailIgnoreCase` e `existsByEmailIgnoreCase` (`UsuarioRepositoryIntegrationTest` 5/5 em PostgreSQL `16.15` Testcontainers e suíte `.\mvnw.cmd clean test` 45/45 passando) |
| `CONCLUIDO` | `BE-017` | Criar DTO de cadastro | Alta | `BE-009` | Records `RegisterRequest` e `AuthUserResponse` em `auth.dto` com Bean Validation (`nome`, `email`, `senha` forte), normalização de entrada e sem exposição de `senha`/`senhaHash` (`RegisterRequestValidationTest` 25/25, `RegisterDtoWebMvcTest` 2/2 e suíte `.\mvnw.cmd clean test` 72/72 passando) |
| `CONCLUIDO` | `BE-018` | Criar DTO de login | Alta | `BE-009` | Records `LoginRequest` e `AuthTokenResponse` (`accessToken`, `refreshToken`, `expiresIn`, `tokenType`, `usuario`) em `auth.dto` com Bean Validation, fábrica `bearer(...)` e mascaramento de credenciais/tokens no `toString()` (`LoginRequestValidationTest` 13/13, `LoginDtoWebMvcTest` 2/2 e suíte `.\mvnw.cmd clean test` 87/87 passando) |
| `CONCLUIDO` | `BE-019` | Implementar cadastro de usuário | Alta | `BE-016`, `BE-017`, `BE-020` | `POST /api/v1/auth/register` em [AuthController](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java), [AuthService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/AuthService.java) e [AuthMapper](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/mapper/AuthMapper.java) normalizando e-mail, gerando hash BCrypt, bloqueando duplicidade (`409 RESOURCE_CONFLICT`) e retornando `201` sem `senhaHash` (`AuthMapperTest` 2/2, `AuthServiceTest` 4/4, `AuthControllerWebMvcTest` 4/4, `AuthRegistrationIntegrationTest` 1/1 em PostgreSQL `16.15` e `.\mvnw.cmd clean test` 105/105 passando) |
| `CONCLUIDO` | `BE-020` | Implementar criptografia de senha com BCrypt | Alta | `BE-002` | Bean `PasswordEncoder` (`BCryptPasswordEncoder` em [PasswordEncoderConfig](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/PasswordEncoderConfig.java), fator configurável `10..31`) validado em contexto Spring (`UniflowApplicationTests`) e testes unitários (`PasswordEncoderConfigTest` 7/7; `.\mvnw.cmd clean test` 94/94 passando), pronto para uso em `BE-019`, `BE-023` e `BE-029` |
| `CONCLUIDO` | `BE-021` | Configurar Spring Security | Alta | `BE-008`, `BE-012` | [SecurityConfig](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/SecurityConfig.java) com `SecurityFilterChain` `STATELESS`, CORS parametrizado, rotas públicas restritas (`POST /auth/register`, `POST /auth/login`, `POST /auth/refresh` + OpenAPI/Swagger condicionados por perfil) e handlers `401`/`403` em `ProblemDetail` via `HandlerExceptionResolver` (`SecurityConfigWebMvcTest` 5/5, `SecuritySwaggerEnabledWebMvcTest` 1/1, `SecurityIntegrationTest` 2/2 e `.\mvnw.cmd clean test` 113/113 passando) |
| `CONCLUIDO` | `BE-022` | Implementar autenticação com JWT | Alta | `BE-021`, `BE-023` | [JwtAuthenticationFilter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/security/JwtAuthenticationFilter.java), [AuthenticatedUser.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/security/AuthenticatedUser.java) e [SecurityConfig.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/SecurityConfig.java) validando assinatura, expiração e status ativo via `SecurityContext` (`JwtAuthenticationFilterTest` 9/9, `JwtAuthenticationFilterWebMvcTest` 2/2, `JwtAuthenticationIntegrationTest` 1/1) |
| `CONCLUIDO` | `BE-023` | Implementar geração de access token | Alta | `BE-016`, `BE-020` | [JwtService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/JwtService.java) + `POST /api/v1/auth/login` em [AuthController](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) / [AuthService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/AuthService.java) emitindo JWT HS256 com `sub`, `jti`, `iat`, `exp` via env var, erro genérico `401` em credenciais inválidas ou status não autenticável e `refreshToken` reservado para `BE-024` (`JwtServiceTest` 7/7, `AuthServiceTest` 9/9, `AuthControllerWebMvcTest` 7/7, `AuthLoginIntegrationTest` 2/2 e `.\mvnw.cmd clean test` 130/130 passando) |
| `CONCLUIDO` | `BE-024` | Implementar refresh token com rotação | Alta | `BE-015`, `BE-023` | Entidade [RefreshToken](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/entity/RefreshToken.java), repositório [RefreshTokenRepository](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/repository/RefreshTokenRepository.java) (`PESSIMISTIC_WRITE` + revogação em lote) e [RefreshTokenService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/RefreshTokenService.java) (SHA-256 hash, rotação atômica, bloqueio de reuso com `noRollbackFor` e emissão integrada em `AuthService.login`: `RefreshTokenTest` 2/2, `RefreshTokenServiceTest` 13/13, `RefreshTokenIntegrationTest` 4/4, `AuthLoginIntegrationTest` 2/2) |
| `CONCLUIDO` | `BE-025` | Implementar renovação de sessão | Alta | `BE-024` | [RefreshTokenRequest](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/RefreshTokenRequest.java), `AuthService.refresh` (`noRollbackFor`) e `POST /api/v1/auth/refresh` em [AuthController](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) rotacionando refresh token, emitindo novo JWT e bloqueando reuso com `401` (`RefreshTokenRequestValidationTest` 5/5, `AuthServiceTest` 11/11, `AuthControllerWebMvcTest` 10/10, `AuthRefreshIntegrationTest` 2/2) |
| `CONCLUIDO` | `BE-026` | Implementar logout e revogação de sessão | Alta | `BE-024`, `BE-022` | [LogoutRequest](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/LogoutRequest.java), `RefreshTokenService.revokeTokenForUser` (validação de ownership `403 FORBIDDEN_OPERATION` + idempotência), `AuthService.logout` e `POST /api/v1/auth/logout` (`204 No Content`, limpeza de `SecurityContext`) validados (`LogoutRequestValidationTest` 5/5, `RefreshTokenServiceTest` 16/16, `AuthServiceTest` 14/14, `AuthControllerWebMvcTest` 14/14, `AuthLogoutIntegrationTest` 2/2) |
| `CONCLUIDO` | `BE-027` | Implementar consulta do usuário autenticado | Alta | `BE-022` | [CurrentUserService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/security/CurrentUserService.java) e `GET /api/v1/auth/me` em [AuthController](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) / [AuthService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/AuthService.java) lendo identidade exclusivamente do `SecurityContext`, revalidando status ativo no banco e ignorando `usuarioId` enviado pelo cliente (`CurrentUserServiceTest` 4/4, `AuthServiceTest` 16/16, `AuthControllerWebMvcTest` 16/16, `AuthMeIntegrationTest` 2/2) |
| `CONCLUIDO` | `BE-028` | Implementar atualização do perfil | Média | `BE-027` | `GET/PATCH /api/v1/usuarios/me` em [UsuarioController](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/controller/UsuarioController.java), [UsuarioService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/service/UsuarioService.java), [UsuarioMapper](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/mapper/UsuarioMapper.java), [UpdateProfileRequest](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/dto/UpdateProfileRequest.java) e [UsuarioResponse](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/dto/UsuarioResponse.java) atualizando `nome` e/ou `email` com Bean Validation, checagem de unicidade `409` (`ConstraintViolationUtils`) e leitura exclusiva do `SecurityContext` (`UpdateProfileRequestValidationTest` 13/13, `UsuarioMapperTest` 2/2, `UsuarioServiceTest` 6/6, `UsuarioControllerWebMvcTest` 4/4, `UsuarioProfileIntegrationTest` 2/2) |
| `CONCLUIDO` | `BE-029` | Implementar alteração de senha | Média | `BE-020`, `BE-027` | `PATCH /api/v1/auth/password` em [AuthController](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) / [AuthService](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/AuthService.java) com [ChangePasswordRequest](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/ChangePasswordRequest.java) (`senhaAtual`, `novaSenha` forte via [PasswordPolicy](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/PasswordPolicy.java)), validação BCrypt da senha atual (`422`), bloqueio de reutilização da senha vigente, novo hash BCrypt e revogação de todos os refresh tokens ativos da conta (`204 No Content`) (`ChangePasswordRequestValidationTest` 17/17, `AuthServiceTest` 20/20, `AuthPasswordWebMvcTest` 3/3 com `addFilters = true`, `AuthPasswordIntegrationTest` 2/2) |
| `PENDENTE` | `BE-030` | Implementar validações de segurança e autorização | Alta | `BE-022`, `BE-027` | Auditoria e helpers centrais de ownership/usuário autenticado impedindo qualquer confiança em `usuarioId` enviado pelo cliente |
| `PENDENTE` | `BE-031` | Criar testes de autenticação e cadastro | Alta | `BE-019`..`BE-030` | Testes unitários e de integração cobrindo registro, e-mail duplicado, login, rotação refresh, reuso bloqueado, logout e `401`/`403` |

---

## Épico 3: Semestres acadêmicos (`MVP 1.0` — `MOD-03`)

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-032` | Criar entidade Semestre | Alta | `BE-013`, `BE-033` | Entidade `Semestre` em `semestre.entity` (`nome`, `instituicao`, `curso`, `periodoAcademico`, `dataInicial`, `dataFinal`, `status`, `atual`, `usuario`) |
| `PENDENTE` | `BE-033` | Criar enum de status do semestre | Alta | `BE-005` | Enum `StatusSemestre` (`PLANEJADO`, `EM_ANDAMENTO`, `CONCLUIDO`) em `semestre.enums` |
| `PENDENTE` | `BE-034` | Criar migration da tabela semestre | Alta | `BE-015` | `V3__create_semestres_table.sql` com `CHECK (data_inicial < data_final)` e índice único parcial `WHERE atual = true` por usuário |
| `PENDENTE` | `BE-035` | Criar SemestreRepository | Alta | `BE-032`, `BE-034` | `SemestreRepository` com queries filtradas por `usuario.id`, paginação e busca de semestre atual |
| `PENDENTE` | `BE-036` | Criar DTOs de semestre | Alta | `BE-009`, `BE-033` | `SemestreRequest`, `SemestreResponse` e `SemestreMapper` sem expor entidade JPA |
| `PENDENTE` | `BE-037` | Implementar cadastro de semestre | Alta | `BE-035`, `BE-036`, `BE-043`, `BE-044` | `POST /api/v1/semestres` associando automaticamente ao usuário autenticado do `SecurityContext` |
| `PENDENTE` | `BE-038` | Implementar listagem de semestres | Alta | `BE-035`, `BE-036` | `GET /api/v1/semestres` paginado (`PageResponse`) retornando apenas semestres do usuário autenticado |
| `PENDENTE` | `BE-039` | Implementar consulta de semestre por ID | Alta | `BE-035`, `BE-030` | `GET /api/v1/semestres/{id}` validando ownership (bloqueia acesso de outro usuário) |
| `PENDENTE` | `BE-040` | Implementar edição de semestre | Média | `BE-039`, `BE-043`, `BE-044` | `PUT /api/v1/semestres/{id}` validando ownership, datas e regra de semestre atual |
| `PENDENTE` | `BE-041` | Implementar exclusão de semestre | Média | `BE-039` | `DELETE /api/v1/semestres/{id}` impedindo exclusão silenciosa (`409`) quando existirem disciplinas/dados dependentes |
| `PENDENTE` | `BE-042` | Implementar definição do semestre atual | Alta | `BE-039`, `BE-043` | `PATCH /api/v1/semestres/{id}/atual` transacional definindo o semestre selecionado como atual |
| `PENDENTE` | `BE-043` | Impedir mais de um semestre atual por usuário | Alta | `BE-034`, `BE-035` | Desmarcação transacional do semestre atual anterior + garantia via constraint única parcial no PostgreSQL |
| `PENDENTE` | `BE-044` | Implementar validações de datas acadêmicas | Alta | `BE-036` | Invariante `dataInicial < dataFinal` validada no backend + `ProblemDetail` claro em violação |
| `PENDENTE` | `BE-045` | Criar testes do módulo de semestres | Alta | `BE-037`..`BE-044` | Testes unitários e de integração cobrindo CRUD, unicidade de semestre atual, datas inválidas, bloqueio de exclusão com dependências e isolamento |

---

## Épico 4: Disciplinas (`MVP 1.0` — `MOD-04`)

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-046` | Criar entidade Disciplina | Alta | `BE-032` | Entidade `Disciplina` (`nome`, `codigo`, `professor`, `cargaHoraria`, `sala`, `corHex`, `semestre`) |
| `PENDENTE` | `BE-047` | Criar migration da tabela disciplina | Alta | `BE-034` | `V4__create_disciplinas_table.sql` com FK para `semestres`, índice em `semestre_id` e `CHECK` de `carga_horaria > 0` |
| `PENDENTE` | `BE-048` | Criar DisciplinaRepository | Alta | `BE-046`, `BE-047` | Queries filtrando por `semestreId` e ownership `semestre.usuario.id` |
| `PENDENTE` | `BE-049` | Criar DTOs de disciplina | Alta | `BE-009` | `DisciplinaRequest`, `DisciplinaResponse` e `DisciplinaMapper` validando `nome`, `corHex` e `cargaHoraria` positiva |
| `PENDENTE` | `BE-050` | Implementar cadastro de disciplina | Alta | `BE-048`, `BE-049`, `BE-056`, `BE-057` | `POST /api/v1/semestres/{semestreId}/disciplinas` (ou `/api/v1/disciplinas`) garantindo vínculo com semestre do usuário |
| `PENDENTE` | `BE-051` | Implementar listagem de disciplinas | Alta | `BE-048`, `BE-049`, `BE-056` | `GET /api/v1/disciplinas` retornando apenas disciplinas pertencentes aos semestres do usuário autenticado |
| `PENDENTE` | `BE-052` | Implementar consulta por ID | Alta | `BE-048`, `BE-057` | `GET /api/v1/disciplinas/{id}` com validação de ownership |
| `PENDENTE` | `BE-053` | Implementar edição de disciplina | Média | `BE-052`, `BE-057` | `PUT /api/v1/disciplinas/{id}` atualizando dados e validando novo semestre se alterável |
| `PENDENTE` | `BE-054` | Implementar exclusão de disciplina | Média | `BE-052`, `BE-057` | `DELETE /api/v1/disciplinas/{id}` protegida contra acesso indevido e com política clara de dependências |
| `PENDENTE` | `BE-055` | Implementar filtro por semestre | Alta | `BE-051` | Suporte a `?semestreId=` em `GET /api/v1/disciplinas` e `GET /api/v1/semestres/{semestreId}/disciplinas` |
| `PENDENTE` | `BE-056` | Implementar associação com o usuário proprietário | Alta | `BE-039`, `BE-048` | Toda operação vincula e consulta a cadeia `disciplina -> semestre -> usuarioAutenticado` |
| `PENDENTE` | `BE-057` | Implementar validações de propriedade e acesso | Alta | `BE-030`, `BE-056` | Impedir associação/leitura/alteração de disciplina em semestre de outro usuário |
| `PENDENTE` | `BE-058` | Criar testes do módulo de disciplinas | Alta | `BE-050`..`BE-057` | Testes unitários e de integração validando CRUD, filtro por semestre e bloqueio cross-user |

---

## Épico 5: Aulas e horários (`MVP 1.0` — `MOD-05`)

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-059` | Criar entidade Aula | Alta | `BE-046` | Entidade `Aula` em `aula.entity` (`disciplina`, `diaSemana`, `horaInicial`, `horaFinal`, `sala`) |
| `PENDENTE` | `BE-060` | Criar migration da tabela aula | Alta | `BE-047` | `V5__create_aulas_table.sql` com `CHECK (hora_inicial < hora_final)` e índice em `(disciplina_id, dia_semana)` |
| `PENDENTE` | `BE-061` | Criar AulaRepository | Alta | `BE-059`, `BE-060` | Consultas por disciplina, semestre, dia da semana e usuário proprietário |
| `PENDENTE` | `BE-062` | Criar DTOs de aula | Alta | `BE-009` | `AulaRequest`, `AulaResponse` (incluindo lista `conflitos` informativos) e `AulaMapper` |
| `PENDENTE` | `BE-063` | Implementar cadastro de horário de aula | Alta | `BE-057`, `BE-061`, `BE-062`, `BE-068` | `POST /api/v1/disciplinas/{disciplinaId}/aulas` validando `horaInicial < horaFinal` e ownership da disciplina |
| `PENDENTE` | `BE-064` | Implementar listagem de aulas | Alta | `BE-061`, `BE-062` | `GET /api/v1/disciplinas/{disciplinaId}/aulas` e `GET /api/v1/aulas` filtradas por usuário autenticado |
| `PENDENTE` | `BE-065` | Implementar edição de horário | Média | `BE-063`, `BE-068` | `PUT /api/v1/aulas/{id}` validando ownership, horário válido e reavaliando conflitos |
| `PENDENTE` | `BE-066` | Implementar exclusão de horário | Média | `BE-064` | `DELETE /api/v1/aulas/{id}` validando ownership |
| `PENDENTE` | `BE-067` | Implementar recorrência semanal | Alta | `BE-064` | `GET /api/v1/aulas/grade-semanal` agrupando horários recorrentes ordenados por dia da semana e `horaInicial` |
| `PENDENTE` | `BE-068` | Implementar validação de conflitos de horários | Alta | `BE-061` | Detecta sobreposição (`iniA < fimB && iniB < fimA`) no mesmo dia/semestre e **informa os conflitos sem bloquear** o cadastro |
| `PENDENTE` | `BE-069` | Implementar consulta de aulas por dia da semana | Alta | `BE-064` | `GET /api/v1/aulas?diaSemana=...` (e/ou `/dia/{diaSemana}`) retornando aulas ordenadas cronologicamente |
| `PENDENTE` | `BE-070` | Criar testes do módulo de aulas | Alta | `BE-063`..`BE-069` | Testes cobrindo intervalo inválido, bordas adjacentes sem falso conflito, detecção de conflito não bloqueante e grade semanal |

---

## Épico 6: Atividades acadêmicas (`MVP 1.0` — `MOD-06`)

> **Regra importante**: **`ATRASADA` deve ser um estado calculado com base no prazo e na conclusão, não necessariamente um status persistido no banco.**

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-071` | Criar entidade Atividade | Alta | `BE-013`, `BE-046`, `BE-072`, `BE-073` | Entidade `Atividade` (`usuario`, `disciplina` opcional, `titulo`, `descricao`, `tipo`, `status`, `prioridade`, `prazo`, `tempoEstimadoMinutos`, `concluidaEm`) |
| `PENDENTE` | `BE-072` | Criar enum de tipo de atividade | Alta | `BE-005` | Enum `TipoAtividade` (`TAREFA`, `PROVA`, `TRABALHO`, `SEMINARIO`, `LEITURA`, `REVISAO`, `OUTRO`) |
| `PENDENTE` | `BE-073` | Criar enum de status da atividade | Alta | `BE-005` | Enums `StatusAtividade` (`PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `ATRASADA` como status lógico/calculado) e `PrioridadeAtividade` (`BAIXA`, `MEDIA`, `ALTA`, `URGENTE`) |
| `PENDENTE` | `BE-074` | Criar migration da tabela atividade | Alta | `BE-015`, `BE-047` | `V6__create_atividades_table.sql` com `CHECK (tempo_estimado_minutos IS NULL OR tempo_estimado_minutos > 0)` e índices `(usuario_id, status, prazo)` |
| `PENDENTE` | `BE-075` | Criar AtividadeRepository | Alta | `BE-071`, `BE-074` | Repositório com Specifications/queries filtráveis por usuário, disciplina, status, período e atraso |
| `PENDENTE` | `BE-076` | Criar DTOs de atividade | Alta | `BE-009`, `BE-072`, `BE-073` | `AtividadeRequest`, `AtividadeResponse` (expondo `statusEfetivo` e flag `atrasada`) e `AtividadeMapper` |
| `PENDENTE` | `BE-077` | Implementar cadastro de atividade | Alta | `BE-057`, `BE-075`, `BE-076` | `POST /api/v1/atividades` validando ownership da `disciplinaId` opcional e `tempoEstimadoMinutos > 0` |
| `PENDENTE` | `BE-078` | Implementar listagem de atividades | Alta | `BE-075`, `BE-076` | `GET /api/v1/atividades` paginado (`PageResponse`) isolado ao usuário autenticado |
| `PENDENTE` | `BE-079` | Implementar consulta por ID | Alta | `BE-075`, `BE-030` | `GET /api/v1/atividades/{id}` com validação estrita de ownership |
| `PENDENTE` | `BE-080` | Implementar edição de atividade | Média | `BE-079` | `PUT /api/v1/atividades/{id}` validando ownership da atividade e de eventual nova disciplina |
| `PENDENTE` | `BE-081` | Implementar exclusão de atividade | Média | `BE-079` | `DELETE /api/v1/atividades/{id}` com validação de ownership |
| `PENDENTE` | `BE-082` | Implementar conclusão de atividade | Alta | `BE-079` | `PATCH /api/v1/atividades/{id}/concluir` marcando `CONCLUIDA` e gravando `concluidaEm = Instant.now()` |
| `PENDENTE` | `BE-083` | Implementar alteração de status | Alta | `BE-079`, `BE-082` | `PATCH /api/v1/atividades/{id}/status` gerenciando transições válidas (`PENDENTE`/`EM_ANDAMENTO`/`CONCLUIDA`, ajustando `concluidaEm`) |
| `PENDENTE` | `BE-084` | Implementar filtro por disciplina | Alta | `BE-078` | Filtro `?disciplinaId=` em `GET /api/v1/atividades` |
| `PENDENTE` | `BE-085` | Implementar filtro por status | Média | `BE-078`, `BE-087` | Filtro `?status=` considerando corretamente `ATRASADA` como estado calculado (`status != CONCLUIDA && prazo < now`) |
| `PENDENTE` | `BE-086` | Implementar filtro por período | Média | `BE-078` | Filtros `?inicio=&fim=` (UTC / timezone) sobre `prazo` em `GET /api/v1/atividades` |
| `PENDENTE` | `BE-087` | Implementar identificação de atividades atrasadas | Alta | `BE-071`, `BE-076` | Regra de domínio calculando `ATRASADA` dinamicamente quando `prazo != null && prazo < now && status != CONCLUIDA` |
| `PENDENTE` | `BE-088` | Implementar ordenação por prazo | Média | `BE-078` | Ordenação consistente por prazo ascendente/descendente (com tratamento adequado de `prazo == null`) |
| `PENDENTE` | `BE-089` | Criar testes do módulo de atividades | Alta | `BE-077`..`BE-088` | Testes unitários e integração cobrindo cálculo dinâmico de `ATRASADA`, conclusão, filtros combinados, paginação e ownership |

---

## Épico 7: Trabalhos e etapas (`MVP 1.0` — `MOD-08`)

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-090` | Criar entidade Trabalho | Alta | `BE-013`, `BE-046` | Entidade `Trabalho` (`criador`, `disciplina`, `titulo`, `descricao`, `instrucoes`, `prazoFinal`, `status`, `prioridade`, `tempoEstimadoMinutos`, `compartilhado`, `grupoId` nullable para v1.1) |
| `PENDENTE` | `BE-091` | Criar entidade EtapaTrabalho | Alta | `BE-090` | Entidade `EtapaTrabalho` (`trabalho`, `titulo`, `descricao`, `prazo`, `status`, `ordem`, `responsavelId` nullable para v1.1, `concluidaEm`) |
| `PENDENTE` | `BE-092` | Criar migrations das tabelas | Alta | `BE-047`, `BE-074` | `V7__create_trabalhos_table.sql` (`trabalhos`, `etapas_trabalho`, `trabalho_atividades`) validada pelo Flyway/Hibernate |
| `PENDENTE` | `BE-093` | Criar TrabalhoRepository | Alta | `BE-090`, `BE-091`, `BE-092` | Repositórios de `Trabalho` e `EtapaTrabalho` com queries filtradas por criador/disciplina e ordenação de etapas |
| `PENDENTE` | `BE-094` | Criar DTOs de trabalho | Alta | `BE-009` | DTOs de criação/edição/resposta de `Trabalho` e `EtapaTrabalho` incluindo `progressoPercentual` |
| `PENDENTE` | `BE-095` | Implementar cadastro de trabalho | Alta | `BE-057`, `BE-093`, `BE-094`, `BE-106` | `POST /api/v1/trabalhos` validando disciplina autorizada do usuário autenticado |
| `PENDENTE` | `BE-096` | Implementar listagem de trabalhos | Alta | `BE-093`, `BE-094` | `GET /api/v1/trabalhos` paginado com filtro por disciplina/status isolado ao usuário |
| `PENDENTE` | `BE-097` | Implementar consulta por ID | Alta | `BE-093`, `BE-030` | `GET /api/v1/trabalhos/{id}` retornando trabalho, etapas ordenadas e progresso |
| `PENDENTE` | `BE-098` | Implementar edição de trabalho | Média | `BE-097`, `BE-106` | `PUT /api/v1/trabalhos/{id}` validando ownership e prazos |
| `PENDENTE` | `BE-099` | Implementar exclusão de trabalho | Média | `BE-097` | `DELETE /api/v1/trabalhos/{id}` validando ownership |
| `PENDENTE` | `BE-100` | Implementar cadastro de etapas | Alta | `BE-097`, `BE-105`, `BE-106` | `POST /api/v1/trabalhos/{id}/etapas` adicionando etapa ao trabalho autorizado |
| `PENDENTE` | `BE-101` | Implementar edição de etapas | Média | `BE-100`, `BE-106` | `PUT /api/v1/trabalhos/{id}/etapas/{etapaId}` validando pertencimento ao trabalho e usuário |
| `PENDENTE` | `BE-102` | Implementar conclusão de etapas | Alta | `BE-100`, `BE-103`, `BE-104` | `PATCH /api/v1/trabalhos/{id}/etapas/{etapaId}/concluir` registrando conclusão e atualizando progresso |
| `PENDENTE` | `BE-103` | Implementar acompanhamento do progresso | Alta | `BE-100`, `BE-104` | Exposição de total de etapas, etapas concluídas e `progressoPercentual` em respostas e consultas |
| `PENDENTE` | `BE-104` | Implementar cálculo percentual de conclusão | Alta | `BE-091` | Regra pura calculando `(concluidas * 100.0) / total` (retornando `0` se sem etapas) |
| `PENDENTE` | `BE-105` | Implementar ordenação das etapas | Média | `BE-091`, `BE-093` | Campo `ordem` consistente + suporte a reordenação (`PATCH /api/v1/trabalhos/{id}/etapas/reordenar`) |
| `PENDENTE` | `BE-106` | Implementar validação de prazos | Alta | `BE-094` | Validar prazos coerentes (ex.: prazo de etapa não posterior ao `prazoFinal` do trabalho quando ambos informados) |
| `PENDENTE` | `BE-107` | Criar testes do módulo de trabalhos | Alta | `BE-095`..`BE-106` | Testes unitários e integração cobrindo CRUD, etapas, cálculo percentual, ordenação, prazos e ownership |

---

## Épico 8: Dashboard e calendário (`MVP 1.0` — `MOD-07` e Dashboard)

> **Regra de negócio (`MOD-07`)**: O calendário é uma visão consolidada de registros já existentes (`aula`, `atividade`, `trabalho`), **sem duplicação de dados em tabela própria**.

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-108` | Implementar endpoint de resumo do dashboard | Alta | `BE-109`..`BE-113` | `GET /api/v1/dashboard` consolidando semestre atual, contadores, trabalhos e próximas entregas do usuário |
| `PENDENTE` | `BE-109` | Implementar total de atividades pendentes | Alta | `BE-075`, `BE-087` | Contagem exata de atividades não concluídas e não atrasadas (excluindo `CONCLUIDA`) |
| `PENDENTE` | `BE-110` | Implementar total de atividades concluídas | Alta | `BE-075` | Contagem exata de atividades concluídas do usuário autenticado |
| `PENDENTE` | `BE-111` | Implementar total de atividades atrasadas | Alta | `BE-075`, `BE-087` | Contagem baseada na regra calculada de atraso (`prazo < now && status != CONCLUIDA`) |
| `PENDENTE` | `BE-112` | Implementar resumo de trabalhos em andamento | Alta | `BE-093`, `BE-104` | Lista/resumo de trabalhos ativos com prazo e percentual de progresso |
| `PENDENTE` | `BE-113` | Implementar próximas atividades | Alta | `BE-075`, `BE-088` | Listagem ordenada das próximas entregas dentro de janela configurável (ex.: próximos 7 dias) |
| `PENDENTE` | `BE-114` | Implementar endpoint de calendário por período | Alta | `BE-061`, `BE-075`, `BE-093` | `GET /api/v1/calendario` (com visões mensal, semanal e diária e suporte a `timezone` IANA) sem tabela duplicada |
| `PENDENTE` | `BE-115` | Integrar aulas recorrentes ao calendário | Alta | `BE-067`, `BE-114` | Projeção das aulas recorrentes no intervalo `[inicio, fim]` respeitando vigência `[dataInicial, dataFinal]` do semestre |
| `PENDENTE` | `BE-116` | Integrar atividades ao calendário | Alta | `BE-086`, `BE-114` | Inclusão de atividades/provas com `prazo` no intervalo como eventos tipados com cor da disciplina |
| `PENDENTE` | `BE-117` | Integrar prazos de trabalhos ao calendário | Alta | `BE-093`, `BE-114` | Inclusão dos trabalhos com `prazoFinal` no intervalo solicitado |
| `PENDENTE` | `BE-118` | Implementar filtros do calendário | Média | `BE-114`..`BE-117` | Filtros por `semestreId`, `disciplinaId` e `tipos` (`AULA`, `ATIVIDADE`, `TRABALHO`) |
| `PENDENTE` | `BE-119` | Criar testes de dashboard e calendário | Alta | `BE-108`..`BE-118` | Testes validando contadores, não duplicação, fusos horários e isolamento total entre usuários |

---

## Épico 9: Grupos e colaboração (`Versão 1.1` — `MOD-09`)

> **Pré-condição**: Executar na Versão 1.1 após estabilização do núcleo MVP 1.0 (`Épicos 1 a 8`).

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-120` | Criar entidade Grupo | Média | `BE-013` | Entidade `Grupo` (`id`, `nome`, `descricao`, `criador`, `createdAt`, `updatedAt`) |
| `PENDENTE` | `BE-121` | Criar entidade GrupoParticipante | Média | `BE-120` | Entidade `GrupoParticipante` (`grupo`, `usuario`, `papel`: `ADMINISTRADOR`/`PARTICIPANTE`, `ingressouEm`) |
| `PENDENTE` | `BE-122` | Criar entidade ConviteGrupo | Média | `BE-120` | Entidade `ConviteGrupo` (`grupo`, `convidado`, `convidadoPor`, `status`: `PENDENTE`/`ACEITO`/`RECUSADO`) |
| `PENDENTE` | `BE-123` | Criar migrations de grupos | Média | `BE-015`, `BE-120`..`BE-122` | `V8__create_grupos_tables.sql` com unicidade de participante e convite pendente |
| `PENDENTE` | `BE-124` | Implementar criação de grupo | Média | `BE-123` | `POST /api/v1/grupos` criando o grupo e registrando o criador como `ADMINISTRADOR` |
| `PENDENTE` | `BE-125` | Implementar convite de participantes | Média | `BE-124`, `BE-130` | `POST /api/v1/grupos/{id}/convites` restrito a `ADMINISTRADOR` |
| `PENDENTE` | `BE-126` | Implementar aceitação e rejeição de convites | Média | `BE-125` | `POST /api/v1/grupos/convites/{conviteId}/{aceitar\|recusar}` exclusivo ao convidado; aceitar insere `PARTICIPANTE` |
| `PENDENTE` | `BE-127` | Implementar remoção de participantes | Média | `BE-124`, `BE-130` | `DELETE /api/v1/grupos/{id}/participantes/{usuarioId}` restrito a `ADMINISTRADOR` (protegendo último admin) |
| `PENDENTE` | `BE-128` | Implementar listagem de membros | Média | `BE-124`, `BE-131` | `GET /api/v1/grupos/{id}/participantes` acessível apenas a participantes do grupo |
| `PENDENTE` | `BE-129` | Implementar trabalhos compartilhados | Média | `BE-092`, `BE-124` | Vincular trabalhos ao grupo (`compartilhado=true`, `grupoId`) e atribuir responsável por etapa (`EtapaTrabalho`) |
| `PENDENTE` | `BE-130` | Implementar permissões por participante | Média | `BE-121` | Verificadores de papel (`ADMINISTRADOR` vs `PARTICIPANTE`) para ações administrativas e edição |
| `PENDENTE` | `BE-131` | Implementar controle de acesso aos recursos compartilhados | Alta | `BE-129`, `BE-130` | Garantir que apenas membros aceitos acessem trabalhos/atividades compartilhadas e **jamais** recursos privados de outros membros |
| `PENDENTE` | `BE-132` | Criar testes de colaboração | Média | `BE-124`..`BE-131` | Testes de integração cobrindo convite, papéis, compartilhamento e bloqueio de vazamento de dados privados |

---

## Épico 10: Arquivos e notificações (`Versão 1.1` — `MOD-10`, `MOD-11`)

> **Regra de arquivos (`MOD-10`)**: Não armazenar binários (`BYTEA`/`OID`) no PostgreSQL; usar Object Storage, validar extensão/tamanho, sanitizar nome e verificar autorização antes de download.

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `PENDENTE` | `BE-133` | Criar entidade Arquivo | Média | `BE-013`, `BE-046`, `BE-090` | Entidade `Arquivo` (somente metadados + `storageKey`, associação opcional a `disciplina` ou `trabalho`) e migration `V9__create_arquivos_table.sql` |
| `PENDENTE` | `BE-134` | Criar integração com armazenamento de arquivos | Média | `BE-006` | Interface `ObjectStorageService` com implementação local `dev` e S3 compatível para produção |
| `PENDENTE` | `BE-135` | Implementar upload de arquivos | Média | `BE-133`, `BE-134`, `BE-138` | `POST /api/v1/arquivos` gerando `storageKey` segura no backend sem confiar no filename enviado |
| `PENDENTE` | `BE-136` | Implementar download de arquivos | Média | `BE-135`, `BE-131` | `GET /api/v1/arquivos/{id}/download` (e listagem) verificando autorização (dono ou membro do grupo) |
| `PENDENTE` | `BE-137` | Implementar exclusão de arquivos | Média | `BE-135` | `DELETE /api/v1/arquivos/{id}` removendo objeto no storage e registro após checar autorização |
| `PENDENTE` | `BE-138` | Implementar validação de extensão e tamanho | Alta | `BE-009` | Whitelist de extensões/MIME types e limite de tamanho com rejeição segura (`400`/`415`/`413`) |
| `PENDENTE` | `BE-139` | Criar entidade Notificacao | Média | `BE-013` | Entidade `Notificacao` (`PRAZO_PROXIMO`, `ATIVIDADE_ATRASADA`, `NOVO_CONVITE`, `ATIVIDADE_ATRIBUIDA`, `TRABALHO_ATUALIZADO`) + `V10__create_notificacoes_table.sql` |
| `PENDENTE` | `BE-140` | Implementar geração de notificações | Média | `BE-139`, `BE-125`, `BE-129` | Serviço interno gerando notificações de eventos de trabalho/convite e prazos sem duplicação indevida |
| `PENDENTE` | `BE-141` | Implementar listagem de notificações | Média | `BE-139` | `GET /api/v1/notificacoes` paginado, filtro por `lida` e contagem de não lidas apenas do usuário autenticado |
| `PENDENTE` | `BE-142` | Implementar marcação como lida | Média | `BE-141` | `PATCH /api/v1/notificacoes/{id}/lida` e `PATCH /api/v1/notificacoes/marcar-todas-lidas` validando ownership |
| `PENDENTE` | `BE-143` | Implementar notificações de prazos | Média | `BE-075`, `BE-093`, `BE-140` | Rotina agendada gerando alertas `PRAZO_PROXIMO` e `ATIVIDADE_ATRASADA` sem duplicar alertas para a mesma janela |
| `PENDENTE` | `BE-144` | Criar testes de arquivos e notificações | Média | `BE-135`..`BE-143` | Testes cobrindo upload inválido, download não autorizado, sanitização, listagem/leitura e geração de notificações |

---

## Épico 11: Planejamento inteligente e IA (`Versão 2.0` — `MOD-12`)

> ⚠️ **Regra de Produto (MVP Gate)**: Não implementar funcionalidades da Versão 2.0 antes da validação do MVP 1.0, salvo autorização explícita do usuário.

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `FORA_DO_MVP` | `BE-145` | Criar serviço de planejamento acadêmico | Baixa | Validação MVP + `Épico 6`, `Épico 7` | Núcleo de domínio `planejamento` estruturado (sessões/blocos de estudo) |
| `FORA_DO_MVP` | `BE-146` | Implementar cálculo de prioridade de atividades | Baixa | `BE-145` | Algoritmo de score considerando prazo, prioridade, peso e atraso |
| `FORA_DO_MVP` | `BE-147` | Implementar sugestões de distribuição de tarefas | Baixa | `BE-146`, `Épico 5` | Distribuição heurística respeitando janela livre da grade semanal de aulas |
| `FORA_DO_MVP` | `BE-148` | Criar integração com provedor de IA | Baixa | `BE-145` | Interface cliente de IA isolada com timeout, fallback e variáveis de ambiente |
| `FORA_DO_MVP` | `BE-149` | Implementar geração de sugestões de cronograma | Baixa | `BE-147`, `BE-148` | Endpoint gerando proposta estruturada de cronograma semanal |
| `FORA_DO_MVP` | `BE-150` | Implementar validação das sugestões geradas | Média | `BE-149` | Validador determinístico evitando conflitos com aulas e durações inválidas |
| `FORA_DO_MVP` | `BE-151` | Criar testes do planejamento inteligente | Baixa | `BE-145`..`BE-150` | Testes com mocks do provedor de IA e validação das regras determinísticas |

---

## Épico 12: Monetização (`Versão Futura` — `MOD-13`)

> ⚠️ **Regra de Produto (`MOD-13`)**: Não implementar pagamentos fictícios no MVP. Somente integrar gateway real após validação comercial.

| Status | ID | Tarefa | Prioridade | Depende de | Critério de Aceite Principal |
| :--- | :--- | :--- | :---: | :--- | :--- |
| `FORA_DO_MVP` | `BE-152` | Criar entidade Assinatura | Baixa | Validação comercial MVP | Entidade `Assinatura` + `Plano` + migration `V11__create_assinaturas_tables.sql` |
| `FORA_DO_MVP` | `BE-153` | Criar planos gratuitos e premium | Baixa | `BE-152` | Seed/catálogo de planos (`GET /api/v1/assinaturas/planos`) |
| `FORA_DO_MVP` | `BE-154` | Implementar controle de recursos por plano | Baixa | `BE-153` | Enforcement de limites (semestres/arquivos/IA) por assinatura ativa |
| `FORA_DO_MVP` | `BE-155` | Integrar gateway de pagamento | Baixa | `BE-153` | Interface de pagamento integrada a gateway real com assinatura/checkout |
| `FORA_DO_MVP` | `BE-156` | Implementar confirmação de pagamento por webhook | Baixa | `BE-155` | Endpoint webhook com validação criptográfica de assinatura e idempotência |
| `FORA_DO_MVP` | `BE-157` | Implementar cancelamento de assinatura | Baixa | `BE-155` | Cancelamento controlado preservando ciclo pago e histórico de pagamentos |
| `FORA_DO_MVP` | `BE-158` | Criar testes de monetização | Baixa | `BE-152`..`BE-157` | Testes cobrindo webhook válido/inválido, limites por plano e cancelamento |
