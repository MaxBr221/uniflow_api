---
name: uniflow-security-ownership
description: Checklist e regras obrigatórias de autenticação JWT, BCrypt, Spring Security stateless e validação estrita de propriedade (ownership) de recursos no backend UniFlow.
---

# Segurança e Ownership (`uniflow-backend`)

Aplique esta skill ao implementar ou revisar qualquer endpoint, filtro, entidade privada ou regra de acesso no UniFlow. Consulte também `docs/SECURITY.md`.

## 1. Regras Invioláveis (`MOD-01`, `RNF-002`, `RNF-003`, `RNF-005`, `RNF-014`)
1. **Identidade do Usuário Autenticado**:
   - **Nunca** confiar em `usuarioId` enviado pelo frontend no body, path ou query param.
   - Sempre extrair o usuário autenticado do `SecurityContext` (`CurrentUserService` / `@AuthenticationPrincipal`).
2. **Armazenamento de Senhas**:
   - Sempre usar `BCryptPasswordEncoder`.
   - Nunca retornar `senhaHash` em DTOs, exceções ou logs.
3. **Sessão Stateless & Rotas Protegidas**:
   - Apenas endpoints públicos de autenticação (`/api/v1/auth/register`, `/login`, `/forgot-password`, `/reset-password`) e docs habilitadas por perfil podem ser `permitAll()`.
   - Todas as demais rotas exigem autenticação (`authenticated()`).

## 2. Validação de Ownership em Cascata
- **Semestre**: `semestre.usuario.id == currentUserId`
- **Disciplina**: `disciplina.semestre.usuario.id == currentUserId`
- **Aula / Horário**: `aula.disciplina.semestre.usuario.id == currentUserId`
- **Atividade**: `atividade.usuario.id == currentUserId` (+ ownership da disciplina quando informada)
- **Trabalho e Etapas**: `trabalho.criador.id == currentUserId` (ou membro do grupo na v1.1)
- **Sessão de Estudo**: `sessao.usuario.id == currentUserId` (+ ownership de disciplina/atividade opcionais)
- **Notificação**: `notificacao.usuario.id == currentUserId`
- **Arquivo (`v1.1`)**: verificar autorização antes de upload, listagem, download e exclusão.

## 3. Verificação em Testes
- Sempre escrever testes automatizados comprovando que o Usuário B não acessa, altera ou exclui recursos do Usuário A.
