-- V1: Baseline inicial do banco PostgreSQL para o UniFlow Backend.
-- Garante suporte a geracao de UUIDs e funcoes criptograficas nativas quando necessario.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
