# Status do Projeto — UniFlow Backend (`uniflow-backend`)

> **Última atualização**: `2026-11-23`
> **Repositório**: `c:\uniflow_api\api` (exclusivo Backend)
> **Estado Geral**: `ÉPICO 2 EM ANDAMENTO` — `18` concluídas (`12 / 12` Épico 1 + `BE-013`..`BE-018` do Épico 2); suíte completa validada (`.\mvnw.cmd clean test` `87/87` com PostgreSQL `16.15` Testcontainers e Hibernate `validate`). Próximas desbloqueadas: `BE-020` e `BE-021`.

---

## 1. Resumo Executivo de Progresso

| Recorte | Concluído | Em Andamento | Pendente | Fora do MVP | Total | % Concluído |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **MVP 1.0 (Épicos 1–8: `BE-001`..`BE-119`)** | 18 | 0 | 101 | 0 | 119 | **15.1%** |
| **Versão 1.1 (Épicos 9–10: `BE-120`..`BE-144`)** | 0 | 0 | 25 | 0 | 25 | **0.0%** |
| **Versão 2.0 / Futura (Épicos 11–12: `BE-145`..`BE-158`)** | 0 | 0 | 0 | 14 | 14 | **0.0%** |
| **TOTAL GERAL** | **18** | **0** | **126** | **14** | **158** | **11.4%** |

```text
MVP 1.0 (Épicos 1-8)    : [███░░░░░░░░░░░░░░░░░] 15.1% (18 / 119)
v1.1    (Épicos 9-10)   : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 25)
v2.0    (Épicos 11-12)  : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 14) - Aguardando validação do MVP
```

---

## 2. Progresso por Épico e Módulo

| Épico | Título | Módulo(s) | Versão Alvo | Faixa | Concluídas | Status |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | Configuração e estrutura do projeto | `config`, `common` | `MVP 1.0` | `BE-001`..`BE-012` | 12 / 12 | Concluído |
| **2** | Autenticação e usuários | `MOD-01`, `MOD-02` | `MVP 1.0` | `BE-013`..`BE-031` | 6 / 19 | Em andamento |
| **3** | Semestres acadêmicos | `MOD-03` | `MVP 1.0` | `BE-032`..`BE-045` | 0 / 14 | Não iniciado |
| **4** | Disciplinas | `MOD-04` | `MVP 1.0` | `BE-046`..`BE-058` | 0 / 13 | Não iniciado |
| **5** | Aulas e horários | `MOD-05` | `MVP 1.0` | `BE-059`..`BE-070` | 0 / 12 | Não iniciado |
| **6** | Atividades acadêmicas | `MOD-06` | `MVP 1.0` | `BE-071`..`BE-089` | 0 / 19 | Não iniciado |
| **7** | Trabalhos e etapas | `MOD-08` | `MVP 1.0` | `BE-090`..`BE-107` | 0 / 18 | Não iniciado |
| **8** | Dashboard e calendário | `MOD-07` | `MVP 1.0` | `BE-108`..`BE-119` | 0 / 12 | Não iniciado |
| **9** | Grupos e colaboração | `MOD-09`, `MOD-08` | `Versão 1.1` | `BE-120`..`BE-132` | 0 / 13 | Aguardando MVP |
| **10** | Arquivos e notificações | `MOD-10`, `MOD-11` | `Versão 1.1` | `BE-133`..`BE-144` | 0 / 12 | Aguardando MVP |
| **11** | Planejamento inteligente e IA | `MOD-12` | `Versão 2.0` | `BE-145`..`BE-151` | 0 / 7 | Fora do MVP |
| **12** | Monetização | `MOD-13` | `Versão Futura` | `BE-152`..`BE-158` | 0 / 7 | Fora do MVP |

---

## 3. Estado Real Observado no Workspace

