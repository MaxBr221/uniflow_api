# Catálogo de Skills e Comandos Slash — UniFlow Backend

Este repositório utiliza **Skills do Antigravity** localizadas em `.agents/skills/<nome>/SKILL.md` e regras persistentes em [AGENTS.md](file:///c:/uniflow_api/api/AGENTS.md).

---

## 1. Comandos Slash Disponíveis no Chat

| Comando | Skill | O que faz | Exemplo de uso |
| :--- | :--- | :--- | :--- |
| `/executar <ID>` | [executar/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/executar/SKILL.md) | Analisa a tarefa `BE-XXX`, verifica dependências/MVP Gate, apresenta o plano técnico e aguarda aprovação. Após aprovado, implementa, valida testes/build/segurança e atualiza `BACKLOG.md`, `STATUS.md`, `RELATORIO.md`, `DATABASE_DESIGN.md` e `API_CONTRACT.md`. | `/executar be-001` ou `/executar be-044` |
| `/status-projeto` | [status-projeto/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/status-projeto/SKILL.md) | Audita o código real, migrations e `docs/BACKLOG.md`, recalculando métricas e atualizando `docs/STATUS.md`. | `/status-projeto` |
| `/relatorio-projeto` | [relatorio-projeto/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/relatorio-projeto/SKILL.md) | Verifica compilação/testes atuais e gera ou atualiza o relatório executivo em `docs/RELATORIO.md`. | `/relatorio-projeto` |

---

## 2. Skills Técnicas Aplicadas Automaticamente

| Skill | Arquivo | Finalidade |
| :--- | :--- | :--- |
| `uniflow-backend-patterns` | [SKILL.md](file:///c:/uniflow_api/api/.agents/skills/uniflow-backend-patterns/SKILL.md) | Padrões de monólito modular (`controller/service/repository/entity/dto/mapper/enums`), paginação, exceções, UTC e política sem abstrações desnecessárias. |
| `uniflow-flyway-postgres` | [SKILL.md](file:///c:/uniflow_api/api/.agents/skills/uniflow-flyway-postgres/SKILL.md) | Padrões de migrations Flyway (`V{n}__*.sql`), constraints PostgreSQL, índices e sincronização de `docs/DATABASE_DESIGN.md`. |
| `uniflow-security-ownership` | [SKILL.md](file:///c:/uniflow_api/api/.agents/skills/uniflow-security-ownership/SKILL.md) | Regras de Spring Security stateless, JWT, BCrypt, derivação de usuário via `SecurityContext` e isolamento estrita de dados entre usuários (`docs/SECURITY.md`). |

---

## 3. Fluxo Recomendado de Trabalho

1. Consultar o estado atual com `/status-projeto` ou lendo [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md).
2. Executar a estrutura inicial primeiro (Épico 1: `BE-001`..`BE-012`) antes de iniciar módulos funcionais.
3. Ao rodar `/executar BE-XXX`, revisar o **Plano Técnico** apresentado e responder autorizando a execução.
4. Revisar o resumo final e os registros atualizados em [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md).
