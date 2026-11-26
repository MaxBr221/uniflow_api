# Segurança e Isolamento de Dados — UniFlow Backend

Este documento define os requisitos obrigatórios de segurança para toda a API REST (`/api/v1`) do `uniflow-backend`.

---

## 1. Princípios Invioláveis

1. **Autenticação Stateless via JWT + Refresh Token com Rotação (`BE-021`..`BE-026`, `RNF-002`)**:
   - Sem sessão HTTP no servidor (`SessionCreationPolicy.STATELESS`).
   - Tokens JWT assinados com chave secreta injetada exclusivamente via variável de ambiente (`RNF-015`).
   - Refresh tokens persistidos via hash com rotação a cada renovação (`BE-024`, `BE-025`) e revogação no logout (`BE-026`).
   - Bloqueio automático de reutilização de refresh token já rotacionado/revogado.
2. **Hash Seguro de Senhas (`BE-020`, `RNF-005`)**:
   - Utilizar `BCryptPasswordEncoder`.
   - `senhaHash` nunca pode aparecer em DTOs de saída, `ProblemDetail` ou logs (`RNF-013`).
3. **Confiança Zero em IDs Enviados pelo Frontend**:
   - **Nunca** aceitar `usuarioId` no body, path ou query param para autorizar operações.
   - Sempre extrair o usuário autenticado do `SecurityContext` (`CurrentUserService` e `OwnershipValidator`, evitando autenticação manual em controllers).
4. **Isolamento Estrito Entre Usuários (`BE-030`, `BE-031`, `BE-057`, `BE-131`, `RNF-003`, `RNF-014`)**:
   - Toda consulta/mutação deve filtrar ou validar propriedade pelo usuário autenticado via `OwnershipValidator` (`403 FORBIDDEN_OPERATION` / `404 RESOURCE_NOT_FOUND`), com `JwtAuthenticationFilter` obrigatório no `SecurityConfig` e auditoria contínua em `SecurityAuditIntegrationTest` e `AuthEndToEndFlowIntegrationTest`.
   - A única exceção controlada ocorre na `Versão 1.1` para trabalhos e arquivos explicitamente compartilhados via participação aceita em `Grupo` (`BE-130`, `BE-131`).

---

## 2. Matriz de Acesso por Rota

| Rota / Prefixo | Política | Observação |
| :--- | :--- | :--- |
| `POST /api/v1/auth/register` | Pública (`permitAll`) | Cadastro de usuário (`BE-019`) |
| `POST /api/v1/auth/login` | Pública (`permitAll`) | Mensagem genérica em falha de credencial (`BE-023`) |
| `POST /api/v1/auth/refresh` | Pública (`permitAll`) | Exige refresh token válido não revogado (`BE-025`) |
| `/v3/api-docs/**`, `/swagger-ui/**` | Controlada por perfil | Habilitada em `dev`, protegida/configurável em `prod` (`BE-010`) |
| `/api/v1/**` (todas as demais rotas) | Autenticada (`authenticated`) | Exige Bearer access token JWT válido (`BE-021`, `BE-022`) |

---

## 3. Regras de Ownership em Cascata

| Recurso | Regra de Autorização Obrigatória |
| :--- | :--- |
| `Usuario` | Apenas o próprio usuário autenticado acessa/altera `/auth/me`, `/auth/password`, `/usuarios/me`. |
| `Semestre` | `semestre.usuario.id == currentUserId`. |
| `Disciplina` | `disciplina.semestre.usuario.id == currentUserId`. |
| `Aula` | `aula.disciplina.semestre.usuario.id == currentUserId`. |
| `Atividade` | `atividade.usuario.id == currentUserId`; se `disciplinaId != null`, validar ownership da disciplina. |
| `Trabalho` e `EtapaTrabalho` | `trabalho.criador.id == currentUserId` (MVP 1.0) ou membro ativo do `Grupo` quando `compartilhado == true` (v1.1). |
| `Grupo` *(v1.1)* | Apenas participantes visualizam; apenas `ADMINISTRADOR` convida/remove; apenas convidado aceita/rejeita. |
| `Arquivo` *(v1.1)* | Validar extensão/tamanho, sanitizar filename e checar autorização em upload/download/exclusão. |
| `Notificacao` *(v1.1)* | `notificacao.usuario.id == currentUserId`. |

---

## 4. Checklist de Segurança Antes de Concluir Qualquer Tarefa

- [ ] Nenhum endpoint privado ficou exposto sem autenticação no `SecurityConfig`.
- [ ] Nenhum controller retorna entidade JPA diretamente.
- [ ] `usuarioId` é extraído exclusivamente do `SecurityContext`.
- [ ] Queries e buscas por ID validam ownership do recurso.
- [ ] Erros retornam `ProblemDetail` RFC 7807 sem vazar stack trace, SQL, tokens ou hashes.
- [ ] Existe teste comprovando que Usuário B não acessa ou altera recursos do Usuário A.
