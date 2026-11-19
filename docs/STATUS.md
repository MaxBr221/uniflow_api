# Status do Projeto — UniFlow Backend (`uniflow-backend`)

> **Última atualização**: `2026-11-19`
> **Repositório**: `c:\uniflow_api\api` (exclusivo Backend)
> **Estado Geral**: `EM_ANDAMENTO (Épico 1)` — `BE-001` a `BE-011` concluídas (`11 / 12` do Épico 1) e `BE-012` em andamento (`EM_ANDAMENTO`), aguardando inicialização do Docker Desktop para concluir `UniflowApplicationTests` (Testcontainers PostgreSQL) e liberar o Épico 2+.

---

## 1. Resumo Executivo de Progresso

| Recorte | Concluído | Em Andamento | Pendente | Fora do MVP | Total | % Concluído |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **MVP 1.0 (Épicos 1–8: `BE-001`..`BE-119`)** | 11 | 1 | 107 | 0 | 119 | **9.2%** |
| **Versão 1.1 (Épicos 9–10: `BE-120`..`BE-144`)** | 0 | 0 | 25 | 0 | 25 | **0.0%** |
| **Versão 2.0 / Futura (Épicos 11–12: `BE-145`..`BE-158`)** | 0 | 0 | 0 | 14 | 14 | **0.0%** |
| **TOTAL GERAL** | **11** | **1** | **132** | **14** | **158** | **7.0%** |

```text
MVP 1.0 (Épicos 1-8)    : [██░░░░░░░░░░░░░░░░░░]  9.2% (11 / 119)
v1.1    (Épicos 9-10)   : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 25)
v2.0    (Épicos 11-12)  : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 14) - Aguardando validação do MVP
```

---

## 2. Progresso por Épico e Módulo

| Épico | Título | Módulo(s) | Versão Alvo | Faixa | Concluídas | Status |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | Configuração e estrutura do projeto | `config`, `common` | `MVP 1.0` | `BE-001`..`BE-012` | 11 / 12 | Em andamento |
| **2** | Autenticação e usuários | `MOD-01`, `MOD-02` | `MVP 1.0` | `BE-013`..`BE-031` | 0 / 19 | Não iniciado |
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

Fatos verificados diretamente em `c:\uniflow_api\api` (`2026-11-19`):
- **Java**: `21.0.9 LTS` ativo no ambiente.
- **Testes completos executados**: `.\mvnw.cmd test` -> **`Tests run: 35, Failures: 0, Errors: 1, Skipped: 0`** (`BUILD FAILURE` apenas em `UniflowApplicationTests` por ausência do daemon Docker: `Could not find a valid Docker environment`; todas as 12 suítes unitárias/Mockito/WebMvcTest totalizaram **`34 / 34`** passando).
- **Base JUnit 5 + Mockito + Testcontainers**: [TestcontainersConfiguration.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/TestcontainersConfiguration.java) (`postgres:16-alpine` com `@ServiceConnection`), [AbstractIntegrationTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/AbstractIntegrationTest.java), [UniflowApplicationTests.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/UniflowApplicationTests.java) e [RequestLoggingFilterMockitoTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/common/filter/RequestLoggingFilterMockitoTest.java).
- **Logs Estruturados e Segurança de Logs**: [RequestLoggingFilter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/RequestLoggingFilter.java) (`requestId` MDC + header `X-Request-Id`), [SensitiveDataMasker.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/SensitiveDataMasker.java), [SensitiveDataMaskingConverter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/filter/SensitiveDataMaskingConverter.java) e [logback-spring.xml](file:///c:/uniflow_api/api/src/main/resources/logback-spring.xml) (UTC + mascaramento automático de JWT/senhas + formato estruturado em `prod`).
- **OpenAPI / Swagger**: [OpenApiConfig.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/OpenApiConfig.java) com metadados `/api/v1`, servidor base `/` e esquema `bearerAuth` (JWT), controlado por perfil em [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml) (`dev` habilitado, `prod`/`test` protegidos).
- **Exceções Globais, Problem Details e Bean Validation**: [GlobalExceptionHandler.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/GlobalExceptionHandler.java), [ProblemDetailsFactory.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/ProblemDetailsFactory.java) e [FieldErrorDetail.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/FieldErrorDetail.java) + hierarquia `UniflowException` ativos em `com.uniflow.common.exception`, mapeando exceções HTTP/domínio e Bean Validation (`MethodArgumentNotValidException`, `ConstraintViolationException`, `HandlerMethodValidationException`) para RFC 7807 (`type`, `title`, `status`, `detail`, `instance`, `code`, `timestamp` UTC, `fieldErrors`).
- **Estrutura Modular**: [UniflowApplication.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/UniflowApplication.java) em `com.uniflow` + 15 pacotes modulares e subcamadas.
- **Flyway & Config**: [application.yml](file:///c:/uniflow_api/api/src/main/resources/application.yml) (`dev`, `test`, `prod`), [V1__init_baseline.sql](file:///c:/uniflow_api/api/src/main/resources/db/migration/V1__init_baseline.sql), [compose.yaml](file:///c:/uniflow_api/api/compose.yaml), [.env.example](file:///c:/uniflow_api/api/.env.example).

---

## 4. Alertas Técnicos e Decisões Pendentes

1. **Docker Desktop parado no host (`BE-012` em andamento)**:
   - `docker info` falhou (`npipe:////./pipe/dockerDesktopLinuxEngine` indisponível).
   - Iniciar Docker Desktop e reexecutar `.\mvnw.cmd test` para concluir `UniflowApplicationTests` (`TestcontainersConfiguration` + `V1__init_baseline.sql`) e finalizar o Épico 1.

---

## 5. Próximos Passos Recomendados (Épico 1)

1. Iniciar o **Docker Desktop** no host e reexecutar `/run BE-012` (para validar `.\mvnw.cmd test` completo com PostgreSQL Testcontainers e marcar `BE-012` como `CONCLUIDO`).
