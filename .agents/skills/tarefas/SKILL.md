---
name: tarefas
description: Lista todas as tarefas do backlog oficial do UniFlow Backend (BE-001 a BE-158) organizadas por épico e status, com suporte opcional a filtros (ex. /tarefas, /tarefas pendentes, /tarefas desbloqueadas, /tarefas epico 2).
---

# Comando `/tarefas`

Use esta skill sempre que o usuário invocar `/tarefas` (com ou sem filtros) ou pedir para listar todas as tarefas do backlog no chat.

## Modo de Operação (Somente Leitura)
Este comando **não altera código nem status do backlog**. Ele lê `docs/BACKLOG.md` e `docs/STATUS.md` e exibe a lista completa e atualizada diretamente na resposta.

## Passos
1. Leia `docs/BACKLOG.md` completo e `docs/STATUS.md`.
2. Verifique se o usuário passou algum filtro opcional após `/tarefas`:
   - Sem filtro (`/tarefas`): liste **todas as tarefas do backlog (`BE-001` a `BE-158`)**, agrupadas pelos **12 Épicos**.
   - Filtro por status (ex.: `/tarefas pendentes`, `/tarefas concluidas`, `/tarefas em andamento`, `/tarefas fora do mvp`): filtre apenas os itens naquele status.
   - Filtro `/tarefas desbloqueadas`: liste apenas tarefas `PENDENTE` cujas dependências em `Depende de` já estejam `CONCLUIDO` (ou sem dependências).
   - Filtro por épico ou versão (ex.: `/tarefas epico 1`, `/tarefas mvp`, `/tarefas v1.1`, `/tarefas v2.0`): exiba apenas os itens do recorte pedido.
3. Formate a saída de forma limpa e legível:
   - Cabeçalho com resumo numérico atual (`Concluídas`, `Em Andamento`, `Pendentes`, `Fora do MVP`, `Total: 158`).
   - Tabelas por Épico contendo: `ID`, `Status`, `Prioridade`, `Depende de` e `Tarefa`.
   - Destaque ao final as **próximas tarefas desbloqueadas** prontas para iniciar agora.
4. Informe no rodapé que o usuário pode iniciar qualquer tarefa com `/run <ID>` (ex.: `/run be-001`, `/run back-022`) ou `/executar <ID>`.
