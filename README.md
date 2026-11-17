# UniFlow Backend (`uniflow-backend`)

> **Sua vida universitária, em um só lugar.** — Repositório exclusivo da API REST backend do UniFlow.

## Stack Principal
- **Linguagem**: Java 21 LTS
- **Framework**: Spring Boot 3.5.x (Web, Spring Data JPA, Spring Security, Validation) *(alinhamento inicial do `pom.xml` previsto em `BE-001`..`BE-002`)*
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
- `/executar BE-XXX` (ex.: `/executar be-001`, `/executar be-044`) — Analisa a tarefa, apresenta o plano técnico para aprovação e, após autorizado, executa e atualiza os `.md` de controle
- `/status-projeto` — Audita código vs backlog e atualiza `docs/STATUS.md`
- `/relatorio-projeto` — Atualiza o relatório executivo em `docs/RELATORIO.md`

## Verificação Local
```powershell
.\mvnw.cmd test-compile
.\mvnw.cmd test
```
