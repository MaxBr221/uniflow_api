# Contrato da API REST — UniFlow Backend <-> Frontend

> **Base URL**: `/api/v1` | **Formato**: JSON UTF-8 | **Documentação OpenAPI**: `/v3/api-docs` e `/swagger-ui.html`
> **Autenticação**: `Authorization: Bearer <access_token>` (JWT stateless) + Refresh Token com rotação.

---

## 1. Convenções Transversais do Contrato

### 1.1 Envelope de Paginação (`PageResponse<T>`)
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

### 1.2 Payload Padronizado de Erro — Problem Details RFC 7807 (`BE-008`)
```json
{
  "type": "https://api.uniflow.dev/errors/validation-error",
  "title": "Dados inválidos",
  "status": 400,
  "detail": "Um ou mais campos estão inválidos.",
  "instance": "/api/v1/atividades",
  "code": "VALIDATION_ERROR",
  "timestamp": "2026-11-16T22:00:00Z",
  "fieldErrors": [
    { "field": "titulo", "message": "não deve estar em branco" }
  ]
}
```

### 1.3 Datas e Fusos Horários (`RNF-016`)
- Instantes e prazos utilizam ISO-8601 com offset/UTC (ex.: `2026-11-20T23:59:00Z`).
- Consultas de calendário aceitam parâmetro `timezone` IANA (ex.: `America/Sao_Paulo`).

---

## 2. Mapa de Endpoints Planejados e Status de Implementação

### Épico 2 — Autenticação e Usuários (`MVP 1.0`)
| Método | Rota | Acesso | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Público (`permitAll`) | Cadastro de estudante (`201 Created`, `AuthUserResponse`, conflito `409`) | `BE-019`, `BE-021` | `IMPLEMENTADO` |
| `POST` | `/api/v1/auth/login` | Público (`permitAll`) | Login emitindo access token JWT (`refreshToken` com rotação em `BE-024`) | `BE-023`, `BE-024` | `PARCIAL (accessToken implementado)` |
| `POST` | `/api/v1/auth/refresh` | Público (token) | Renovação de sessão com rotação do refresh token | `BE-024`, `BE-025` | `PLANEJADO` |
| `POST` | `/api/v1/auth/logout` | Autenticado | Logout e revogação da sessão | `BE-026` | `PLANEJADO` |
| `GET` | `/api/v1/auth/me` | Autenticado | Dados do usuário autenticado | `BE-027` | `PLANEJADO` |
| `PATCH` | `/api/v1/auth/password` | Autenticado | Alteração de senha | `BE-029` | `PLANEJADO` |
| `GET` / `PATCH` | `/api/v1/usuarios/me` | Autenticado | Consultar e atualizar perfil | `BE-027`, `BE-028` | `PLANEJADO` |

### Épico 3 — Semestres Acadêmicos (`MVP 1.0`)
| Método | Rota | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- |
| `POST` / `GET` | `/api/v1/semestres` | Cadastrar e listar semestres do usuário | `BE-037`, `BE-038` | `PLANEJADO` |
| `GET` / `PUT` / `DELETE` | `/api/v1/semestres/{id}` | Consultar, editar e excluir semestre | `BE-039`, `BE-040`, `BE-041` | `PLANEJADO` |
| `PATCH` | `/api/v1/semestres/{id}/atual` | Definir semestre atual (único por usuário) | `BE-042`, `BE-043` | `PLANEJADO` |

### Épico 4 — Disciplinas (`MVP 1.0`)
| Método | Rota | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/semestres/{semestreId}/disciplinas` | Cadastrar disciplina no semestre | `BE-050`, `BE-057` | `PLANEJADO` |
| `GET` | `/api/v1/disciplinas` | Listar disciplinas (com filtro `?semestreId=`) | `BE-051`, `BE-055` | `PLANEJADO` |
| `GET` / `PUT` / `DELETE` | `/api/v1/disciplinas/{id}` | Consultar, editar e excluir disciplina | `BE-052`, `BE-053`, `BE-054` | `PLANEJADO` |

### Épico 5 — Aulas e Horários (`MVP 1.0`)
| Método | Rota | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/disciplinas/{disciplinaId}/aulas` | Cadastrar horário (retorna alertas de conflito) | `BE-063`, `BE-068` | `PLANEJADO` |
| `GET` | `/api/v1/aulas` | Listar aulas (filtro opcional `?diaSemana=`) | `BE-064`, `BE-069` | `PLANEJADO` |
| `GET` | `/api/v1/aulas/grade-semanal` | Grade semanal recorrente consolidada | `BE-067` | `PLANEJADO` |
| `PUT` / `DELETE` | `/api/v1/aulas/{id}` | Editar e excluir horário | `BE-065`, `BE-066` | `PLANEJADO` |

