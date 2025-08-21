CREATE SCHEMA IF NOT EXISTS notification_service;

CREATE TABLE notification_service.notifications
(
    id          BIGSERIAL PRIMARY KEY,
    sender_id   BIGINT      NOT NULL,
    receiver_id BIGINT      NOT NULL,
    entity_id   BIGINT      NOT NULL,
    type        VARCHAR(50) NOT NULL,
    message     TEXT        NOT NULL,
    is_read     BOOLEAN     NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_receiver_id ON notification_service.notifications(receiver_id);
CREATE INDeX idx_notification_entity_id_type ON notification_service.notifications(entity_id, type);