CREATE SCHEMA IF NOT EXISTS like_service;

CREATE TABLE like_service.likes
(
    id         BIGSERIAL PRIMARY KEY NOT NULL,
    post_id    BIGINT                NOT NULL,
    user_id    BIGINT                NOT NULL,
    created_at TIMESTAMP             NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_like UNIQUE (user_id, post_id)
);