### Épico 6 — Atividades Acadêmicas (`MVP 1.0`)
| Método | Rota | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/atividades` | Cadastrar atividade | `BE-077` | `PLANEJADO` |
| `GET` | `/api/v1/atividades` | Listar paginado (filtros disciplina, status, período, ordenação) | `BE-078`, `BE-084`..`BE-088` | `PLANEJADO` |
| `GET` / `PUT` / `DELETE` | `/api/v1/atividades/{id}` | Consultar, editar e excluir atividade | `BE-079`, `BE-080`, `BE-081` | `PLANEJADO` |
| `PATCH` | `/api/v1/atividades/{id}/concluir` | Concluir atividade | `BE-082` | `PLANEJADO` |
| `PATCH` | `/api/v1/atividades/{id}/status` | Alterar status da atividade | `BE-083` | `PLANEJADO` |

### Épico 7 — Trabalhos e Etapas (`MVP 1.0`)
| Método | Rota | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- |
| `POST` / `GET` | `/api/v1/trabalhos` | Cadastrar e listar trabalhos | `BE-095`, `BE-096` | `PLANEJADO` |
| `GET` / `PUT` / `DELETE` | `/api/v1/trabalhos/{id}` | Consultar (com progresso), editar e excluir trabalho | `BE-097`..`BE-099`, `BE-103` | `PLANEJADO` |
| `POST` | `/api/v1/trabalhos/{id}/etapas` | Cadastrar etapa | `BE-100` | `PLANEJADO` |
| `PUT` | `/api/v1/trabalhos/{id}/etapas/{etapaId}` | Editar etapa | `BE-101` | `PLANEJADO` |
| `PATCH` | `/api/v1/trabalhos/{id}/etapas/{etapaId}/concluir` | Concluir etapa e recalcular % | `BE-102`, `BE-104` | `PLANEJADO` |
| `PATCH` | `/api/v1/trabalhos/{id}/etapas/reordenar` | Reordenar etapas | `BE-105` | `PLANEJADO` |

### Épico 8 — Dashboard e Calendário (`MVP 1.0`)
| Método | Rota | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/dashboard` | Resumo (pendentes, concluídas, atrasadas, trabalhos, próximas) | `BE-108`..`BE-113` | `PLANEJADO` |
| `GET` | `/api/v1/calendario` | Eventos do período (aulas, atividades, trabalhos) + filtros | `BE-114`..`BE-118` | `PLANEJADO` |

### Épicos 9 e 10 — Colaboração, Arquivos e Notificações (`Versão 1.1`)
| Método | Rota | Descrição | Tarefa(s) | Status |
| :--- | :--- | :--- | :--- | :--- |
| `POST` / `GET` | `/api/v1/grupos`, `/api/v1/grupos/{id}` | Criar e consultar grupos | `BE-124` | `PLANEJADO` |
| `POST` | `/api/v1/grupos/{id}/convites` | Convidar participante | `BE-125` | `PLANEJADO` |
| `POST` | `/api/v1/grupos/convites/{id}/{aceitar\|recusar}` | Aceitar ou rejeitar convite | `BE-126` | `PLANEJADO` |
| `GET` / `DELETE` | `/api/v1/grupos/{id}/participantes...` | Listar/remover membros | `BE-127`, `BE-128` | `PLANEJADO` |
| `POST` / `GET` / `DELETE` | `/api/v1/arquivos...` | Upload, download autorizado e exclusão | `BE-135`..`BE-138` | `PLANEJADO` |
| `GET` / `PATCH` | `/api/v1/notificacoes...` | Listar notificações e marcar lida(s) | `BE-141`, `BE-142` | `PLANEJADO` |

> Endpoints dos **Épicos 11 e 12** (`BE-145`..`BE-158`) ficam fora do contrato ativo até validação do MVP 1.0.
