# Status do Projeto — UniFlow Backend (`uniflow-backend`)

> **Última atualização**: `2026-11-25`
> **Repositório**: `c:\uniflow_api\api` (exclusivo Backend)
> **Estado Geral**: `ÉPICO 2 EM ANDAMENTO` — `29` concluídas (`12 / 12` Épico 1 + `BE-013`..`BE-029` do Épico 2); suíte completa validada (`.\mvnw.cmd test` `253/253` com PostgreSQL `16.15` Testcontainers e Hibernate `validate`). Próxima desbloqueada no Épico 2: `BE-030`.

---

## 1. Resumo Executivo de Progresso

| Recorte | Concluído | Em Andamento | Pendente | Fora do MVP | Total | % Concluído |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **MVP 1.0 (Épicos 1–8: `BE-001`..`BE-119`)** | 29 | 0 | 90 | 0 | 119 | **24.4%** |
| **Versão 1.1 (Épicos 9–10: `BE-120`..`BE-144`)** | 0 | 0 | 25 | 0 | 25 | **0.0%** |
| **Versão 2.0 / Futura (Épicos 11–12: `BE-145`..`BE-158`)** | 0 | 0 | 0 | 14 | 14 | **0.0%** |
| **TOTAL GERAL** | **29** | **0** | **115** | **14** | **158** | **18.4%** |

```text
MVP 1.0 (Épicos 1-8)    : [█████░░░░░░░░░░░░░░░] 24.4% (29 / 119)
v1.1    (Épicos 9-10)   : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 25)
v2.0    (Épicos 11-12)  : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 14) - Aguardando validação do MVP
```

---

## 2. Progresso por Épico e Módulo

| Épico | Título | Módulo(s) | Versão Alvo | Faixa | Concluídas | Status |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | Configuração e estrutura do projeto | `config`, `common` | `MVP 1.0` | `BE-001`..`BE-012` | 12 / 12 | Concluído |
| **2** | Autenticação e usuários | `MOD-01`, `MOD-02` | `MVP 1.0` | `BE-013`..`BE-031` | 17 / 19 | Em andamento |
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

