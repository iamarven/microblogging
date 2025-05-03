CREATE SCHEMA IF NOT EXISTS feed_service;

CREATE TABLE feed_service.subscriptions (
    id BIGSERIAL PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    followee_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT unique_follower_followee UNIQUE (follower_id, followee_id)
);