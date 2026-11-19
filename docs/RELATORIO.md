# Relatório de Andamento — UniFlow Backend (`uniflow-backend`)

Este documento registra a evolução cronológica, entregas verificadas, métricas de qualidade e riscos técnicos do backend do **UniFlow**.

---

## 1. Resumo Executivo Atual (`2026-11-19`)

- **Fase Atual**: Épico 1 em andamento (`BE-001` a `BE-011` concluídas; `BE-012` em andamento aguardando Docker ativo para `UniflowApplicationTests`).
- **Progresso do MVP 1.0 (Épicos 1–8)**: `11 / 119` tarefas concluídas (`9.2%`), `1` em andamento (`BE-012`).
- **Progresso Versão 1.1 (Épicos 9–10)**: `0 / 25` tarefas concluídas (`0.0%`).
- **Progresso Versão 2.0 / Futura (Épicos 11–12)**: `0 / 14` tarefas (`FORA_DO_MVP`).
- **Progresso Geral**: `11 / 158` tarefas concluídas (`7.0%`).
- **Status do Build**: `.\mvnw.cmd test` -> **`Tests run: 35, Failures: 0, Errors: 1, Skipped: 0`** (`34/34` unitários/Mockito/WebMvcTest passando; `UniflowApplicationTests` falhou por Docker parado no host).
- **Endpoints implementados**: `0` | **Migrations Flyway criadas**: `1` (`V1__init_baseline.sql`) | **Testes passando**: `34 / 35` (`BeanValidationWebMvcTest` 4/4 + `GlobalExceptionHandlerWebMvcTest` 7/7 + `ProblemDetailsFactoryTest` 2/2 + `LogbackLoggingConfigTest` 2/2 + `RequestLoggingFilterMockitoTest` 2/2 + `RequestLoggingFilterWebMvcTest` 2/2 + `SensitiveDataMaskerTest` 3/3 + `OpenApiConfigTest` 1/1 + `OpenApiWebMvcTest` 2/2 + `DatabaseConfigPropertiesTest` 3/3 + `DependenciesSmokeTest` 2/2 + `FlywayConfigTest` 2/2 + `ModularStructureTest` 2/2; `UniflowApplicationTests` `0/1` por falta de daemon Docker).

### Principais Pontos de Atenção
1. Mantido **Spring Boot `4.1.2`** (do esqueleto Initializr) por decisão do usuário em `2026-11-19` (`BE-001` concluída).
2. **Docker Desktop parado no host** (`dockerDesktopLinuxEngine` indisponível): necessário iniciar Docker para concluir `BE-012` (`.\mvnw.cmd test` completo com PostgreSQL Testcontainers) e liberar o Épico 2+.

---

## 2. Indicadores de Cobertura e Qualidade

