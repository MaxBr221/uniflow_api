---
name: run
description: Executa diretamente uma tarefa do backlog do UniFlow Backend pelo ID (ex. /run be-004, /run back-022 ou /run BE-044) sem pedir confirmação intermediária quando estiver desbloqueada, rodando implementação, testes e atualização dos documentos.
---

# Comando `/run <ID>` (Execução Direta)

Use esta skill sempre que o usuário invocar `/run <ID>` (ex.: `/run be-004`, `/run back-022`, `/run BE-044`).

> **Regra de Autorização Direta**: Quando a tarefa estiver **desbloqueada** e dentro do escopo permitido, o próprio comando `/run <ID>` já vale como autorização explícita do usuário. **Não pare para perguntar "se autoriza"** — analise, implemente, teste e atualize os documentos diretamente.

---

## Etapa 1: Identificação, Análise e Verificação de Travas

1. **Normalizar e Localizar o ID**:
   - Aceite formatos `BE-XXX`, `be-XXX`, `BACK-XXX`, `back-XXX` ou apenas número `XXX`, normalizando para `BE-XXX` com 3 dígitos (ex.: `back-022` -> `BE-022`, `be-4` -> `BE-004`, `44` -> `BE-044`).
   - Quando o usuário informar `back-XXX`, deixe explícito na resposta: *"Referência `back-XXX` mapeada para a tarefa oficial `BE-XXX` do `docs/BACKLOG.md`."*
   - Se nenhum ID for informado, leia `docs/STATUS.md` e `docs/BACKLOG.md`, liste as tarefas desbloqueadas disponíveis e pergunte qual ID deseja executar.
   - Se o ID não existir na faixa `BE-001`..`BE-158`, informe ao usuário e sugira usar `/tarefas`.
2. **Consultar Documentação e Estado Atual**:
   - Leia o bloco da tarefa `BE-XXX` em `docs/BACKLOG.md` (Épico, versão, prioridade, dependências, status e critério de aceite).
   - Consulte os trechos pertinentes em `docs/` (`PRODUCT_SPEC.md`, `REQUIREMENTS.md`, `BUSINESS_RULES.md`, `DATABASE_DESIGN.md`, `API_CONTRACT.md`, `SECURITY.md`, `ARCHITECTURE.md`).
   - Inspecione os arquivos reais envolvidos no projeto (`pom.xml`, `src/main/java/`, `src/main/resources/`, `src/test/java/`).
3. **Validar Travas Obrigatórias (únicos casos em que deve parar e avisar)**:
   - Se a tarefa já estiver `CONCLUIDO`, informe ao usuário e confirme apenas se deseja reexecutar/refatorar.
   - Se alguma dependência listada em `Depende de` ainda **não** estiver `CONCLUIDO`, **pare** e informe exatamente quais pré-requisitos faltam.
   - **Estrutura Inicial (`Épico 1`)**: Se a tarefa for de `Épico 2+` e o `Épico 1` (`BE-001`..`BE-012`) ainda não estiver concluído, **pare** e alerte que a estrutura inicial precisa ser finalizada primeiro (ou peça confirmação expressa para pular).
   - **MVP Gate**: Se a tarefa for do `Épico 11` (`Versão 2.0`) ou `Épico 12` (`Versão Futura`) antes da validação do MVP 1.0, **pare** e alerte sobre a regra do Documento Mestre.

---

## Etapa 2: Implementação Direta (Sem Perguntar Confirmação)

1. Atualize o status da tarefa em `docs/BACKLOG.md` para `EM_ANDAMENTO`.
2. Planeje internamente e implemente **somente o escopo da `BE-XXX`**, preservando código existente e contratos já definidos.
3. Siga obrigatoriamente as 16 regras de `AGENTS.md` e as skills técnicas:
   - [uniflow-backend-patterns](../uniflow-backend-patterns/SKILL.md) (DTOs, mappers, sem regra em controller, `@Transactional`, Problem Details RFC 7807, OpenAPI/Swagger, sem abstrações desnecessárias);
   - [uniflow-flyway-postgres](../uniflow-flyway-postgres/SKILL.md) (toda mudança no banco via Flyway, nunca `ddl-auto=create/update`);
   - [uniflow-security-ownership](../uniflow-security-ownership/SKILL.md) (usuário autenticado via `SecurityContext`, isolamento rigoroso entre usuários).
4. Crie ou atualize os testes unitários e de integração correspondentes.

---

## Etapa 3: Validação, Segurança e Atualização Documental

1. Execute os testes e o build Maven (`.\mvnw.cmd test` no escopo aplicável, reportando exatamente o comando e resultado observados).
2. Verifique regressões e revise o checklist de `docs/SECURITY.md`.
3. **Regra crítica**: **Não marque a tarefa como `CONCLUIDO` se os critérios de aceite não forem atendidos ou se build/testes falharem.**
4. Após validação bem-sucedida, atualize:
   - `docs/BACKLOG.md` (`CONCLUIDO`);
   - `docs/STATUS.md` (métricas recalculadas por versão e épico, estado atual e próximas desbloqueadas);
   - `docs/RELATORIO.md` (nova entrada cronológica);
   - `docs/API_CONTRACT.md`, `docs/DATABASE_DESIGN.md` e `docs/SECURITY.md` conforme alterações realizadas.

---

## Etapa 4: Formato Obrigatório da Resposta Final

Ao finalizar a execução, apresente exatamente os 6 tópicos abaixo:
1. **O que foi implementado**
2. **Arquivos criados**
3. **Arquivos modificados**
4. **Regras de negócio atendidas**
5. **Testes executados e resultados**
6. **Pendências identificadas**
