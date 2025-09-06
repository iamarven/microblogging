CREATE SCHEMA IF NOT EXISTS profile_service;

CREATE TABLE profile_service.post_read_model
(
    post_id        BIGINT PRIMARY KEY,
    author_id      BIGINT      NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL,
    content        TEXT,
    likes_count    BIGINT      NOT NULL DEFAULT 0,
    comments_count BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE profile_service.comment_read_model
(
    comment_id  BIGINT PRIMARY KEY,
    post_id     BIGINT      NOT NULL,
    author_id   BIGINT      NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    content     TEXT        NOT NULL,
    likes_count BIGINT      NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_prm_author_created_at ON profile_service.post_read_model(author_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_crm_post_created ON profile_service.comment_read_model (post_id, created_at DESC);