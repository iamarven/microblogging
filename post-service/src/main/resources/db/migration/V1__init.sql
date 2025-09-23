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

CREATE TABLE post_service.post_media
(
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL,
    file_name  TEXT      NOT NULL,
    file_url   TEXT      NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_post_media_post
        FOREIGN KEY (post_id)
            REFERENCES post_service.posts (id)
            ON DELETE CASCADE
);

CREATE TABLE post_service.outbox_events
(
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR NOT NULL,
    payload JSONB NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_post_media_post_id ON post_service.post_media (post_id);
CREATE INDEX idx_posts_created_at ON post_service.posts (created_at DESC);
CREATE INDEX idx_posts_updated_at ON post_service.posts (updated_at DESC);
CREATE INDEX idx_outbox_events_sent_false ON post_service.outbox_events (sent) WHERE sent = FALSE;