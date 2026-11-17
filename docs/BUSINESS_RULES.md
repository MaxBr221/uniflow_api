# Regras de Negócio — UniFlow Backend

Consolidação oficial das regras de negócio por módulo para implementação e testes do backend (`uniflow-backend`).

---

## MOD-01 — Autenticação
- **Enums (`BE-014`)**: `StatusUsuario` = `ATIVO`, `INATIVO`, `BLOQUEADO`, `EXCLUIDO`.
- **Regras (`BE-013`..`BE-031`)**:
  1. O e-mail deve ser único no sistema (comparação case-insensitive / normalizado em lowercase).
  2. A senha deve ser armazenada exclusivamente com hash **BCrypt** (nunca em texto puro e nunca retornada em DTOs ou logs).
  3. Autenticação stateless com **access token JWT** + **refresh token com rotação** (`BE-024`, `BE-025`) e **revogação de sessão no logout** (`BE-026`).
  4. O backend deve identificar o usuário autenticado pelo `SecurityContext` (token JWT), **nunca** confiando em `usuarioId` enviado pelo frontend.
  5. Usuário só pode acessar seus próprios recursos, exceto recursos explicitamente compartilhados via grupos (`v1.1`).

## MOD-02 — Usuários
- **RF-008..RF-011**: Consultar perfil, atualizar dados pessoais, alterar preferências de notificação e excluir conta.
- **Regras**:
  1. Todos os dados pessoais possuem validação Bean Validation no backend.
  2. Exclusão de conta deve seguir política explícita de retenção/anonimização e revogar imediatamente sessões/tokens.
  3. A partir da v1.1, exclusão de conta deve preservar a integridade de grupos e trabalhos compartilhados ativos.

## MOD-03 — Semestres
- **Enums**: `StatusSemestre` = `PLANEJADO`, `EM_ANDAMENTO`, `CONCLUIDO`.
- **Dados**: `nome`, `instituicao` (opcional), `curso` (opcional), `periodoAcademico`, `dataInicial`, `dataFinal`, `status`, `atual`.
- **Regras**:
  1. `dataInicial` deve ser estritamente anterior a `dataFinal` (`dataInicial < dataFinal`).
  2. Todo semestre pertence a exatamente um usuário.
  3. Um usuário pode possuir vários semestres.
  4. Apenas **um** semestre pode estar marcado como `atual = true` por usuário (garantido transacionalmente e por índice único parcial no PostgreSQL).
  5. Não permitir exclusão silenciosa de semestre quando houver disciplinas ou dados dependentes vinculados (`409 Conflict`).

## MOD-04 — Disciplinas
- **Dados**: `nome`, `codigo` (opcional), `professor` (opcional), `cargaHoraria` (opcional, > 0 quando informada), `sala` (opcional), `corHex`, `semestreId`.
- **Regras**:
  1. Uma disciplina pertence a um semestre; um semestre pode possuir várias disciplinas.
  2. A disciplina não pode ser associada a um semestre pertencente a outro usuário.
  3. Horários de aulas vinculados à disciplina possuem `diaSemana`, `horaInicial` e `horaFinal`, com `horaInicial < horaFinal`.

## MOD-05 — Aulas
- **RF-027..RF-031**: CRUD de horários recorrentes e visualização de grade semanal.
- **Regras**:
  1. Uma disciplina pode possuir vários horários recorrentes; cada horário pertence a uma disciplina.
  2. `horaInicial` deve ser estritamente anterior a `horaFinal`.
  3. O backend deve detectar conflitos de horário (sobreposição no mesmo dia da semana entre disciplinas do mesmo semestre/usuário).
  4. Conflitos devem ser **informados ao usuário** na resposta sem impedir o cadastro ou atualização.

## MOD-06 — Atividades
- **Enums**:
  - `TipoAtividade`: `TAREFA`, `PROVA`, `TRABALHO`, `SEMINARIO`, `LEITURA`, `REVISAO`, `OUTRO`.
  - `StatusAtividade`: `PENDENTE`, `EM_ANDAMENTO`, `CONCLUIDA`, `ATRASADA` *(estado lógico calculado)*.
  - `PrioridadeAtividade`: `BAIXA`, `MEDIA`, `ALTA`, `URGENTE`.
- **Regras**:
  1. Uma atividade pertence a um usuário e pode opcionalmente estar associada a uma disciplina (do próprio usuário).
  2. A data de entrega (`prazo`) é opcional.
  3. `tempoEstimadoMinutos` deve ser estritamente positivo (`> 0`) quando informado.
  4. Atividade concluída nunca deve ser contabilizada como pendente ou atrasada.
  5. **Regra crítica de atraso (`Épico 6`)**: `ATRASADA` deve ser um **estado calculado** com base no prazo e na conclusão (`prazo != null && prazo < agora && status != CONCLUIDA`), não necessariamente um status persistido no banco.
  6. Concluir uma atividade deve registrar `concluidaEm` (`dataConclusao`).
  7. Na v1.1, atividades compartilhadas devem respeitar as permissões do grupo.

