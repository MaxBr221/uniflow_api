# Relatório de Andamento — UniFlow Backend (`uniflow-backend`)

Este documento registra a evolução cronológica, entregas verificadas, métricas de qualidade e riscos técnicos do backend do **UniFlow**.

---

## 1. Resumo Executivo Atual (`2026-11-19`)

- **Fase Atual**: Épico 1 em andamento (`BE-001` e `BE-002` concluídas; próximas desbloqueadas: `BE-003` e `BE-005`).
- **Progresso do MVP 1.0 (Épicos 1–8)**: `2 / 119` tarefas concluídas (`1.7%`).
- **Progresso Versão 1.1 (Épicos 9–10)**: `0 / 25` tarefas concluídas (`0.0%`).
- **Progresso Versão 2.0 / Futura (Épicos 11–12)**: `0 / 14` tarefas (`FORA_DO_MVP`).
- **Progresso Geral**: `2 / 158` tarefas concluídas (`1.3%`).
- **Status do Build**: `.\mvnw.cmd test-compile` passando (`BUILD SUCCESS` com Java `21.0.9 LTS` e Spring Boot `4.1.2`); `DependenciesSmokeTest` passando (`2 / 2`).
- **Endpoints implementados**: `0` | **Migrations Flyway**: `0` | **Testes passando**: `2` (`DependenciesSmokeTest`).

### Principais Pontos de Atenção
1. Mantido **Spring Boot `4.1.2`** (do esqueleto Initializr) por decisão do usuário em `2026-11-19` (`BE-001` concluída).
2. Configurar PostgreSQL (`compose.yaml` + env vars em `BE-003`), Flyway (`BE-004`) e `application.yml` (`BE-006`) antes de executar `@SpringBootTest` completo e iniciar os módulos funcionais.
3. Seguir rigorosamente as regras chave do backlog oficial:
   - Respostas de erro padronizadas com **Problem Details (RFC 7807)** (`BE-008`);
   - **Refresh token com rotação** e revogação no logout (`BE-024`..`BE-026`);
   - **`ATRASADA` como estado calculado** com base em prazo e conclusão (`Épico 6`);
   - **Proibido** iniciar `Épico 11` (`v2.0`) ou `Épico 12` (`Futura`) antes da validação do MVP 1.0.

---

## 2. Indicadores de Cobertura e Qualidade

| Métrica | Valor Atual | Meta MVP 1.0 |
| :--- | :---: | :---: |
| Tarefas `BE-XXX` concluídas | 2 | 119 (`Épicos 1–8`) |
| Tarefas Versão 1.1 concluídas | 0 | 25 (`Épicos 9–10`, pós-MVP) |
| Migrations Flyway em `src/main/resources/db/migration/` | 0 | 7 (`V1`..`V7` no MVP 1.0) |
| Endpoints REST implementados | 0 | Todos os de `MVP 1.0` em [API_CONTRACT.md](file:///c:/uniflow_api/api/docs/API_CONTRACT.md) |
| Compilação (`.\mvnw.cmd test-compile`) | ✅ Passando | ✅ Passando |
| Suíte de testes (`.\mvnw.cmd test`) | ⚠️ Parcial (`DependenciesSmokeTest` 2/2; `ApiApplicationTests` aguarda `BE-003`/`006`) | ✅ 100% Passando |

---

## 3. Histórico Cronológico de Execuções

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
