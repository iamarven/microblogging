CREATE TABLE post_service.outbox_events
(
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR NOT NULL,
    aggregate_id BIGINT NOT NULL,
    event_type VARCHAR NOT NULL,
    payload JSONB NOT NULL,
    sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
)