## MOD-07 — Calendário
- **RF-045..RF-051**: Consultas mensal, semanal e diária consolidando aulas, provas/atividades e trabalhos.
- **Regras**:
  1. O calendário é uma projeção consolidada de registros já existentes (`aula`, `atividade`, `trabalho`).
  2. É **proibido** criar tabelas ou registros duplicados apenas para exibição no calendário.
  3. Deve respeitar o fuso horário nas fronteiras de dia/semana/mês.

## MOD-08 — Trabalhos Acadêmicos
- **Enums**: `StatusTrabalho` = `PLANEJAMENTO`, `EM_ANDAMENTO`, `EM_REVISAO`, `CONCLUIDO`, `CANCELADO`.
- **Regras**:
  1. Um trabalho pode possuir várias etapas (`titulo`, `descricao`, `prazo`, `status`, `responsavel` opcional).
  2. A conclusão ou alteração de etapas recalcula automaticamente o progresso do trabalho.
  3. Um trabalho pode ser individual (MVP 1.0) ou compartilhado via grupo (v1.1).
  4. A disciplina associada ao trabalho deve pertencer a um semestre autorizado do estudante.
  5. Atividades existentes podem ser associadas ao trabalho (desde que autorizadas para o mesmo usuário/grupo).

## MOD-09 — Grupos Acadêmicos *(Versão 1.1)*
- **Enums**:
  - `PapelGrupo`: `ADMINISTRADOR`, `PARTICIPANTE`.
  - `StatusConvite`: `PENDENTE`, `ACEITO`, `RECUSADO`.
- **Regras**:
  1. Todo grupo possui um criador (que inicia como `ADMINISTRADOR`) e pode possuir vários participantes.
  2. Somente participantes autorizados podem visualizar os dados do grupo.
  3. Somente `ADMINISTRADOR` pode convidar ou remover participantes.
  4. Apenas o usuário destinatário do convite pode aceitá-lo ou recusá-lo.
  5. O sistema jamais pode permitir acesso a trabalhos ou atividades privadas de usuários que não pertencem ao grupo (ou que não estejam explicitamente compartilhados com o grupo).

## MOD-10 — Arquivos *(Versão 1.1)*
- **RF-071..RF-074**: Upload, consulta, exclusão e associação a disciplinas ou trabalhos.
- **Regras**:
  1. Validar extensão permitida e tamanho máximo no backend.
  2. **Não armazenar arquivos como grandes objetos (`BYTEA` / `OID`) no PostgreSQL**; usar Object Storage (ex.: S3 compatível) e guardar apenas metadados no banco.
  3. Verificar autorização do usuário antes de permitir upload, listagem, download ou exclusão.
  4. **Não confiar no nome do arquivo enviado pelo cliente** para o armazenamento (gerar chave única segura e sanitizar o nome de exibição).

## MOD-11 — Notificações
- **Enums**: `TipoNotificacao` = `PRAZO_PROXIMO`, `ATIVIDADE_ATRASADA`, `NOVO_CONVITE`, `ATIVIDADE_ATRIBUIDA`, `TRABALHO_ATUALIZADO`.
- **Regras**:
  1. MVP 1.0 utiliza notificações internas no sistema (e-mail e push ficam para evolução posterior na v2.0).
  2. Usuário só consulta e marca como lidas as próprias notificações.
  3. Alertas automáticos devem respeitar as preferências configuradas pelo usuário (`MOD-02`) e evitar duplicação.

## MOD-12 — Planejamento de Estudos
- **RF-080..RF-086**: Sessões manuais de estudo associadas a disciplina/atividade, duração, conclusão, histórico e tempo por disciplina.
- **Regras**:
  1. Duração da sessão deve ser estritamente positiva (`> 0`).
  2. Vínculo com disciplina ou atividade é opcional, mas quando informado deve pertencer ao usuário autenticado.
  3. Histórico deve permitir filtro por período; somatório de tempo considera apenas sessões concluídas.
  4. Automações e sugestões inteligentes pertencem à **Versão 2.0**.

## MOD-13 — Assinaturas *(Fora do MVP — Versão 2.0)*
- **RF-087..RF-092**: Planos, assinatura, cancelamento, limites de uso e histórico de pagamentos.
- **Regra**: **Não implementar pagamentos fictícios no MVP.** Somente integrar após validação comercial com gateway real.
