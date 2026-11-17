# Especificação do Produto — UniFlow (`uniflow-backend`)

> **Slogan**: *Sua vida universitária, em um só lugar.*
> **Escopo deste repositório**: Backend (`uniflow-backend`) em Java 21 + Spring Boot 3.5.x + PostgreSQL. O frontend (`uniflow-frontend`, React + TypeScript) reside em repositório separado.

---

## 1. Visão e Objetivo do Produto

O **UniFlow** é uma plataforma SaaS para gerenciamento da vida acadêmica de estudantes universitários, reduzindo trabalho manual ao conectar informações acadêmicas (semestres, disciplinas, aulas, avaliações, trabalhos, materiais e planejamento) em ações organizadas.

O sistema permite que o estudante:
1. Organize seus semestres letivos.
2. Cadastre e gerencie disciplinas.
3. Controle horários recorrentes de aulas.
4. Cadastre provas, atividades e trabalhos.
5. Acompanhe datas de entrega e status.
6. Gerencie trabalhos individuais e em grupo.
7. Compartilhe responsabilidades com outros estudantes.
8. Organize materiais acadêmicos.
9. Visualize sua rotina em calendário (mensal, semanal e diário).
10. Planeje seus estudos e acompanhe sua evolução.

---

## 2. Público-Alvo

- **Público principal**: Estudantes de graduação de instituições brasileiras.
- **Públicos secundários**:
  - Estudantes de cursos técnicos.
  - Estudantes de pós-graduação.
  - Estudantes de ensino a distância (EAD).
  - Grupos acadêmicos e projetos universitários.

---

## 3. Proposta de Valor (Centralização Conectada)

O UniFlow conecta os módulos acadêmicos em um fluxo integrado: ao cadastrar um semestre com suas disciplinas, horários de aulas, avaliações, trabalhos e materiais, o backend consolida os compromissos em visão de calendário e dashboard centralizado (próximos prazos, tarefas pendentes, atrasadas e concluídas), evitando duplicação de dados.

---

## 4. Estratégia de Versões e Escopo (MVP Gate)

| Versão | Objetivo | Módulos Incluídos | Regra de Entrega |
| :--- | :--- | :--- | :--- |
| **MVP 1.0** | Gestão individual da vida acadêmica | `INFRA`, `MOD-01`..`MOD-08`, `MOD-11` (notificações internas), `MOD-12` (planejamento manual) | Prioridade absoluta até validação do MVP |
| **Versão 1.1** | Colaboração e trabalhos em grupo | `MOD-09` (Grupos), `MOD-10` (Arquivos), `MOD-11` (eventos colaborativos) | Após consolidação do MVP 1.0 |
| **Versão 2.0** | IA, automações e monetização | `MOD-11` (e-mail/push), `MOD-12` (planejamento inteligente), `MOD-13` (Assinaturas) | **Bloqueada até validação do MVP 1.0** |

> **Regra de negócio de produto**: Não implementar funcionalidades da Versão 2.0 (nem pagamentos fictícios) antes da validação do MVP 1.0, salvo autorização explícita do usuário.
