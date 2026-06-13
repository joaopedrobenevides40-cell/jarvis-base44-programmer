-- Schema Supabase para memória vetorial do J.A.R.V.I.S
-- Execute isso no SQL Editor do Supabase

-- Habilita extensão pgvector
CREATE EXTENSION IF NOT EXISTS vector;

-- Tabela de memória longa
CREATE TABLE jarvis_memory (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    content TEXT NOT NULL,
    embedding VECTOR(1536),  -- dimensão do text-embedding-3-small
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Índice para busca rápida por similaridade
CREATE INDEX ON jarvis_memory
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);

-- Função de busca semântica
CREATE OR REPLACE FUNCTION search_memory(
    query_embedding VECTOR(1536),
    match_count INT DEFAULT 5
)
RETURNS TABLE (
    id UUID,
    content TEXT,
    metadata JSONB,
    similarity FLOAT
)
LANGUAGE SQL STABLE
AS $$
    SELECT
        id,
        content,
        metadata,
        1 - (embedding <=> query_embedding) AS similarity
    FROM jarvis_memory
    ORDER BY embedding <=> query_embedding
    LIMIT match_count;
$$;

-- Row Level Security
ALTER TABLE jarvis_memory ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Service role full access"
ON jarvis_memory FOR ALL
TO service_role USING (true);
