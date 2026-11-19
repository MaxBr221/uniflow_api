# Status do Projeto — UniFlow Backend (`uniflow-backend`)

> **Última atualização**: `2026-11-19`
> **Repositório**: `c:\uniflow_api\api` (exclusivo Backend)
> **Estado Geral**: `EM_ANDAMENTO (Épico 1)` — `BE-001` e `BE-002` concluídas; preparando banco PostgreSQL, Flyway, ambientes e pacotes de domínio.

---

## 1. Resumo Executivo de Progresso

| Recorte | Concluído | Em Andamento | Pendente | Fora do MVP | Total | % Concluído |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **MVP 1.0 (Épicos 1–8: `BE-001`..`BE-119`)** | 2 | 0 | 117 | 0 | 119 | **1.7%** |
| **Versão 1.1 (Épicos 9–10: `BE-120`..`BE-144`)** | 0 | 0 | 25 | 0 | 25 | **0.0%** |
| **Versão 2.0 / Futura (Épicos 11–12: `BE-145`..`BE-158`)** | 0 | 0 | 0 | 14 | 14 | **0.0%** |
| **TOTAL GERAL** | **2** | **0** | **142** | **14** | **158** | **1.3%** |

```text
MVP 1.0 (Épicos 1-8)    : [░░░░░░░░░░░░░░░░░░░░]  1.7% (2 / 119)
v1.1    (Épicos 9-10)   : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 25)
v2.0    (Épicos 11-12)  : [░░░░░░░░░░░░░░░░░░░░]  0.0% (0 / 14) - Aguardando validação do MVP
```

---

## 2. Progresso por Épico e Módulo

| Épico | Título | Módulo(s) | Versão Alvo | Faixa | Concluídas | Status |
| :---: | :--- | :--- | :--- | :--- | :---: | :--- |
| **1** | Configuração e estrutura do projeto | `config`, `common` | `MVP 1.0` | `BE-001`..`BE-012` | 2 / 12 | Em andamento |
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

Fatos verificados diretamente em `c:\uniflow_api\api` (`2026-11-19`):
- **Java**: `21.0.9 LTS` ativo no ambiente.
- **Compilação Maven**: `.\mvnw.cmd test-compile` -> **`BUILD SUCCESS`** (`48.051s`).
- **Teste executado**: `.\mvnw.cmd test "-Dtest=DependenciesSmokeTest"` -> **`Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`** (`BUILD SUCCESS`).
- **Spring Boot no `pom.xml`**: `4.1.2` *(mantido a partir do Initializr por decisão do usuário em `2026-11-19`)*, com `spring-boot-starter-data-jpa`, `spring-boot-starter-flyway`, `spring-boot-starter-security`, `spring-boot-starter-validation`, `spring-boot-starter-webmvc`, `flyway-database-postgresql`, `springdoc-openapi-starter-webmvc-ui:3.2.0`, `jjwt` (`0.13.0`), `postgresql`, `lombok`, `spring-boot-starter-*-test`, `spring-boot-testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql`.
- **Código Java existente**: [ApiApplication.java](file:///c:/uniflow_api/api/src/main/java/com/uniflow/api/ApiApplication.java), [ApiApplicationTests.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/ApiApplicationTests.java) e [DependenciesSmokeTest.java](file:///c:/uniflow_api/api/src/test/java/com/uniflow/api/DependenciesSmokeTest.java) no pacote `com.uniflow.api`.
- **Configuração atual**: apenas [application.properties](file:///c:/uniflow_api/api/src/main/resources/application.properties) (`spring.application.name=api`).
- **Migrations Flyway**: pasta `src/main/resources/db/migration/` ainda está **vazia** (`0` migrations).

---

## 4. Alertas Técnicos e Decisões Pendentes

1. **Decisão de Versão Registrada (`2026-11-19`)**:
   - Usuário optou por manter **Spring Boot `4.1.2`** (gerado pelo Initializr) em vez de fazer downgrade para `3.5.x`; `BE-001` concluída.
2. **Pacote base (`BE-005`)**:
   - Próxima reorganização moverá/estruturará o monólito modular sob `com.uniflow.{auth,usuario,semestre,disciplina,aula,atividade,calendario,trabalho,grupo,arquivo,notificacao,planejamento,assinatura,common,config}`.
3. **Execução de `ApiApplicationTests` (`@SpringBootTest`)**:
   - Continua aguardando datasource PostgreSQL e `application.yml` (`BE-003`, `BE-004`, `BE-006`).
4. **Regra de processo em 2 etapas**:
   - Apresentar plano técnico e aguardar autorização explícita antes de implementar novas tarefas.

---

## 5. Próximos Passos Recomendados (Desbloqueados Agora no Épico 1)

1. `/run BE-003` — Configurar PostgreSQL (`compose.yaml`) e variáveis de ambiente (`.env.example`).
2. `/run BE-005` — Criar estrutura de pacotes por domínio (`com.uniflow.*`).
3. Na sequência: `/run BE-004` (Flyway) e `/run BE-006` (`application.yml` por ambiente).
