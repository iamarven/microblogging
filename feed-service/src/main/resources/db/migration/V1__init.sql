CREATE SCHEMA IF NOT EXISTS feed_service;

CREATE TABLE feed_service.feeds
(
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL,
    post_id    BIGINT    NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT unique_user_post UNIQUE (user_id, post_id)
);

CREATE TABLE feed_service.subscriptions
(
    id          BIGSERIAL PRIMARY KEY,
    follower_id BIGINT    NOT NULL,
    followee_id BIGINT    NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT unique_follower_followee UNIQUE (follower_id, followee_id)
);

CREATE TABLE feed_service.outbox_events
(
    id             BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR   NOT NULL,
    aggregate_id   BIGINT    NOT NULL,
    event_type     VARCHAR   NOT NULL,
    payload        JSONB     NOT NULL,
    sent           BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_feeds_user_id ON feed_service.feeds (user_id);
CREATE INDEX idx_feeds_created_at ON feed_service.feeds (created_at);
CREATE INDEX idx_feeds_user_id_and_created_at ON feed_service.feeds (user_id, created_at);
CREATE INDEX idx_outbox_events_sent_false ON feed_service.outbox_events (sent) WHERE sent = FALSE;