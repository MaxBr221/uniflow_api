---
name: run
description: Executa o fluxo de uma tarefa do backlog do UniFlow Backend pelo ID (ex. /run be-001, /run back-022 ou /run BE-044), com análise prévia, plano técnico, aprovação obrigatória, testes e atualização dos documentos.
---

# Comando `/run <ID>`

Use esta skill sempre que o usuário invocar `/run <ID>` (ex.: `/run be-001`, `/run back-022`, `/run BE-044`) para analisar e executar uma tarefa do backlog backend.

---

## Etapa 1: Identificação, Análise Prévia e Plano Técnico (OBRIGATÓRIA antes de codar)

1. **Normalizar e Localizar o ID**:
   - Aceite formatos `BE-XXX`, `be-XXX`, `BACK-XXX`, `back-XXX` ou apenas número `XXX`, normalizando para o ID oficial `BE-XXX` com 3 dígitos (ex.: `back-022` -> `BE-022`, `be-1` -> `BE-001`, `44` -> `BE-044`).
   - Quando o usuário informar `back-XXX`, deixe explícito logo no início: *"Referência `back-XXX` mapeada para a tarefa oficial `BE-XXX` do `docs/BACKLOG.md`."*
   - Se nenhum ID for informado, leia `docs/STATUS.md` e `docs/BACKLOG.md`, liste as tarefas desbloqueadas disponíveis e pergunte qual ID deseja executar.
   - Se o ID não existir na faixa `BE-001`..`BE-158`, informe ao usuário e sugira usar `/tarefas` para consultar todos os IDs válidos.
2. **Consultar Documentação e Backlog**:
   - Leia os detalhes da tarefa `BE-XXX` em `docs/BACKLOG.md` (Épico, versão, prioridade, dependências, status e critério de aceite).
   - Consulte os documentos pertinentes em `docs/`: `PRODUCT_SPEC.md`, `REQUIREMENTS.md`, `BUSINESS_RULES.md`, `DATABASE_DESIGN.md`, `API_CONTRACT.md`, `SECURITY.md` e `ARCHITECTURE.md`.
3. **Verificar Código Atual, Dependências e Travas de Escopo**:
   - Inspecione o estado atual do código (`pom.xml`, `src/main/java/`, `src/main/resources/db/migration/`, `src/test/java/`).
   - Verifique se todas as dependências listadas em `Depende de` estão com status `CONCLUIDO`. Se alguma estiver pendente, sinalize claramente como pré-requisito/bloqueio.
   - Verifique as travas obrigatórias:
     - **Estrutura Inicial (Épico 1)**: Não implementar módulos funcionais (`Épico 2+`) antes de concluir o `Épico 1` (`BE-001`..`BE-012`), salvo autorização expressa do usuário.
     - **MVP Gate**: Tarefas de `Épico 11` (`Versão 2.0`) e `Épico 12` (`Versão Futura`) são bloqueadas até validação do MVP 1.0, salvo autorização expressa.
4. **Apresentar Plano Técnico e Aguardar Aprovação**:
   - Apresente:
     - Identificação da tarefa (`BE-XXX`), Épico, status atual e status real das dependências;
     - **Arquivos que serão criados** e **arquivos que serão modificados**;
     - Migration Flyway, endpoints REST, DTOs, regras de negócio, segurança/ownership e testes previstos;
     - **Possíveis impactos em funcionalidades existentes**.
   - **Pare e aguarde a autorização explícita do usuário** antes de implementar (só avance direto se o usuário já tiver aprovado explicitamente o plano na mensagem anterior).

---

## Etapa 2: Implementação Controlada (Somente após Aprovação)

1. Atualize o status da tarefa em `docs/BACKLOG.md` e `docs/STATUS.md` para `EM_ANDAMENTO`.
2. Implemente **somente o escopo autorizado**, preservando código existente e contratos já definidos.
3. Siga as 16 regras de `AGENTS.md` e as skills técnicas:
   - [uniflow-backend-patterns](../uniflow-backend-patterns/SKILL.md) (DTOs, mappers, sem regra em controller, `@Transactional`, Problem Details RFC 7807, OpenAPI/Swagger, sem abstrações desnecessárias);
   - [uniflow-flyway-postgres](../uniflow-flyway-postgres/SKILL.md) (toda mudança no banco via Flyway, nunca `ddl-auto=create/update`);
   - [uniflow-security-ownership](../uniflow-security-ownership/SKILL.md) (usuário autenticado via `SecurityContext`, isolamento rigoroso entre usuários).
4. Crie ou atualize os testes unitários e de integração correspondentes.

---

## Etapa 3: Validação, Segurança e Atualização Documental

1. Execute os testes e o build Maven (`.\mvnw.cmd test`).
2. Verifique regressões e revise o checklist de `docs/SECURITY.md`.
3. **Regra crítica**: **Não marque a tarefa como `CONCLUIDO` se os critérios de aceite não forem atendidos ou se build/testes falharem.**
4. Após validação bem-sucedida, atualize:
   - `docs/BACKLOG.md` (`CONCLUIDO`);
   - `docs/STATUS.md` (métricas recalculadas por versão e épico);
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
