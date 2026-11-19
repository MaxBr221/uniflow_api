# Catálogo de Skills e Comandos Slash — UniFlow Backend

Este repositório utiliza **Skills do Antigravity** localizadas em `.agents/skills/<nome>/SKILL.md` e regras persistentes em [AGENTS.md](file:///c:/uniflow_api/api/AGENTS.md).

---

## 1. Comandos Slash Disponíveis no Chat

| Comando | Skill | O que faz | Exemplo de uso |
| :--- | :--- | :--- | :--- |
| `/tarefas [filtro]` | [tarefas/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/tarefas/SKILL.md) | Lista todas as tarefas do backlog oficial (`BE-001` a `BE-158`) por épico e status, ou filtra por `pendentes`, `desbloqueadas`, `epico X`, `mvp`, etc. | `/tarefas` ou `/tarefas desbloqueadas` |
| `/run <ID>` | [run/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/run/SKILL.md) | Executa **diretamente** a tarefa (aceita `BE-XXX` ou `back-XXX`) sem pedir confirmação quando estiver desbloqueada e na fase permitida; implementa, testa e atualiza os `.md`. | `/run back-022` ou `/run BE-004` |
| `/executar <ID>` | [executar/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/executar/SKILL.md) | Fluxo com revisão prévia: analisa a tarefa, apresenta o plano técnico para sua aprovação e implementa após você autorizar. | `/executar be-004` ou `/executar be-044` |
| `/status-projeto` | [status-projeto/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/status-projeto/SKILL.md) | Audita o código real, migrations e `docs/BACKLOG.md`, recalculando métricas e atualizando `docs/STATUS.md`. | `/status-projeto` |
| `/relatorio-projeto` | [relatorio-projeto/SKILL.md](file:///c:/uniflow_api/api/.agents/skills/relatorio-projeto/SKILL.md) | Verifica compilação/testes atuais e gera ou atualiza o relatório executivo em `docs/RELATORIO.md`. | `/relatorio-projeto` |

---

## 2. Skills Técnicas Aplicadas Automaticamente

| Skill | Arquivo | Finalidade |
| :--- | :--- | :--- |
| `uniflow-backend-patterns` | [SKILL.md](file:///c:/uniflow_api/api/.agents/skills/uniflow-backend-patterns/SKILL.md) | Padrões de monólito modular (`controller/service/repository/entity/dto/mapper/enums`), paginação, Problem Details RFC 7807, UTC e política sem abstrações desnecessárias. |
| `uniflow-flyway-postgres` | [SKILL.md](file:///c:/uniflow_api/api/.agents/skills/uniflow-flyway-postgres/SKILL.md) | Padrões de migrations Flyway (`V{n}__*.sql`), constraints PostgreSQL, índices e sincronização de `docs/DATABASE_DESIGN.md`. |
| `uniflow-security-ownership` | [SKILL.md](file:///c:/uniflow_api/api/.agents/skills/uniflow-security-ownership/SKILL.md) | Regras de Spring Security stateless, JWT, BCrypt, derivação de usuário via `SecurityContext` e isolamento estrito de dados entre usuários (`docs/SECURITY.md`). |

---

## 3. Fluxo Recomendado de Trabalho

1. Ver todas as tarefas com `/tarefas` ou consultar o resumo com `/status-projeto`.
2. Executar a estrutura inicial primeiro (Épico 1: `BE-001`..`BE-012`) antes de iniciar módulos funcionais.
3. Para **execução direta** (sem parada para confirmar), use `/run <ID>`. Se quiser **revisar o plano antes de codar**, use `/executar <ID>`.
4. Revisar o resumo final e os registros atualizados em [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md).
