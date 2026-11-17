# Requisitos do Sistema — UniFlow Backend

Especificação completa dos módulos, requisitos funcionais (`RF-001`..`RF-092`) e requisitos não funcionais (`RNF-001`..`RNF-017`) derivados do Documento Mestre do Projeto.

---

## 1. Módulos do Sistema (`uniflow-backend`)

| Código | Módulo / Pacote | Responsabilidade | Versão Alvo |
| :--- | :--- | :--- | :--- |
| `MOD-01` | `auth` | Cadastro, login, logout, JWT, senhas e proteção de rotas | `MVP 1.0` |
| `MOD-02` | `usuario` | Perfil, dados pessoais, preferências e exclusão de conta | `MVP 1.0` / `v1.1` |
| `MOD-03` | `semestre` | Organização dos períodos letivos e semestre atual | `MVP 1.0` |
| `MOD-04` | `disciplina` | Gestão das matérias, professor, sala e carga horária | `MVP 1.0` |
| `MOD-05` | `aula` | Horários recorrentes, conflitos e grade semanal | `MVP 1.0` |
| `MOD-06` | `atividade` | Tarefas, provas, entregas, prioridades, conclusão e atrasos | `MVP 1.0` |
| `MOD-07` | `calendario` | Consolidação mensal, semanal e diária (e Dashboard) | `MVP 1.0` |
| `MOD-08` | `trabalho` | Projetos acadêmicos, etapas, progresso e atividades vinculadas | `MVP 1.0` / `v1.1` |
| `MOD-09` | `grupo` | Grupos acadêmicos, convites, papéis e compartilhamento | `v1.1` |
| `MOD-10` | `arquivo` | Metadados e Object Storage de materiais acadêmicos | `v1.1` |
| `MOD-11` | `notificacao` | Alertas internos (`MVP 1.0`), colaborativos (`v1.1`) e externos (`v2.0`) | `MVP 1.0`..`v2.0` |
| `MOD-12` | `planejamento` | Sessões manuais (`MVP 1.0`) e planejamento inteligente (`v2.0`) | `MVP 1.0` / `v2.0` |
| `MOD-13` | `assinatura` | Planos, assinaturas, limites e pagamentos reais | `v2.0 (Fora do MVP)` |
| `INFRA` | `common` / `config` | Exceções globais, paginação, logs, OpenAPI, Security, UTC | `MVP 1.0` |

---

## 2. Requisitos Funcionais (`RF`)

### `MOD-01` — Autenticação (`auth`)
- `RF-001`: Permitir cadastro de estudantes.
- `RF-002`: Permitir login com e-mail e senha.
- `RF-003`: Permitir logout.
- `RF-004`: Permitir atualização de senha.
- `RF-005`: Permitir recuperação de senha.
- `RF-006`: Permitir consulta dos dados do usuário autenticado.
- `RF-007`: Proteger endpoints que exigem autenticação.

### `MOD-02` — Usuários (`usuario`)
- `RF-008`: Consultar perfil.
- `RF-009`: Atualizar nome e informações pessoais.
- `RF-010`: Alterar preferências de notificação.
- `RF-011`: Excluir conta.

### `MOD-03` — Semestres (`semestre`)
- `RF-012`: Cadastrar semestre.
- `RF-013`: Listar semestres do usuário.
- `RF-014`: Consultar semestre.
- `RF-015`: Atualizar semestre.
- `RF-016`: Excluir semestre.
- `RF-017`: Definir semestre atual.
- `RF-018`: Consultar informações consolidadas do semestre.

### `MOD-04` — Disciplinas (`disciplina`)
- `RF-019`: Cadastrar disciplina.
- `RF-020`: Listar disciplinas do semestre.
- `RF-021`: Consultar disciplina.
- `RF-022`: Atualizar disciplina.
- `RF-023`: Excluir disciplina.
- `RF-024`: Cadastrar professor.
- `RF-025`: Definir carga horária.
- `RF-026`: Definir horários das aulas.

### `MOD-05` — Aulas (`aula`)
- `RF-027`: Cadastrar horário de aula.
- `RF-028`: Consultar horários.
- `RF-029`: Atualizar horário.
- `RF-030`: Excluir horário.
- `RF-031`: Visualizar grade semanal.

### `MOD-06` — Atividades (`atividade`)
- `RF-032`: Criar atividade.
- `RF-033`: Listar atividades.
- `RF-034`: Consultar atividade.
- `RF-035`: Atualizar atividade.
- `RF-036`: Excluir atividade.
- `RF-037`: Marcar atividade como concluída.
- `RF-038`: Alterar prioridade.
- `RF-039`: Definir prazo.
- `RF-040`: Definir tempo estimado.
- `RF-041`: Filtrar atividades por disciplina.
- `RF-042`: Filtrar por status.
- `RF-043`: Filtrar por prioridade.
- `RF-044`: Consultar atividades próximas do vencimento.

