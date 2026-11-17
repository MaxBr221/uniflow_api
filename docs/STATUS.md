# Status do Projeto — UniFlow Backend (`uniflow-backend`)

> **Última atualização**: `2026-11-16`
> **Repositório**: `c:\uniflow_api\api` (exclusivo Backend)
> **Estado Geral**: `INICIALIZACAO / SKELETON` — Apenas esqueleto inicial Spring Initializr + documentação e backlog oficial (`BE-001`..`BE-158`); nenhum épico funcional iniciado ainda.

---

## 1. Resumo Executivo de Progresso

| Recorte | Concluído | Em Andamento | Pendente | Fora do MVP | Total | % Concluído |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **MVP 1.0 (Épicos 1–8: `BE-001`..`BE-119`)** | 0 | 0 | 119 | 0 | 119 | **0.0%** |
| **Versão 1.1 (Épicos 9–10: `BE-120`..`BE-144`)** | 0 | 0 | 25 | 0 | 25 | **0.0%** |
| **Versão 2.0 / Futura (Épicos 11–12: `BE-145`..`BE-158`)** | 0 | 0 | 0 | 14 | 14 | **0.0%** |
| **TOTAL GERAL** | **0** | **0** | **144** | **14** | **158** | **0.0%** |

```text
MVP 1.0 (Épicos 1-8)    : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 119)
v1.1    (Épicos 9-10)   : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 25)
v2.0    (Épicos 11-12)  : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 14) - Aguardando validação do MVP
```

---

## 2. Progresso por Épico e Módulo

| Épico | Título | Módulo(s) | Versão Alvo | Faixa | Concluídas | Status |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | Configuração e estrutura do projeto | `config`, `common` | `MVP 1.0` | `BE-001`..`BE-012` | 0 / 12 | Não iniciado |
| **2** | Autenticação e usuários | `MOD-01`, `MOD-02` | `MVP 1.0` | `BE-013`..`BE-031` | 0 / 19 | Não iniciado |
| **3** | Semestres acadêmicos | `MOD-03` | `MVP 1.0` | `BE-032`..`BE-045` | 0 / 14 | Não iniciado |
| **4** | Disciplinas | `MOD-04` | `MVP 1.0` | `BE-046`..`BE-058` | 0 / 13 | Não iniciado |
| **5** | Aulas e horários | `MOD-05` | `MVP 1.0` | `BE-059`..`BE-070` | 0 / 12 | Não iniciado |
| **6** | Atividades acadêmicas | `MOD-06` | `MVP 1.0` | `BE-071`..`BE-089` | 0 / 19 | Não iniciado |
| **7** | Trabalhos e etapas | `MOD-08` | `MVP 1.0` | `BE-090`..`BE-107` | 0 / 18 | Não iniciado |
| **8** | Dashboard e calendário | `MOD-07` | `MVP 1.0` | `BE-108`..`BE-119` | 0 / 12 | Não iniciado |
| **9** | Grupos e colaboração | `MOD-09`, `MOD-08` | `Versão 1.1` | `BE-120`..`BE-132` | 0 / 13 | Aguardando MVP |
| **10** | Arquivos e notificações | `MOD-10`, `MOD-11` | `Versão 1.1` | `BE-133`..`BE-144` | 0 / 12 | Aguardando MVP |
| **11** | Planejamento inteligente e IA | `MOD-12` | `Versão 2.0` | `BE-145`..`BE-151` | 0 / 7 | Fora do MVP |
| **12** | Monetização | `MOD-13` | `Versão Futura` | `BE-152`..`BE-158` | 0 / 7 | Fora do MVP |

---

## 3. Estado Real Observado no Workspace

Fatos verificados diretamente em `c:\uniflow_api\api`:
- **Java**: `21.0.9 LTS` ativo no ambiente.
- **Compilação Maven**: `.\mvnw.cmd test-compile` executado em `2026-11-16` com resultado **`BUILD SUCCESS`**.
- **Spring Boot no `pom.xml`**: atualmente `4.1.2` (com `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `spring-boot-starter-security`, `spring-boot-starter-webmvc`, `flyway-database-postgresql`, `springdoc-openapi-starter-webmvc-ui:3.2.0`, `postgresql`, `lombok`).
- **Código Java existente**: apenas [ApiApplication.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/api/ApiApplication.java) e [ApiApplicationTests.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/ApiApplicationTests.java) no pacote `com.uniflow.api`.
- **Configuração atual**: apenas [application.properties](file:///c:/uniflow_api/api/src/main/resources/application.properties) (`spring.application.name=api`).
- **Migrations Flyway**: pasta `src/main/resources/db/migration/` está **vazia** (`0` migrations).

---

## 4. Alertas Técnicos e Decisões Pendentes

1. **Alinhamento obrigatório para Spring Boot `3.5.x` (`BE-001` / `BE-002`)**:
   - O `pom.xml` gerado pelo Initializr está em `4.1.2`, mas a stack obrigatória do projeto exige **Spring Boot 3.5.x** + Java 21.
   - `BE-001` e `BE-002` farão o alinhamento do parent `3.5.x`, `spring-boot-starter-web`, `spring-boot-starter-validation`, `springdoc-openapi` (`2.8.x`), JWT (`jjwt`) e Testcontainers/Mockito/JUnit.
2. **Pacote base (`BE-005`)**:
   - Reorganizar `com.uniflow.api` para a raiz `com.uniflow` com os 15 pacotes de domínio (`auth`, `usuario`, `semestre`, `disciplina`, `aula`, `atividade`, `calendario`, `trabalho`, `grupo`, `arquivo`, `notificacao`, `planejamento`, `assinatura`, `common`, `config`).
3. **Execução de `.\mvnw.cmd test`**:
   - `ApiApplicationTests` (`@SpringBootTest`) ainda não possui datasource PostgreSQL nem `application.yml` (`BE-003`, `BE-004`, `BE-006`).
4. **Regra de processo em 2 etapas**:
   - Nenhuma tarefa pode ser implementada antes da apresentação do plano técnico e autorização explícita do usuário; módulos funcionais não devem iniciar antes da estrutura inicial (`Épico 1`).

---

## 5. Próximos Passos Recomendados (Épico 1 — Estrutura Inicial)

1. `/executar BE-001` + `/executar BE-002` — Alinhar projeto Maven para Java 21 + **Spring Boot 3.5.x** e configurar dependências completas.
2. `/executar BE-003` + `/executar BE-004` + `/executar BE-006` — Docker/PostgreSQL, variáveis de ambiente, Flyway e `application.yml` multiambiente.
3. `/executar BE-005` + `/executar BE-007` + `/executar BE-008` + `/executar BE-009` — Estrutura dos 15 pacotes, tratamento global de exceções com **Problem Details (RFC 7807)** e Bean Validation.
4. `/executar BE-010` + `/executar BE-011` + `/executar BE-012` — Swagger/OpenAPI, logs estruturados e base de testes JUnit 5 + Mockito.
