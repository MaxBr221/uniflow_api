# AGENTS.md — Regras do Repositório `uniflow-backend`

Este repositório (`c:\uniflow_api\api`) é responsável **exclusivamente pelo backend** da plataforma **UniFlow** (`uniflow-backend`).

## 1. Escopo, Stack e Arquitetura Obrigatória
- **Escopo**: Backend only. **Nunca** crie código de frontend (React, TypeScript, Vite, Tailwind, UI) neste repositório.
- **Stack obrigatória**: Java 21, Spring Boot (**`4.1.2`** mantido conforme decisão do usuário em `2026-11-19` sobre o projeto gerado no Initializr; especificação original citava `3.5.x`), Maven, PostgreSQL, Spring Data JPA, Hibernate, Spring Security, JWT, BCrypt, Flyway, Bean Validation, Lombok, Springdoc OpenAPI, JUnit 5, Mockito.
  - *Entrypoint modular*: `com.uniflow.UniflowApplication` escaneando todos os pacotes `com.uniflow.*` (estruturados em `BE-005`).
- **Arquitetura**: Monólito Modular com separação por domínio — **não implementar microsserviços**.
- **Módulos**: `auth`, `usuario`, `semestre`, `disciplina`, `aula`, `atividade`, `calendario`, `trabalho`, `grupo`, `arquivo`, `notificacao`, `planejamento`, `assinatura`, `common`, `config`.
- **Organização interna por responsabilidades**: `controller/`, `service/`, `repository/`, `entity/`, `dto/`, `mapper/`, `enums/`.

## 2. As 16 Regras Obrigatórias de Desenvolvimento
1. Nunca implementar regras de negócio nos controllers.
2. Nunca retornar entidades JPA diretamente nos endpoints.
3. Utilizar DTOs dedicados de entrada e saída.
4. Implementar validações no backend (Bean Validation + invariantes de domínio).
5. Utilizar transações (`@Transactional`) onde houver necessidade de consistência.
6. Utilizar **Flyway** para todas as alterações no banco de dados.
7. Nunca utilizar `ddl-auto=create` ou `update` em ambientes compartilhados (`validate` em dev/prod).
8. Implementar isolamento rigoroso de dados entre usuários.
9. Nunca confiar em `usuarioId` enviados pelo frontend para autorizar operações.
10. Utilizar sempre o usuário autenticado do contexto do Spring Security (`SecurityContext`).
11. Implementar tratamento global padronizado de exceções.
12. Documentar endpoints no Swagger/OpenAPI.
13. Escrever testes unitários e de integração.
14. Não implementar microsserviços.
15. Não criar abstrações desnecessárias (sem `*Service` + `*ServiceImpl` para serviços únicos).
16. Não adicionar funcionalidades fora do escopo do backlog sem autorização.

## 3. Regra de Escopo e Versões (MVP Gate)
- **Versão MVP 1.0**: Épicos 1 a 8 (`BE-001`..`BE-119`: Estrutura inicial, Auth/Usuários, Semestres, Disciplinas, Aulas, Atividades, Trabalhos/Etapas, Dashboard e Calendário).
- **Versão 1.1**: Épicos 9 e 10 (`BE-120`..`BE-144`: Grupos/Colaboração, Arquivos e Notificações).
- **Versão 2.0 / Futura**: Épicos 11 e 12 (`BE-145`..`BE-158`: Planejamento inteligente/IA e Monetização/Assinaturas).
- **Regra rígida**: **NÃO implementar módulos funcionais antes de concluir a estrutura inicial (Épico 1)** e **NÃO implementar funcionalidades da versão 2.0/futura antes de validar o MVP 1.0**, salvo autorização explícita do usuário.

## 4. Processo Obrigatório de Desenvolvimento
- **Via comando `/run <ID>`**: Quando o usuário invocar `/run <ID>` para uma tarefa `PENDENTE` cujas dependências já estejam concluídas e dentro da fase permitida (`Épico 1` antes de módulos funcionais; `MVP Gate` para `v2.0`), o próprio `/run <ID>` **vale como autorização explícita** — executar análise, implementação, testes, verificação e atualização dos documentos **diretamente sem pedir confirmação intermediária**.
- **Via `/executar <ID>` ou solicitações livres/bloqueadas**:
  1. Ler a documentação em `docs/` (`PRODUCT_SPEC.md`, `REQUIREMENTS.md`, `BUSINESS_RULES.md`, `DATABASE_DESIGN.md`, `API_CONTRACT.md`, `SECURITY.md`, `ARCHITECTURE.md`).
  2. Consultar `docs/BACKLOG.md` e verificar se as dependências da tarefa estão concluídas.
  3. Verificar o estado atual do código no projeto.
  4. Apresentar um **plano técnico** (arquivos a criar/modificar, migrations, testes e possíveis impactos em funcionalidades existentes).
  5. **Aguardar autorização explícita do usuário** antes de implementar a tarefa.

Durante e após a implementação (somente do escopo autorizado):
1. Executar os testes e o build Maven (`.\mvnw.cmd test`).
2. Verificar segurança e ausência de regressões.
3. Atualizar `docs/BACKLOG.md` (marcar como `CONCLUIDO` **somente após validação e testes passando**), `docs/STATUS.md`, `docs/RELATORIO.md`, `docs/API_CONTRACT.md` e `docs/DATABASE_DESIGN.md`.
4. Informar: o que foi implementado, arquivos criados, arquivos modificados, regras de negócio atendidas, testes executados/resultados e pendências identificadas.
