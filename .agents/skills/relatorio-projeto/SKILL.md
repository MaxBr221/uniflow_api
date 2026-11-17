---
name: relatorio-projeto
description: Gera ou atualiza o relatório executivo e técnico de andamento do UniFlow Backend em docs/RELATORIO.md com base no backlog, status, código e resultados de build/testes. Pode ser invocado com /relatorio-projeto.
---

# Comando `/relatorio-projeto`

Use esta skill quando o usuário invocar `/relatorio-projeto` ou solicitar um relatório consolidado de andamento do backend.

## Passos
1. Leia `docs/BACKLOG.md`, `docs/STATUS.md`, `docs/ROADMAP.md` e `docs/RELATORIO.md`.
2. Inspecione migrations em `src/main/resources/db/migration/`, pacotes em `src/main/java/`, testes em `src/test/java/` e execute `.\mvnw.cmd test-compile` (ou `.\mvnw.cmd test` se o ambiente de testes já estiver ativo) para registrar resultados reais.
3. Atualize `docs/RELATORIO.md` preservando o histórico cronológico e atualizando o Resumo Executivo, Indicadores de Qualidade e Riscos/Pendências.
4. Entregue ao usuário um resumo objetivo apontando para `docs/RELATORIO.md`.
