# Banco de Dados e Migrations Flyway — UniFlow Backend

> **SGBD**: PostgreSQL | **Versionamento**: Flyway (`src/main/resources/db/migration/`)
> **Estado Atual**: `1` migration criada (`V1__init_baseline.sql`, pronta para aplicação assim que o PostgreSQL/Docker estiver ativo).

---

## 1. Convenções de Modelagem

- **Chaves Primárias**: `UUID` gerado pela aplicação ou `gen_random_uuid()`.
- **Timestamps**: Sempre `TIMESTAMPTZ` (armazenando UTC, conforme `RNF-016`).
- **Horários recorrentes de aula**: `VARCHAR` para dia da semana + `TIME` para `hora_inicial` e `hora_final`.
- **Enums**: `VARCHAR(40)` com `CHECK` constraint (facilita evoluções sem `ALTER TYPE`).
- **Hibernate**: `spring.jpa.hibernate.ddl-auto=validate` em desenvolvimento/produção (somente Flyway altera o schema).
- **Regra `ATRASADA` (`Épico 6`)**: O estado `ATRASADA` de atividades é calculado dinamicamente com base em `prazo` e conclusão, não dependendo de atualização batch persistida no banco.

---

## 2. Diagrama Entidade-Relacionamento Alvo (MVP 1.0 + v1.1)

```mermaid
erDiagram
    USUARIOS ||--o{ REFRESH_TOKENS : possui
    USUARIOS ||--o{ SEMESTRES : possui
    USUARIOS ||--o{ ATIVIDADES : cria
    USUARIOS ||--o{ TRABALHOS : cria
    USUARIOS ||--o{ NOTIFICACOES : recebe
    SEMESTRES ||--o{ DISCIPLINAS : contem
    DISCIPLINAS ||--o{ AULAS : tem
    DISCIPLINAS ||--o{ ATIVIDADES : agrupa
    DISCIPLINAS ||--o{ TRABALHOS : referencia
    TRABALHOS ||--o{ ETAPAS_TRABALHO : divide_em
    GRUPOS ||--o{ GRUPO_PARTICIPANTES : possui
    GRUPOS ||--o{ CONVITES_GRUPO : envia
    GRUPOS ||--o{ TRABALHOS : compartilha
    DISCIPLINAS ||--o{ ARQUIVOS : anexa
    TRABALHOS ||--o{ ARQUIVOS : anexa
```

---

## 3. Plano de Migrations Flyway

| Versão | Arquivo Planejado | Tabelas / Objetos | Tarefa | Status |
| :--- | :--- | :--- | :--- | :--- |
| `V1` | `V1__init_baseline.sql` | Extensões base / baseline inicial (`pgcrypto`) | `BE-004` | `CRIADA (aguarda DB ativo)` |
| `V2` | `V2__create_usuarios_table.sql` | `usuarios` (`status`), `refresh_tokens` (rotação/revogação) | `BE-015` | `PENDENTE` |
| `V3` | `V3__create_semestres_table.sql` | `semestres` + índice único parcial `WHERE atual = true` | `BE-034` | `PENDENTE` |
| `V4` | `V4__create_disciplinas_table.sql` | `disciplinas` | `BE-047` | `PENDENTE` |
| `V5` | `V5__create_aulas_table.sql` | `aulas` + `CHECK (hora_inicial < hora_final)` | `BE-060` | `PENDENTE` |
| `V6` | `V6__create_atividades_table.sql` | `atividades` + índices `(usuario_id, status, prazo)` | `BE-074` | `PENDENTE` |
| `V7` | `V7__create_trabalhos_table.sql` | `trabalhos`, `etapas_trabalho` (`ordem`, `prazo`) | `BE-092` | `PENDENTE` |
| `V8` | `V8__create_grupos_tables.sql` | `grupos`, `grupo_participantes`, `convites_grupo` *(v1.1)* | `BE-123` | `PENDENTE` |
| `V9` | `V9__create_arquivos_table.sql` | `arquivos` *(somente metadados + `storage_key`, v1.1)* | `BE-133` | `PENDENTE` |
| `V10` | `V10__create_notificacoes_table.sql` | `notificacoes` *(v1.1)* | `BE-139` | `PENDENTE` |
| `V11` | `V11__create_assinaturas_tables.sql` | `planos`, `assinaturas`, `pagamentos` *(v2.0 — bloqueado)* | `BE-152` | `FORA_DO_MVP` |

---

## 4. Constraints e Índices Críticos

1. **`usuarios`**: `UNIQUE (email)` com e-mail normalizado em lowercase; `status` em `StatusUsuario`.
2. **`refresh_tokens`**: `token_hash` único, referência a `usuario_id`, `expires_at`, `revoked`, `replaced_by_token_id` (suporte à rotação segura `BE-024`).
3. **`semestres`**:
   - `CONSTRAINT ck_semestres_datas CHECK (data_inicial < data_final)`
   - `CREATE UNIQUE INDEX uq_semestre_atual_usuario ON semestres (usuario_id) WHERE atual = true`
4. **`aulas`**: `CONSTRAINT ck_aulas_horario CHECK (hora_inicial < hora_final)`.
5. **`atividades`**:
   - `CONSTRAINT ck_atividades_tempo_positivo CHECK (tempo_estimado_minutos IS NULL OR tempo_estimado_minutos > 0)`
   - Índice composto `(usuario_id, status, prazo)` para filtro eficiente de pendentes, atrasadas (via `prazo < now()`) e concluídas.
6. **`etapas_trabalho`**: índice `(trabalho_id, ordem)` para ordenação determinística (`BE-105`).
7. **`arquivos`**: sem colunas `BYTEA` / `OID` (regra estrita do `MOD-10`).
