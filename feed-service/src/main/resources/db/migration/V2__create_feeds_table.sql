CREATE SCHEMA IF NOT EXISTS feed_service;

CREATE TABLE feed_service.feeds (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT unique_user_post UNIQUE (user_id, post_id)
);

CREATE INDEX idx_feeds_user_id ON feed_service.feeds(user_id);
CREATE INDEX idx_feeds_created_at ON feed_service.feeds(created_at);
CREATE INDEX idx_feeds_user_id_and_created_at ON feed_service.feeds(user_id, created_at);