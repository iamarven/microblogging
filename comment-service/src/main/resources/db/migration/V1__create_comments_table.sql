CREATE SCHEMA IF NOT EXISTS comment_service;

CREATE TABLE comment_service.comments
(
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    content    TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP
);