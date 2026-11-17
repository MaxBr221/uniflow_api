---
name: status-projeto
description: Audita o estado real do código-fonte, migrations Flyway e docs/BACKLOG.md para atualizar e exibir o painel de progresso em docs/STATUS.md. Pode ser invocado com /status-projeto.
---

# Comando `/status-projeto`

Use esta skill quando o usuário invocar `/status-projeto` ou pedir para ver o andamento atual do projeto UniFlow Backend.

## Passos
1. Leia `docs/BACKLOG.md` e conte exatamente quantos itens estão em cada status (`CONCLUIDO`, `EM_ANDAMENTO`, `PENDENTE`, `BLOQUEADO`, `FORA_DO_MVP`), agrupados por Versão e pelos Épicos 1 a 12 (`BE-001`..`BE-158`).
2. Inspecione o workspace real para validar aderência entre código e documentação:
   - Leia `pom.xml` e `src/main/resources/` (`application*.yml`, `db/migration/V*.sql`).
   - Liste pacotes/classes em `src/main/java/` e testes em `src/test/java/`.
3. Identifique divergências entre o backlog e o código real, bloqueios, e os 3 próximos `BE-XXX` recomendados (respeitando prioridade inicial do Épico 1 e ordem de dependências).
4. Atualize `docs/STATUS.md` com os números recalculados e a data atual.
5. Responda ao usuário com um resumo executivo claro apontando para `docs/STATUS.md`.
