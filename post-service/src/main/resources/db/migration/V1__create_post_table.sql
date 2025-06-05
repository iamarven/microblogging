CREATE SCHEMA IF NOT EXISTS post_service;

CREATE TABLE post_service.posts
(
    id         BIGSERIAL PRIMARY KEY,
    author_id  BIGINT    NOT NULL,
    content    TEXT      NOT NULL,
    media_url  TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);