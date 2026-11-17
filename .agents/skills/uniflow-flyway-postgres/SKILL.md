---
name: uniflow-flyway-postgres
description: Convenções para criação de migrations Flyway, modelagem PostgreSQL, constraints, índices e atualização do docs/DATABASE_DESIGN.md no backend UniFlow.
---

# Convenções Flyway e PostgreSQL (`uniflow-backend`)

Aplique esta skill sempre que uma tarefa `BE-XXX` envolver criação ou alteração de tabelas, colunas, constraints ou índices.

## 1. Versionamento de Migrations (`RNF-007`)
- Diretório obrigatório: `src/main/resources/db/migration/`.
- Convenção de nome: `V<numero>__<descricao_em_snake_case>.sql` (ex.: `V1__init_baseline.sql`, `V2__create_usuarios_table.sql`).
- **Nunca edite** uma migration já aplicada em histórico compartilhado; crie uma nova `V<proximo>__*.sql`.
- Nunca utilize `spring.jpa.hibernate.ddl-auto=create` ou `update` em ambientes compartilhados (`validate` em dev/prod).

## 2. Boas Práticas de Modelagem PostgreSQL
- **Chaves Primárias**: `UUID` conforme `docs/DATABASE_DESIGN.md`.
- **Integridade Referencial**: FKs nomeadas (`fk_<tabela>_<referencia>`), `CHECK` constraints para invariantes (`data_inicial < data_final`, `hora_inicial < hora_final`, `duracao_planejada_minutos > 0`) e índice único parcial para único semestre atual por usuário.
- **Tipos Temporais**: `TIMESTAMPTZ` para instantes/prazos em UTC; `DATE` e `TIME` para datas/horários de aula recorrente.
- **Enums**: `VARCHAR` com `CHECK` constraint.
- **Arquivos (`MOD-10`)**: Nunca armazenar `BYTEA` / `OID` no PostgreSQL; guardar apenas metadados e `storage_key`.

## 3. Sincronização Obrigatória
- Ao criar/alterar qualquer migration, atualize `docs/DATABASE_DESIGN.md`.
