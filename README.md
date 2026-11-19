# UniFlow Backend (`uniflow-backend`)

> **Sua vida universitária, em um só lugar.** — Repositório exclusivo da API REST backend do UniFlow.

## Stack Principal
- **Linguagem**: Java 21 LTS
- **Framework**: Spring Boot `4.1.2` (Web MVC, Spring Data JPA, Spring Security, Validation) *(mantido a partir do Initializr por decisão em `BE-001`)*
- **Banco de Dados & Migrations**: PostgreSQL + Flyway
- **Documentação**: Springdoc OpenAPI / Swagger
- **Testes**: JUnit 5, Mockito e testes de integração

## Documentação e Acompanhamento (`docs/`)
- [PRODUCT_SPEC.md](file:///c:/uniflow_api/api/docs/PRODUCT_SPEC.md) — Visão do produto, público-alvo e escopo (MVP 1.0, v1.1, v2.0)
- [REQUIREMENTS.md](file:///c:/uniflow_api/api/docs/REQUIREMENTS.md) — Requisitos funcionais (`RF-001`..`RF-092`) e não funcionais (`RNF-001`..`RNF-017`)
- [BUSINESS_RULES.md](file:///c:/uniflow_api/api/docs/BUSINESS_RULES.md) — Regras de negócio consolidadas de `MOD-01` a `MOD-13`
- [DATABASE_DESIGN.md](file:///c:/uniflow_api/api/docs/DATABASE_DESIGN.md) — Modelo relacional PostgreSQL e plano Flyway
- [API_CONTRACT.md](file:///c:/uniflow_api/api/docs/API_CONTRACT.md) — Contrato HTTP `/api/v1` entre backend e `uniflow-frontend`
- [SECURITY.md](file:///c:/uniflow_api/api/docs/SECURITY.md) — Segurança (JWT, BCrypt, `SecurityContext`, isolamento e ownership)
- [ARCHITECTURE.md](file:///c:/uniflow_api/api/docs/ARCHITECTURE.md) — Arquitetura em Monólito Modular (`com.uniflow.*`)
- [BACKLOG.md](file:///c:/uniflow_api/api/docs/BACKLOG.md) — Backlog completo do backend (`BE-001` a `BE-158`) com critérios de aceite
- [ROADMAP.md](file:///c:/uniflow_api/api/docs/ROADMAP.md) — Roadmap, grafo de dependências e matriz de rastreabilidade
- [STATUS.md](file:///c:/uniflow_api/api/docs/STATUS.md) — Painel atualizado de progresso do projeto (por versão e módulo)
- [RELATORIO.md](file:///c:/uniflow_api/api/docs/RELATORIO.md) — Histórico cronológico de execuções, build e testes
- [SKILLS.md](file:///c:/uniflow_api/api/docs/SKILLS.md) — Guia das skills e comandos slash disponíveis

## Comandos do Agente (`.agents/skills/`)
- `/tarefas [filtro]` — Lista todas as tarefas do backlog (`BE-001`..`BE-158`) por épico e status (suporta filtros como `pendentes`, `desbloqueadas`, `epico 1`)
- `/run <ID>` (ex.: `/run back-022`, `/run BE-001`) — Analisa a tarefa, apresenta o plano técnico para aprovação e, após autorizado, executa e atualiza os `.md` de controle
- `/executar <ID>` (ex.: `/executar be-001`, `/executar be-044`) — Mesmo fluxo do `/run <ID>`
- `/status-projeto` — Audita código vs backlog e atualiza `docs/STATUS.md`
- `/relatorio-projeto` — Atualiza o relatório executivo em `docs/RELATORIO.md`

## Verificação Local
```powershell
.\mvnw.cmd test-compile
.\mvnw.cmd test
```
