---
name: uniflow-backend-patterns
description: Padrões arquiteturais e convenções de código do monólito modular UniFlow Backend (Java 21, Spring Boot, pacotes por módulo e responsabilidade, DTOs, mappers, paginação, tratamento de erros e fusos horários).
---

# Padrões do Monólito Modular UniFlow (`uniflow-backend`)

Aplique esta skill ao criar ou modificar qualquer módulo em `src/main/java/com/uniflow/`.

## 1. Estrutura de Pacotes
Cada módulo de domínio fica em `com.uniflow.<modulo>` e é organizado por responsabilidade:
- `controller/` — Endpoints REST (`/api/v1/...`), validação `@Valid`, códigos HTTP adequados (`200`, `201`, `204`), anotações OpenAPI. Sem regra de negócio no controller.
- `service/` — Regras de negócio, controle transacional (`@Transactional`), validação de ownership e orquestração. **Não crie interface + Impl sem necessidade real.**
- `repository/` — Interfaces Spring Data JPA (`JpaRepository<Entidade, UUID>` ou tipo de ID definido no projeto).
- `entity/` — Entidades JPA com restrições coerentes com o Flyway e auditoria temporal (`Instant`/`OffsetDateTime`).
- `dto/` — Java `record`s imutáveis separados para request (`*Request`) e response (`*Response`), evitando expor entidades JPA diretamente na API.
- `mapper/` — Conversões explícitas entre Entity e DTO (métodos simples/componentes Spring ou MapStruct se adotado de forma consistente — preferir mappers explícitos sem mágica oculta).
- `enums/` — Enums de domínio (`StatusAtividade`, `PrioridadeAtividade`, `TipoAtividade`, `StatusSemestre`, `StatusTrabalho`, `PapelGrupo`, `StatusConvite`, `TipoNotificacao`, etc.).

## 2. Regras de Acoplamento entre Módulos
- Evite acoplamento circular entre módulos.
- Prefira injetar serviços públicos/consultas do módulo dono do dado ou queries dedicadas em casos de consolidação (`calendario`, `dashboard`), sem duplicar dados em tabelas paralelas (`MOD-07`).

## 3. Padrões Transversais Obrigatórios (RNF)
- **RNF-001 / RNF-009 (HTTP & Exceções)**: Utilize respostas padronizadas no formato **Problem Details (RFC 7807 / `ProblemDetail`)** definido em `common/exception/` via `@RestControllerAdvice` (`BE-007`, `BE-008`).
- **RNF-004 (Validação)**: Use Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, `@Size`, `@Email`, etc.) nos DTOs de entrada + invariantes de domínio no `service`.
- **Estado `ATRASADA` (Épico 6)**: Deve ser calculado dinamicamente com base no prazo e na conclusão (`prazo != null && prazo < now && status != CONCLUIDA`), não necessariamente persistido no banco.
- **RNF-012 (Paginação)**: Endpoints de listagem que podem crescer (atividades, semestres, notificações, sessões de estudo, trabalhos) devem aceitar `Pageable` (`page`, `size`, `sort`) e retornar envelope paginado padronizado (`PageResponse<T>`).
- **RNF-016 (Fuso Horário)**: Armazenar instantes em UTC (`Instant` / `TIMESTAMPTZ`) e tratar datas locais (`LocalDate`, `LocalTime`) explicitamente quando o conceito for de calendário/horário recorrente de aula (`MOD-05`).
- **RNF-013 (Logs)**: Logs estruturados sem vazar senhas, tokens JWT ou dados pessoais sensíveis.
