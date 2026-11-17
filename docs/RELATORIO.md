# Relatório de Andamento — UniFlow Backend (`uniflow-backend`)

Este documento registra a evolução cronológica, entregas verificadas, métricas de qualidade e riscos técnicos do backend do **UniFlow**.

---

## 1. Resumo Executivo Atual (`2026-11-16`)

- **Fase Atual**: Épico 1 (Preparação da estrutura inicial, aguardando autorização para `BE-001`..`BE-012`).
- **Progresso do MVP 1.0 (Épicos 1–8)**: `0 / 119` tarefas concluídas (`0.0%`).
- **Progresso Versão 1.1 (Épicos 9–10)**: `0 / 25` tarefas concluídas (`0.0%`).
- **Progresso Versão 2.0 / Futura (Épicos 11–12)**: `0 / 14` tarefas (`FORA_DO_MVP`).
- **Progresso Geral**: `0 / 158` tarefas concluídas (`0.0%`).
- **Status do Build**: `.\mvnw.cmd test-compile` passando (`BUILD SUCCESS` com Java `21.0.9 LTS`).
- **Endpoints implementados**: `0` | **Migrations Flyway**: `0` | **Testes de domínio**: `0`.

### Principais Pontos de Atenção
1. Alinhar o `pom.xml` em `BE-001`/`BE-002` de `4.1.2` (skeleton Initializr) para a stack obrigatória **Spring Boot 3.5.x** + Java 21, adicionando `spring-boot-starter-validation` e JWT.
2. Configurar PostgreSQL (`compose.yaml` + env vars), Flyway (`BE-004`) e `application.yml` (`BE-006`) antes de iniciar os módulos funcionais (`Épico 2` em diante).
3. Seguir rigorosamente as regras chave do backlog oficial:
   - Respostas de erro padronizadas com **Problem Details (RFC 7807)** (`BE-008`);
   - **Refresh token com rotação** e revogação no logout (`BE-024`..`BE-026`);
   - **`ATRASADA` como estado calculado** com base em prazo e conclusão (`Épico 6`);
   - **Proibido** iniciar `Épico 11` (`v2.0`) ou `Épico 12` (`Futura`) antes da validação do MVP 1.0.

---

## 2. Indicadores de Cobertura e Qualidade

| Métrica | Valor Atual | Meta MVP 1.0 |
| :--- | :---: | :---: |
| Tarefas `BE-XXX` concluídas | 0 | 119 (`Épicos 1–8`) |
| Tarefas Versão 1.1 concluídas | 0 | 25 (`Épicos 9–10`, pós-MVP) |
| Migrations Flyway em `src/main/resources/db/migration/` | 0 | 7 (`V1`..`V7` no MVP 1.0) |
| Endpoints REST implementados | 0 | Todos os de `MVP 1.0` em [API_CONTRACT.md](file:///c:/uniflow_api/api/docs/API_CONTRACT.md) |
| Compilação (`.\mvnw.cmd test-compile`) | ✅ Passando | ✅ Passando |
| Suíte de testes (`.\mvnw.cmd test`) | ⏳ Aguardando Épico 1 (`BE-003`/`004`/`006`/`012`) | ✅ 100% Passando |

---

## 3. Histórico Cronológico de Execuções

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