| Métrica | Valor Atual | Meta MVP 1.0 |
| :--- | :---: | :---: |
| Tarefas `BE-XXX` concluídas | 11 | 119 (`Épicos 1–8`) |
| Tarefas Versão 1.1 concluídas | 0 | 25 (`Épicos 9–10`, pós-MVP) |
| Migrations Flyway em `src/main/resources/db/migration/` | 1 (`V1__init_baseline.sql`) | 7 (`V1`..`V7` no MVP 1.0) |
| Endpoints REST implementados | 0 | Todos os de `MVP 1.0` em [API_CONTRACT.md](file:///c:/uniflow_api/api/docs/API_CONTRACT.md) |
| Compilação (`.\mvnw.cmd test-compile`) | ✅ Passando | ✅ Passando |
| Suíte de testes (`.\mvnw.cmd test`) | ⚠️ `34/35` passando (`UniflowApplicationTests` aguarda Docker ativo no host) | ✅ 100% Passando |

---

## 3. Histórico Cronológico de Execuções

### [2026-11-19] — Execução parcial `BE-012` (`EM_ANDAMENTO`): Configurar testes com JUnit e Mockito
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Criada [TestcontainersConfiguration.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/TestcontainersConfiguration.java) com bean `@ServiceConnection` `PostgreSQLContainer` (`postgres:16-alpine`).
  - Criada classe base [AbstractIntegrationTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/AbstractIntegrationTest.java) (`@SpringBootTest`, `@Import(TestcontainersConfiguration.class)`, `@ActiveProfiles("test")`).
  - Atualizada [UniflowApplicationTests.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/UniflowApplicationTests.java) estendendo `AbstractIntegrationTest` para validar `DataSource`, Flyway `V1__init_baseline.sql`, extensão `pgcrypto`, `OpenAPI`, `Validator`, `GlobalExceptionHandler` e `RequestLoggingFilter`.
  - Criado teste unitário [RequestLoggingFilterMockitoTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/filter/RequestLoggingFilterMockitoTest.java) (`@ExtendWith(MockitoExtension.class)`) cobrindo propagação e limpeza de `MDC` mesmo sob exceção no `FilterChain`.
- **Arquivos criados**: [TestcontainersConfiguration.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/TestcontainersConfiguration.java), [AbstractIntegrationTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/AbstractIntegrationTest.java), [RequestLoggingFilterMockitoTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/filter/RequestLoggingFilterMockitoTest.java)
- **Arquivos modificados**: [UniflowApplicationTests.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/UniflowApplicationTests.java), [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test` -> `Tests run: 35, Failures: 0, Errors: 1, Skipped: 0` (`34/34` unitários/Mockito/WebMvcTest passando; `UniflowApplicationTests` falhou com `Could not find a valid Docker environment`).
- **Pendências identificadas**: Tarefa mantida como `EM_ANDAMENTO`. Necessário iniciar o daemon do Docker Desktop no host e reexecutar `.\mvnw.cmd test` para concluir `BE-012` como `CONCLUIDO` e desbloquear o `Épico 2+`.

### [2026-11-19] — Execução `BE-011`: Configurar logs da aplicação
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Criado [RequestLoggingFilter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/RequestLoggingFilter.java) (`OncePerRequestFilter`, `@Order(Ordered.HIGHEST_PRECEDENCE)`) em `com.uniflow.common.filter` populando `requestId`, `httpMethod` e `requestPath` no `MDC`, devolvendo `X-Request-Id` na resposta, registrando apenas metadados seguros de requisição (`method`, `path` sanitizado, `status`, `durationMs`, sem headers/query/body sensíveis) e limpando o `MDC` em `finally`.
  - Criados [SensitiveDataMasker.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/SensitiveDataMasker.java) e [SensitiveDataMaskingConverter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/SensitiveDataMaskingConverter.java) para mascarar automaticamente tokens `Bearer`/`Basic`, JWTs (`eyJ...`), senhas e credenciais (`password`, `senha`, `senhaHash`, `token`, `accessToken`, `refreshToken`, `authorization`, `secret`, etc.), além de sanitizar quebras de linha contra log injection.
  - Criado [logback-spring.xml](file:///c:/uniflow_api/api/src/main/resources/logback-spring.xml) com timestamp UTC, conversão `%maskedMsg`, `requestId` via MDC e formato estruturado específico para o perfil `prod`.
  - Criadas as suítes [SensitiveDataMaskerTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/filter/SensitiveDataMaskerTest.java), [LogbackLoggingConfigTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/filter/LogbackLoggingConfigTest.java) e [RequestLoggingFilterWebMvcTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/filter/RequestLoggingFilterWebMvcTest.java).
- **Arquivos criados**: `RequestLoggingFilter.java`, `SensitiveDataMasker.java`, `SensitiveDataMaskingConverter.java`, [logback-spring.xml](file:///c:/uniflow_api/api/src/main/resources/logback-spring.xml), `SensitiveDataMaskerTest.java`, `LogbackLoggingConfigTest.java`, `RequestLoggingFilterWebMvcTest.java`
- **Arquivos modificados**: [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest,FlywayConfigTest,ModularStructureTest,GlobalExceptionHandlerWebMvcTest,ProblemDetailsFactoryTest,BeanValidationWebMvcTest,OpenApiConfigTest,OpenApiWebMvcTest,SensitiveDataMaskerTest,LogbackLoggingConfigTest,RequestLoggingFilterWebMvcTest"` -> `Tests run: 32, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Resta apenas `BE-012` (JUnit/Mockito/Testcontainers) para finalizar o Épico 1.

### [2026-11-19] — Execução `BE-010`: Configurar documentação Swagger/OpenAPI
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Criado [OpenApiConfig.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/OpenApiConfig.java) com metadados da API `/api/v1` (`UniFlow Backend API`, versão `v1`), servidor base `/` e esquema de segurança `bearerAuth` (`HTTP` / `bearer` / `JWT`) aplicado globalmente como `SecurityRequirement`.
  - Ajustado [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml) com configurações padrão da Swagger UI (`/swagger-ui.html`, ordenação por método HTTP e tag, persistência de autorização no dev), mantendo habilitação controlada por perfil (`dev` ativo, `prod` controlado via `API_DOCS_ENABLED`/`SWAGGER_ENABLED` e `test` isolado).
  - Criadas as suítes [OpenApiConfigTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/config/OpenApiConfigTest.java) e [OpenApiWebMvcTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/config/OpenApiWebMvcTest.java) validando `/v3/api-docs` e `/swagger-ui.html` -> `/swagger-ui/index.html`.
- **Arquivos criados**: `OpenApiConfig.java`, `OpenApiConfigTest.java`, `OpenApiWebMvcTest.java`
- **Arquivos modificados**: [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml), [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest,FlywayConfigTest,ModularStructureTest,GlobalExceptionHandlerWebMvcTest,ProblemDetailsFactoryTest,BeanValidationWebMvcTest,OpenApiConfigTest,OpenApiWebMvcTest"` -> `Tests run: 25, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Restam `BE-011` (logs estruturados com `requestId`) e `BE-012` (JUnit/Mockito/Testcontainers); em `BE-021`, liberar `/v3/api-docs/**` e `/swagger-ui/**` conforme política de perfil em [SECURITY.md](file:///c:/uniflow_api/api/docs/SECURITY.md).

### [2026-11-19] — Execução `BE-009`: Configurar validação com Bean Validation
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Adicionados handlers de Bean Validation no [GlobalExceptionHandler.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/GlobalExceptionHandler.java) para `MethodArgumentNotValidException` (erros de campo e globais de DTOs `@Valid`), `ConstraintViolationException` (serviços `@Validated` e validadores programáticos, normalizando o caminho do campo sem expor nós internos de método/parâmetro) e `HandlerMethodValidationException` (validação nativa de `@RequestParam`/`@PathVariable` no Spring WebMVC 7).
  - Todas as violações mapeadas de forma uniforme via `ProblemDetailsFactory` para `400 Bad Request` (`application/problem+json`) com `code = "VALIDATION_ERROR"`, `type = "https://api.uniflow.dev/errors/validation-error"` e lista ordenada de `fieldErrors`.
  - Criada suíte [BeanValidationWebMvcTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/exception/BeanValidationWebMvcTest.java) validando DTOs aninhados com `@Valid`, parâmetros `@RequestParam`/`@PathVariable` e serviço anotado com `@Validated`.
- **Arquivos criados**: `BeanValidationWebMvcTest.java`
- **Arquivos modificados**: `GlobalExceptionHandler.java`, [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest,FlywayConfigTest,ModularStructureTest,GlobalExceptionHandlerWebMvcTest,ProblemDetailsFactoryTest,BeanValidationWebMvcTest"` -> `Tests run: 22, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Restam no Épico 1: `BE-010` (OpenAPI/Swagger), `BE-011` (logs estruturados) e `BE-012` (testes JUnit/Mockito/Testcontainers com Docker ativo).

### [2026-11-19] — Execução `BE-008`: Criar padrão de respostas de erro com Problem Details
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Criado o record [FieldErrorDetail.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/FieldErrorDetail.java) para padronizar erros por campo (`field`, `message`).
  - Criada a fábrica utilitária [ProblemDetailsFactory.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/ProblemDetailsFactory.java) gerando respostas `application/problem+json` (RFC 7807) com `type` (`https://api.uniflow.dev/errors/<slug>`), `title`, `status`, `detail`, `instance`, `code`, `timestamp` ISO-8601 UTC (`...Z`) e `fieldErrors` (lista imutável, vazia por padrão).
  - Atualizado [GlobalExceptionHandler.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/GlobalExceptionHandler.java) para delegar todas as respostas de erro HTTP/domínio à `ProblemDetailsFactory`.
  - Criada suíte unitária [ProblemDetailsFactoryTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/exception/ProblemDetailsFactoryTest.java) e expandido [GlobalExceptionHandlerWebMvcTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/exception/GlobalExceptionHandlerWebMvcTest.java) validando `type`, `code`, `timestamp`, `fieldErrors` vazios e serialização de `fieldErrors` preenchidos.
- **Arquivos criados**: `FieldErrorDetail.java`, `ProblemDetailsFactory.java`, `ProblemDetailsFactoryTest.java`
- **Arquivos modificados**: `GlobalExceptionHandler.java`, `GlobalExceptionHandlerWebMvcTest.java`, [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest,FlywayConfigTest,ModularStructureTest,GlobalExceptionHandlerWebMvcTest,ProblemDetailsFactoryTest"` -> `Tests run: 18, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Handlers de validação Bean Validation (`MethodArgumentNotValidException` e `ConstraintViolationException`) aguardam `BE-009` (agora desbloqueada).

### [2026-11-19] — Execução `BE-007`: Configurar tratamento global de exceções
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Criada hierarquia de exceções em `com.uniflow.common.exception`: [UniflowException.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/UniflowException.java), [ResourceNotFoundException.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/ResourceNotFoundException.java) (`404`), [ConflictException.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/ConflictException.java) (`409`), [BusinessRuleException.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/BusinessRuleException.java) (`422 UNPROCESSABLE_CONTENT`), [UnauthorizedOperationException.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/UnauthorizedOperationException.java) (`403`) e [AuthenticationFailedException.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/AuthenticationFailedException.java) (`401`).
  - Criado [GlobalExceptionHandler.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/GlobalExceptionHandler.java) (`@RestControllerAdvice`) tratando exceções de domínio, `AuthenticationException`, `AccessDeniedException`, JSON malformado, parâmetros ausentes/inválidos, rota inexistente (`404`), método HTTP não suportado (`405`) e fallback `500` sem vazar detalhes internos/stacktrace.
  - Criada suíte [GlobalExceptionHandlerWebMvcTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/exception/GlobalExceptionHandlerWebMvcTest.java) com `@WebMvcTest` e `MockMvc`.
- **Arquivos criados**: `UniflowException.java`, `ResourceNotFoundException.java`, `ConflictException.java`, `BusinessRuleException.java`, `UnauthorizedOperationException.java`, `AuthenticationFailedException.java`, `GlobalExceptionHandler.java`, `GlobalExceptionHandlerWebMvcTest.java`
- **Arquivos modificados**: [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest,FlywayConfigTest,ModularStructureTest,GlobalExceptionHandlerWebMvcTest"` -> `Tests run: 15, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Extensões RFC 7807 completas (`type`, `code`, `timestamp`, `fieldErrors`) entram em `BE-008` e handlers de Bean Validation em `BE-009`.

### [2026-11-19] — Execução `BE-005`: Criar estrutura de pacotes por domínio
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Movido o entrypoint de `com.uniflow.api.ApiApplication` para [UniflowApplication.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/UniflowApplication.java) em `com.uniflow`, garantindo `@SpringBootApplication` escaneando todos os módulos.
  - Estruturados os 15 pacotes modulares (`auth`, `usuario`, `semestre`, `disciplina`, `aula`, `atividade`, `calendario`, `trabalho`, `grupo`, `arquivo`, `notificacao`, `planejamento`, `assinatura`, `common`, `config`) com `package-info.java` e subpacotes por responsabilidade (`controller`, `service`, `repository`, `entity`, `dto`, `mapper`, `enums`, com `calendario` sem entidades/repositórios próprios conforme `MOD-07` e `common` com `exception`, `dto`, `filter`).
  - Migrados os testes existentes para `com.uniflow` e criado [ModularStructureTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/ModularStructureTest.java).
- **Arquivos criados**: [UniflowApplication.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/UniflowApplication.java), 15 `package-info.java` dos módulos, [UniflowApplicationTests.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/UniflowApplicationTests.java), [ModularStructureTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/ModularStructureTest.java), e versões em `com.uniflow` de `DependenciesSmokeTest`, `DatabaseConfigPropertiesTest` e `FlywayConfigTest`.
- **Arquivos modificados / removidos**: [AGENTS.md](file:///c:/uniflow_api/api/AGENTS.md), [ARCHITECTURE.md](file:///c:/uniflow_api/api/docs/ARCHITECTURE.md), [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md); removidos pacotes legados `com.uniflow.api`.
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest,FlywayConfigTest,ModularStructureTest"` -> `Tests run: 9, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`, 16 arquivos compilados em `main` e 5 em `test`).
- **Pendências identificadas**: Próximos passos desbloqueados no Épico 1: `BE-007` (exceções globais), `BE-010` (OpenAPI/Swagger), `BE-011` (logs) e `BE-012` (testes JUnit/Mockito + `@SpringBootTest`).

### [2026-11-19] — Execução `BE-004`: Configurar Flyway
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Configurado `spring.flyway` em [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml) (`enabled: true`, `locations: classpath:db/migration`, `encoding: UTF-8`, `baseline-on-migrate: false`, `validate-on-migrate: true`, `validate-migration-naming: true`, `fail-on-missing-locations: true`, `out-of-order: false`, `clean-disabled: true`).
  - Criada a migration baseline [V1__init_baseline.sql](file:///c:/uniflow_api/api/src/main/resources/db/migration/V1__init_baseline.sql) (`CREATE EXTENSION IF NOT EXISTS pgcrypto;`).
  - Criado [FlywayConfigTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/FlywayConfigTest.java) validando propriedades Flyway, presença em classpath, padrão `V<numero>__<snake_case>.sql` e ausência de versões duplicadas.
- **Arquivos criados**: [V1__init_baseline.sql](file:///c:/uniflow_api/api/src/main/resources/db/migration/V1__init_baseline.sql), [FlywayConfigTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/FlywayConfigTest.java)
- **Arquivos modificados**: [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml), [DATABASE_DESIGN.md](file:///c:/uniflow_api/api/docs/DATABASE_DESIGN.md), [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest,FlywayConfigTest"` -> `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Daemon Docker estava desligado durante a execução (`docker info` falhou ao conectar no pipe); validar aplicação real da `V1` em PostgreSQL quando o container ou Testcontainers estiver ativo (`BE-012`).

### [2026-11-19] — Execução `BE-006`: Configurar `application.yml` para ambientes
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Criados [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml) (configuração comum com timezone `UTC`, `open-in-view: false`, `ddl-auto: validate`, JWT e CORS via env vars), [application-dev.yml](file:///c:/uniflow_api/api/src/main/resources/application-dev.yml) (Swagger habilitado e logs `DEBUG`), [application-prod.yml](file:///c:/uniflow_api/api/src/main/resources/application-prod.yml) (env vars obrigatórias sem fallbacks inseguros, Swagger desabilitado por padrão e sem stacktraces) e [application-test.yml](file:///c:/uniflow_api/api/src/test/resources/application-test.yml) (perfil isolado em `src/test/resources/`).
  - Removido o `application.properties` legado para evitar duplicidade.
  - Atualizados [.env.example](file:///c:/uniflow_api/api/.env.example) (`SERVER_PORT`, `API_DOCS_ENABLED`, `SWAGGER_ENABLED`) e [DatabaseConfigPropertiesTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/DatabaseConfigPropertiesTest.java) usando `YamlPropertiesFactoryBean` para validar os 4 arquivos YAML.
- **Arquivos criados**: [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml), [application-dev.yml](file:///c:/uniflow_api/api/src/main/resources/application-dev.yml), [application-prod.yml](file:///c:/uniflow_api/api/src/main/resources/application-prod.yml), [application-test.yml](file:///c:/uniflow_api/api/src/test/resources/application-test.yml)
- **Arquivos modificados / removidos**: [.env.example](file:///c:/uniflow_api/api/.env.example), [DatabaseConfigPropertiesTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/DatabaseConfigPropertiesTest.java), [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md); removido `src/main/resources/application.properties`.
- **Verificação**:
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest"` -> `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Configurar Flyway em `BE-004` e vincular `@ActiveProfiles("test")` / Testcontainers em `BE-012`.

### [2026-11-19] — Execução `BE-003`: Configurar PostgreSQL e variáveis de ambiente
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - Criado [compose.yaml](file:///c:/uniflow_api/api/compose.yaml) com PostgreSQL `postgres:16-alpine`, `healthcheck` (`pg_isready`), volume persistente `uniflow_postgres_data`, timezone `UTC` e credenciais via variáveis de ambiente (`POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` obrigatória).
  - Criado [.env.example](file:///c:/uniflow_api/api/.env.example) documentando variáveis de banco, perfil Spring, JWT e CORS sem segredos reais, e atualizado [.gitignore](file:///c:/uniflow_api/api/.gitignore) para ignorar `.env` e `.env.*`.
  - Parametrizado [application.properties](file:///c:/uniflow_api/api/src/main/resources/application.properties) com `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}`, `spring.jpa.open-in-view=false`, `spring.jpa.hibernate.ddl-auto=validate` e `hibernate.jdbc.time_zone=UTC`.
  - Criado [DatabaseConfigPropertiesTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/DatabaseConfigPropertiesTest.java) validando ausência de credenciais hardcoded, `ddl-auto=validate`, `compose.yaml`, `.env.example` e `.gitignore`.
- **Arquivos criados**: [compose.yaml](file:///c:/uniflow_api/api/compose.yaml), [.env.example](file:///c:/uniflow_api/api/.env.example), [DatabaseConfigPropertiesTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/DatabaseConfigPropertiesTest.java)
- **Arquivos modificados**: [.gitignore](file:///c:/uniflow_api/api/.gitignore), [application.properties](file:///c:/uniflow_api/api/src/main/resources/application.properties), [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `docker compose config --quiet` (com `POSTGRES_PASSWORD` temporária) -> `exit=0`
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest,DatabaseConfigPropertiesTest"` -> `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: Criar `.env` local a partir de `.env.example` ao subir o container Docker; migrar configuração para `application.yml` multiambiente em `BE-006`; configurar Flyway em `BE-004`.

### [2026-11-19] — Execução `BE-002`: Configurar dependências do projeto (e conclusão de `BE-001`)
- **Épico / Versão**: `Épico 1` | `MVP 1.0`
- **O que foi implementado**:
  - `BE-001` marcada como `CONCLUIDO` mantendo o projeto inicial Spring Boot **`4.1.2`** + Maven + Java 21 por decisão explícita do usuário.
  - Em `BE-002`, configuradas em [pom.xml](file:///c:/uniflow_api/api/pom.xml) as dependências faltantes da stack obrigatória: `spring-boot-starter-validation`, `io.jsonwebtoken:jjwt-{api,impl,jackson}` (`0.13.0`), `spring-boot-starter-validation-test`, `spring-boot-testcontainers`, `org.testcontainers:testcontainers-junit-jupiter` e `org.testcontainers:testcontainers-postgresql`, além de `<name>` e `<description>` do projeto.
  - Criado [DependenciesSmokeTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/DependenciesSmokeTest.java) validando Bean Validation (Hibernate Validator), `BCryptPasswordEncoder`, assinatura/leitura JWT com `jjwt-jackson`, JUnit e Mockito em runtime com Spring Boot `4.1.2`.
- **Arquivos criados**: [DependenciesSmokeTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/DependenciesSmokeTest.java)
- **Arquivos modificados**: [pom.xml](file:///c:/uniflow_api/api/pom.xml), [AGENTS.md](file:///c:/uniflow_api/api/AGENTS.md), [README.md](file:///c:/uniflow_api/api/README.md), [ARCHITECTURE.md](file:///c:/uniflow_api/api/docs/ARCHITECTURE.md), [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md), [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md), [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md)
- **Verificação**:
  - `.\mvnw.cmd test-compile` -> `BUILD SUCCESS` (`48.051s`)
  - `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest"` -> `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`)
- **Pendências identificadas**: `ApiApplicationTests` (`@SpringBootTest`) permanece dependendo de datasource PostgreSQL e `application.yml` (`BE-003`, `BE-004`, `BE-006`); reorganização do pacote `com.uniflow.api` -> `com.uniflow.*` segue na `BE-005`.

### [2026-11-16] — Checkpoint `#001`: Adoção do Backlog Oficial (`BE-001`..`BE-158`) e Processo de Aprovação em 2 Etapas
- **Tipo**: Governança, Especificação & Alinhamento Arquitetural
- **Resumo do que foi atualizado**:
  - Adoção integral do backlog oficial com **158 tarefas (`BE-001` a `BE-158`)** distribuídas nos **12 Épicos** em [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md).
  - Atualização da arquitetura, regras de negócio, modelo de banco e contrato da API para incluir:
    - Padrão **Problem Details (RFC 7807)** no tratamento global de exceções (`BE-008`);
    - `StatusUsuario` (`BE-014`) e **refresh token com rotação e revogação** (`BE-024`..`BE-026`);
    - Regra explícita de **`ATRASADA` calculado dinamicamente** (`Épico 6`);
    - Entidade `EtapaTrabalho` com ordenação e cálculo percentual (`Épico 7`);
    - Escopo v1.1 (`Épicos 9 e 10`: `Grupo`, `GrupoParticipante`, `ConviteGrupo`, `Arquivo`, `Notificacao`) e v2.0/Futura (`Épicos 11 e 12`).
  - Atualização de [AGENTS.md](file:///c:/uniflow_api/api/AGENTS.md) e [/executar](file:///c:/uniflow_api/api/.agents/skills/executar/SKILL.md) para reforçar as **16 regras obrigatórias** e o fluxo em 2 etapas (plano técnico + autorização obrigatória antes de codar).

### [2026-11-16] — Checkpoint `#000`: Baseline Inicial e Auditoria do Workspace
- **Tipo**: Setup de Governança & Planejamento Backend
- **Resumo do que foi verificado e criado**:
  - Auditoria inicial de `c:\uniflow_api\api`: Java `21.0.9 LTS`, `pom.xml` com Spring Boot `4.1.2` e pacote `com.uniflow.api`, `application.properties` mínimo e `db/migration` vazio.
  - Verificação de compilação via `.\mvnw.cmd test-compile` -> **`BUILD SUCCESS`** (`18.829s`).
  - Criação da estrutura completa em `docs/` (`PRODUCT_SPEC.md`, `REQUIREMENTS.md`, `BUSINESS_RULES.md`, `DATABASE_DESIGN.md`, `API_CONTRACT.md`, `SECURITY.md`, `ARCHITECTURE.md`, `BACKLOG.md`, `ROADMAP.md`, `STATUS.md`, `RELATORIO.md`, `SKILLS.md`) e skills em `.agents/skills/`.
- **Próxima ação recomendada**: Apresentar o plano técnico da infraestrutura inicial (`BE-001` / `BE-002`) e aguardar autorização do usuário.

---

## 4. Template de Registro para Futuras Execuções (`/executar`)

```markdown
### [YYYY-MM-DD] — Execução `BE-XXX`: <Título da Tarefa>
- **Épico / Versão**: `Épico X` | `MVP 1.0`
- **O que foi implementado**: <Resumo técnico>
- **Arquivos criados**: <Links>
- **Arquivos modificados**: <Links>
- **Regras de negócio atendidas**: <Regras e validações>
- **Verificação**: `.\mvnw.cmd test` -> `<resultado real>`
- **Pendências identificadas**: <Observações ou nenhuma>
```