Fatos verificados diretamente em `c:\uniflow_api\api` (`2026-11-25`):
- **Java**: `21.0.9 LTS` ativo no ambiente.
- **Docker**: Docker Desktop ativo no host (`29.3.1`).
- **Suíte completa executada**: `.\mvnw.cmd test` -> **`Tests run: 253, Failures: 0, Errors: 0, Skipped: 0`** (`BUILD SUCCESS`), validando `UniflowApplicationTests`, `UsuarioRepositoryIntegrationTest` (`5/5` em PostgreSQL `16.15` Testcontainers com `V1` + `V2` e Hibernate `ddl-auto=validate`), `UsuarioControllerWebMvcTest` (`4/4`), `UpdateProfileRequestValidationTest` (`13/13`), `UsuarioMapperTest` (`2/2`), `UsuarioServiceTest` (`6/6`), `UsuarioProfileIntegrationTest` (`2/2`), `CurrentUserServiceTest` (`4/4`), `AuthMeIntegrationTest` (`2/2`), `AuthPasswordWebMvcTest` (`3/3`), `ChangePasswordRequestValidationTest` (`17/17`), `AuthPasswordIntegrationTest` (`2/2`), `RefreshTokenTest` (`2/2`), `RefreshTokenServiceTest` (`16/16`), `RefreshTokenIntegrationTest` (`4/4`), `LogoutRequestValidationTest` (`5/5`), `RefreshTokenRequestValidationTest` (`5/5`), `AuthLogoutIntegrationTest` (`2/2`), `AuthRefreshIntegrationTest` (`2/2`), `JwtAuthenticationFilterTest` (`9/9`), `JwtAuthenticationFilterWebMvcTest` (`2/2`), `JwtAuthenticationIntegrationTest` (`1/1`), `AuthRegistrationIntegrationTest` (`1/1`), `AuthLoginIntegrationTest` (`2/2`), `SecurityIntegrationTest` (`2/2`), `SecurityConfigWebMvcTest` (`5/5`), `SecuritySwaggerEnabledWebMvcTest` (`1/1`), `AuthControllerWebMvcTest` (`16/16`), `AuthServiceTest` (`20/20`), `JwtServiceTest` (`7/7`), `AuthMapperTest` (`2/2`), `PasswordEncoderConfigTest` (`7/7`), `RegisterRequestValidationTest` (`25/25`), `RegisterDtoWebMvcTest` (`2/2`), `LoginRequestValidationTest` (`13/13`), `LoginDtoWebMvcTest` (`2/2`) e `SensitiveDataMaskerTest` (`3/3`).
- **Alteração de Senha (`BE-029` / `MOD-01`)**: [PasswordPolicy.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/PasswordPolicy.java), [ChangePasswordRequest.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/ChangePasswordRequest.java) (`senhaAtual`, `novaSenha` forte com redação em `toString()`), `AuthService.changePassword(...)` em [AuthService.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/AuthService.java) (identidade via `CurrentUserService`, validação BCrypt da `senhaAtual` com `422 BusinessRuleException`, bloqueio de senha idêntica à vigente, geração de novo hash BCrypt e revogação atômica de todos os refresh tokens ativos da conta via `RefreshTokenService.revokeAllForUser(...)`) e endpoint autenticado `PATCH /api/v1/auth/password` (`204 No Content`) em [AuthController.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java).
- **Perfil do Usuário Autenticado (`BE-028` / `MOD-02`)**: [UsuarioController.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/controller/UsuarioController.java) (`GET/PATCH /api/v1/usuarios/me` autenticados), [UsuarioService.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/service/UsuarioService.java) (identidade via `CurrentUserService`, atualização parcial de `nome`/`email`, unicidade `409` via [ConstraintViolationUtils.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/common/exception/ConstraintViolationUtils.java)), [UsuarioMapper.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/mapper/UsuarioMapper.java), [UpdateProfileRequest.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/dto/UpdateProfileRequest.java) e [UsuarioResponse.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/usuario/dto/UsuarioResponse.java).
- **Consulta do Usuário Autenticado (`BE-027` / `MOD-01`)**: [CurrentUserService.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/security/CurrentUserService.java) extraindo identidade exclusivamente do `SecurityContextHolder` (rejeitando contexto ausente/anônimo ou principal incompatível e revalidando conta ativa no banco), `AuthService.getCurrentUser()` e endpoint autenticado `GET /api/v1/auth/me` em [AuthController.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java).
- **Logout e Revogação de Sessão (`BE-026` / `MOD-01`)**: [LogoutRequest.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/LogoutRequest.java), `RefreshTokenService.revokeTokenForUser(...)` (ownership estrito com `403 FORBIDDEN_OPERATION` e idempotência), `AuthService.logout(...)` e endpoint autenticado `POST /api/v1/auth/logout` em [AuthController.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) (`204 No Content`, limpeza do `SecurityContextHolder`).
- **Renovação de Sessão (`BE-025` / `MOD-01`)**: [RefreshTokenRequest.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/dto/RefreshTokenRequest.java), `AuthService.refresh(...)` (`@Transactional(noRollbackFor = AuthenticationFailedException.class)`) e endpoint público `POST /api/v1/auth/refresh` em [AuthController.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) rotacionando refresh tokens, emitindo novo access token JWT e bloqueando reutilização (`401 AUTHENTICATION_FAILED`).
- **Refresh Token com Rotação e Segurança (`BE-024` / `MOD-01`)**: [RefreshToken.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/entity/RefreshToken.java) (`refresh_tokens`), [RefreshTokenRepository.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/repository/RefreshTokenRepository.java) (`PESSIMISTIC_WRITE` + revogação em lote) e [RefreshTokenService.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/RefreshTokenService.java) (token opaco `rft_` via `SecureRandom`, hash SHA-256, expiração `app.security.jwt.refresh-expiration-ms`, rotação atômica invalidando token anterior via `replacedByTokenId`, detecção de reuso revogando todos os tokens ativos com `noRollbackFor` e emissão integrada em `AuthService.login`).
- **Autenticação Stateless por Access Token JWT (`BE-022` / `MOD-01`)**: [AuthenticatedUser.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/security/AuthenticatedUser.java) (`UserDetails` com `UUID id`, `email`, `status`, `getPassword() == null`) + [JwtAuthenticationFilter.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/security/JwtAuthenticationFilter.java) (`OncePerRequestFilter` validando Bearer token via `JwtService`, verificando `usuario.podeAutenticar()` em `UsuarioRepository` e populando `SecurityContextHolder`, integrado em [SecurityConfig.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/SecurityConfig.java) antes de `UsernamePasswordAuthenticationFilter.class`).
- **Emissão de Tokens e Login (`BE-023` + `BE-024` / `MOD-01`)**: [JwtService.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/JwtService.java) (HS256 via `app.security.jwt.secret`, expiração `app.security.jwt.access-expiration-ms`, claims `sub`, `jti`, `iat`, `exp`, `email`, `type=access`) + `POST /api/v1/auth/login` em [AuthController.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) e [AuthService.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/AuthService.java) com validação BCrypt, checagem de `podeAutenticar()`, resposta genérica `401 AUTHENTICATION_FAILED` e emissão completa de `accessToken` + `refreshToken`.
- **Spring Security Stateless (`BE-021`)**: [SecurityConfig.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/SecurityConfig.java) com `@EnableWebSecurity`, `@EnableMethodSecurity`, `SessionCreationPolicy.STATELESS`, CORS configurável via `app.cors.allowed-origins`, `permitAll` restrito (`POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh` e OpenAPI/Swagger condicionados por perfil) e handlers `401`/`403` em `ProblemDetail` RFC 7807 via `HandlerExceptionResolver`.
- **Cadastro de Usuário (`BE-019` / `MOD-01`)**: [AuthController.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/controller/AuthController.java) (`POST /api/v1/auth/register` -> `201 Created` público, `@SecurityRequirements`, Swagger `@ApiResponses`), [AuthService.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/service/AuthService.java) (normalização, verificação case-insensitive + tratamento de corrida em `uk_usuarios_email` -> `ConflictException 409`, hash BCrypt) e [AuthMapper.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/auth/mapper/AuthMapper.java) (mapeamento seguro sem `senhaHash`).
- **Criptografia de Senhas (`BE-020` / `RNF-005`)**: [PasswordEncoderConfig.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/config/PasswordEncoderConfig.java) provendo `@Bean PasswordEncoder` (`BCryptPasswordEncoder` com custo mínimo `10` e configurável `10..31` via `app.security.password.bcrypt-strength`).
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

1. `/run BE-030` — Implementar validações de segurança e autorização (auditoria/helpers centrais de ownership para todos os módulos), desbloqueando `BE-031` na sequência.