### `MOD-07` — Calendário (`calendario`)
- `RF-045`: Consultar calendário mensal.
- `RF-046`: Consultar calendário semanal.
- `RF-047`: Consultar calendário diário.
- `RF-048`: Exibir aulas.
- `RF-049`: Exibir provas e entregas.
- `RF-050`: Exibir trabalhos.
- `RF-051`: Navegar entre períodos.

### `MOD-08` — Trabalhos Acadêmicos (`trabalho`)
- `RF-052`: Criar trabalho.
- `RF-053`: Consultar trabalho.
- `RF-054`: Atualizar trabalho.
- `RF-055`: Excluir trabalho.
- `RF-056`: Associar trabalho a uma disciplina.
- `RF-057`: Definir data de entrega.
- `RF-058`: Criar etapas do trabalho.
- `RF-059`: Atualizar etapas.
- `RF-060`: Acompanhar progresso.
- `RF-061`: Associar atividades ao trabalho.
- `RF-062`: Registrar descrição e instruções.

### `MOD-09` — Grupos Acadêmicos (`grupo`, *v1.1*)
- `RF-063`: Criar grupo.
- `RF-064`: Convidar estudante.
- `RF-065`: Aceitar convite.
- `RF-066`: Recusar convite.
- `RF-067`: Remover participante.
- `RF-068`: Listar participantes.
- `RF-069`: Definir responsável por etapa.
- `RF-070`: Consultar atividades compartilhadas.

### `MOD-10` — Arquivos (`arquivo`, *v1.1*)
- `RF-071`: Enviar arquivos.
- `RF-072`: Consultar arquivos.
- `RF-073`: Excluir arquivos.
- `RF-074`: Associar arquivos a disciplinas ou trabalhos.

### `MOD-11` — Notificações (`notificacao`)
- `RF-075`: Consultar notificações.
- `RF-076`: Marcar notificação como lida.
- `RF-077`: Marcar todas como lidas.
- `RF-078`: Gerar alertas de prazos próximos.
- `RF-079`: Configurar preferências.

### `MOD-12` — Planejamento de Estudos (`planejamento`)
- `RF-080`: Criar sessão de estudo.
- `RF-081`: Associar sessão a uma disciplina.
- `RF-082`: Associar sessão a uma atividade.
- `RF-083`: Definir duração.
- `RF-084`: Registrar sessão concluída.
- `RF-085`: Consultar histórico.
- `RF-086`: Visualizar tempo estudado por disciplina.

### `MOD-13` — Assinaturas (`assinatura`, *v2.0 / Fora do MVP*)
- `RF-087`: Consultar planos.
- `RF-088`: Assinar plano.
- `RF-089`: Consultar assinatura.
- `RF-090`: Cancelar assinatura.
- `RF-091`: Controlar limites de utilização.
- `RF-092`: Registrar histórico de pagamentos.

---

## 3. Requisitos Não Funcionais (`RNF`)

| Código | Requisito | Escopo |
| :--- | :--- | :--- |
| `RNF-001` | API REST seguindo padrões HTTP | Backend |
| `RNF-002` | Autenticação segura (JWT stateless) | Backend |
| `RNF-003` | Autorização em todos os recursos privados | Backend |
| `RNF-004` | Validação de dados no backend (Bean Validation + domínio) | Backend |
| `RNF-005` | Senhas armazenadas com hash seguro (BCrypt) | Backend |
| `RNF-006` | Banco PostgreSQL | Backend |
| `RNF-007` | Versionamento do banco com Flyway | Backend |
| `RNF-008` | Documentação com OpenAPI/Swagger | Backend |
| `RNF-009` | Tratamento padronizado de exceções | Backend |
| `RNF-010` | Testes unitários e de integração (JUnit 5 + Mockito) | Backend |
| `RNF-011` | Interface responsiva | Frontend (`N/A` no backend) |
| `RNF-012` | Paginação em consultas que possam crescer | Backend |
| `RNF-013` | Logs estruturados | Backend |
| `RNF-014` | Proteção contra acesso indevido a dados (isolamento multiusuário) | Backend |
| `RNF-015` | Configurações sensíveis via variáveis de ambiente | Backend |
| `RNF-016` | Compatibilidade com fusos horários | Backend |
| `RNF-017` | Acessibilidade básica e navegação por teclado | Frontend (`N/A` no backend) |

> Para a matriz completa ligando cada `RF` e `RNF` às tarefas `BACK-XXX`, consulte [ROADMAP.md](file:///c:/uniflow_api/api/docs/ROADMAP.md).
