---
name: executar
description: Executa o fluxo oficial de uma tarefa do backlog do UniFlow Backend pelo seu ID (ex. /executar be-001 ou /executar be-044), com análise prévia, plano técnico e aprovação obrigatória antes da implementação.
---

# Comando `/executar <ID>`

Use esta skill sempre que o usuário invocar `/executar <BE-XXX>` (ex.: `/executar be-001`, `/executar BE-044`) ou solicitar a implementação de uma tarefa do backlog.

---

## Etapa 1: Análise Prévia e Plano Técnico (OBRIGATÓRIA antes de codar)

1. **Identificar a Tarefa**:
   - Normalize o argumento para o padrão oficial `BE-XXX` (maiúsculas e 3 dígitos; aceitando `be-044`, `BE-044`, `back-044`, `BACK-044` ou `44` -> `BE-044`; quando digitado como `back-XXX`, informe explicitamente o mapeamento para `BE-XXX` do `docs/BACKLOG.md`).
   - Se nenhum ID for informado, leia `docs/STATUS.md` e `docs/BACKLOG.md`, liste as próximas tarefas desbloqueadas e peça ao usuário informar o ID desejado.
2. **Consultar Documentação e Backlog**:
   - Leia o bloco da tarefa em `docs/BACKLOG.md` (Épico, versão, prioridade, dependências, status e critério de aceite).
   - Consulte os documentos pertinentes em `docs/`: `PRODUCT_SPEC.md`, `REQUIREMENTS.md`, `BUSINESS_RULES.md`, `DATABASE_DESIGN.md`, `API_CONTRACT.md`, `SECURITY.md` e `ARCHITECTURE.md`.
3. **Analisar Estado Atual e Dependências**:
   - Inspecione o código atual (`pom.xml`, `src/main/java/`, `src/main/resources/db/migration/`, `src/test/java/`).
   - Verifique se todas as tarefas listadas em `Depende de` estão com status `CONCLUIDO`. Se alguma dependência estiver pendente, sinalize claramente como bloqueio/pré-requisito.
   - Verifique as travas de escopo:
     - **Infraestrutura Inicial**: Não implemente módulos funcionais antes de concluir a estrutura inicial (Épico 1: `BE-001`..`BE-012`), salvo autorização expressa.
     - **MVP Gate**: Se a tarefa for do Épico 11 (`Versão 2.0`) ou Épico 12 (`Versão Futura`), bloqueie por padrão e alerte sobre a regra do Documento Mestre.
4. **Apresentar Plano Técnico e Aguardar Aprovação**:
   - Apresente ao usuário:
     - Resumo da tarefa (`BE-XXX`) e status das dependências;
     - **Arquivos que serão criados** e **arquivos que serão modificados**;
     - Migration Flyway, endpoints REST, DTOs, regras de negócio, segurança/ownership e testes planejados;
     - **Possíveis impactos em funcionalidades existentes**.
   - **Pare aqui e aguarde a autorização explícita do usuário** antes de modificar qualquer código (pule a espera apenas se o usuário já tiver aprovado explicitamente o plano na mensagem anterior ou pedido execução direta sem confirmação prévia).

---

## Etapa 2: Implementação Controlada (Somente após Aprovação)

1. Atualize o status da tarefa em `docs/BACKLOG.md` e `docs/STATUS.md` para `EM_ANDAMENTO`.
2. Implemente **somente o escopo autorizado**, preservando código existente e sem alterar contratos já implementados sem justificar.
3. Siga obrigatoriamente as 16 regras de `AGENTS.md` e as skills técnicas:
   - [uniflow-backend-patterns](../uniflow-backend-patterns/SKILL.md) (sem regra de negócio em controller, sem retornar entidade JPA, DTOs entrada/saída, transações, paginação, Problem Details RFC 7807, OpenAPI/Swagger, sem abstrações desnecessárias);
   - [uniflow-flyway-postgres](../uniflow-flyway-postgres/SKILL.md) (toda mudança de banco via Flyway `V{n}__*.sql`, nunca `ddl-auto=create/update`);
   - [uniflow-security-ownership](../uniflow-security-ownership/SKILL.md) (usuário do `SecurityContext`, isolamento entre usuários, nunca confiar em `usuarioId` do frontend).
4. Crie ou atualize os testes unitários e de integração correspondentes.

---

## Etapa 3: Validação, Segurança e Atualização Documental

1. Execute os testes e o build Maven (`.\mvnw.cmd test` e, quando aplicável, `.\mvnw.cmd verify` / `package`).
2. Verifique regressões e revise o checklist de `docs/SECURITY.md`.
3. **Regra crítica**: **Não marque a tarefa como `CONCLUIDO` se os critérios de aceite não forem atendidos ou se build/testes falharem.**
4. Após validação bem-sucedida, atualize:
   - `docs/BACKLOG.md` (`CONCLUIDO`);
   - `docs/STATUS.md` (métricas recalculadas por versão e épico, alertas e próximos passos);
   - `docs/RELATORIO.md` (nova entrada cronológica com arquivos, build e testes);
   - `docs/API_CONTRACT.md`, `docs/DATABASE_DESIGN.md` e `docs/SECURITY.md` (conforme alterações realizadas).

---

## Etapa 4: Formato Obrigatório da Resposta Final

Ao finalizar a execução, apresente exatamente os 6 tópicos abaixo:
1. **O que foi implementado**
2. **Arquivos criados**
3. **Arquivos modificados**
4. **Regras de negócio atendidas**
5. **Testes executados e resultados**
6. **Pendências identificadas**