Fatos verificados diretamente em `c:\uniflow_api\api` (`2026-11-23`):
- **Java**: `21.0.9 LTS` ativo no ambiente.
- **Docker**: Docker Desktop ativo no host (`29.3.1`).
- **Suíte completa executada**: `.\mvnw.cmd clean test` -> **`Tests run: 87, Failures: 0, Errors: 0, Skipped: 0`** (`BUILD SUCCESS`), validando `UniflowApplicationTests`, `UsuarioRepositoryIntegrationTest` (`5/5` em PostgreSQL `16.15` Testcontainers com `V1` + `V2` e Hibernate `ddl-auto=validate`), `RegisterRequestValidationTest` (`25/25`), `RegisterDtoWebMvcTest` (`2/2`), `LoginRequestValidationTest` (`13/13`) e `LoginDtoWebMvcTest` (`2/2`).
- **DTOs de Autenticação (`MOD-01`)**: Records [RegisterRequest.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/RegisterRequest.java), [AuthUserResponse.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/AuthUserResponse.java), [LoginRequest.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/LoginRequest.java) e [AuthTokenResponse.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/AuthTokenResponse.java) (Bean Validation, normalização de `email`, saída UTC sem `senhaHash` e redação de segredos em `toString()`).
- **Flyway V2 (`BE-015` concluída)**: [V2__create_usuarios_table.sql](file:///c:/uniflow_api/api/src/main/resources/db/migration/V2__create_usuarios_table.sql) (`usuarios`, `refresh_tokens`, constraints e índices) aplicada e validada pelo Hibernate (`validate`) e por `FlywayConfigTest` (`2/2`).
- **Domínio e Persistência de Usuário (`MOD-01`)**: Entidade [Usuario.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/entity/Usuario.java) (`usuarios`, `id UUID`, `nome`, `email` normalizado + `uk_usuarios_email`, `senha_hash`, `status`, `created_at`/`updated_at` UTC), enum [StatusUsuario.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/enums/StatusUsuario.java) (`ATIVO`, `INATIVO`, `BLOQUEADO`, `EXCLUIDO`) e repositório [UsuarioRepository.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/repository/UsuarioRepository.java) (`findByEmailIgnoreCase`, `existsByEmailIgnoreCase`).
- **Base JUnit 5 + Mockito + Testcontainers**: [TestcontainersConfiguration.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/TestcontainersConfiguration.java) (`postgres:16-alpine` com `@ServiceConnection`), [AbstractIntegrationTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/AbstractIntegrationTest.java), [UniflowApplicationTests.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/UniflowApplicationTests.java) e [RequestLoggingFilterMockitoTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/filter/RequestLoggingFilterMockitoTest.java).
- **Logs Estruturados e Segurança de Logs**: [RequestLoggingFilter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/RequestLoggingFilter.java) (`requestId` MDC + header `X-Request-Id`), [SensitiveDataMasker.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/SensitiveDataMasker.java), [SensitiveDataMaskingConverter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/SensitiveDataMaskingConverter.java) e [logback-spring.xml](file:///c:/uniflow_api/api/src/main/resources/logback-spring.xml) (UTC + mascaramento automático de JWT/senhas + formato estruturado em `prod`).
- **OpenAPI / Swagger**: [OpenApiConfig.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/OpenApiConfig.java) com metadados `/api/v1`, servidor base `/` e esquema `bearerAuth` (JWT), controlado por perfil em [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml) (`dev` habilitado, `prod`/`test` protegidos).
- **Exceções Globais, Problem Details e Bean Validation**: [GlobalExceptionHandler.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/GlobalExceptionHandler.java), [ProblemDetailsFactory.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/ProblemDetailsFactory.java) e [FieldErrorDetail.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/FieldErrorDetail.java) + hierarquia `UniflowException` ativos em `com.uniflow.common.exception`, mapeando exceções HTTP/domínio e Bean Validation (`MethodArgumentNotValidException`, `ConstraintViolationException`, `HandlerMethodValidationException`) para RFC 7807 (`type`, `title`, `status`, `detail`, `instance`, `code`, `timestamp` UTC, `fieldErrors`).
- **Estrutura Modular**: [UniflowApplication.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/UniflowApplication.java) em `com.uniflow` + 15 pacotes modulares e subcamadas.
- **Flyway & Config**: [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml) (`dev`, `test`, `prod`), [V1__init_baseline.sql](file:///c:/uniflow_api/api/src/main/resources/db/migration/V1__init_baseline.sql), [compose.yaml](file:///c:/uniflow_api/api/compose.yaml), [.env.example](file:///c:/uniflow_api/api/.env.example).

---

## 4. Alertas Técnicos e Decisões Pendentes

1. **Docker Desktop para testes de integração**:
   - Manter Docker ativo ao rodar `.\mvnw.cmd test` completo (Testcontainers `postgres:16-alpine`).
2. **Spring Boot `4.1.2`**:
   - Versão do Spring Boot Starter Parent mantida conforme decisão registrada em `BE-001`.

---

## 5. Próximos Passos Recomendados (Épico 2 Desbloqueado)

1. `/run BE-020` — Implementar criptografia de senha com BCrypt (`PasswordEncoder`, desbloqueando `BE-019` e `BE-023`).
2. `/run BE-021` — Configurar Spring Security (`SecurityFilterChain` stateless + handlers `401`/`403` RFC 7807).
