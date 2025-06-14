CREATE SCHEMA IF NOT EXISTS comment_service;

CREATE TABLE comment_service.comments
(
    id         BIGSERIAL PRIMARY KEY,
    post_id    BIGINT    NOT NULL,
    user_id    BIGINT    NOT NULL,
    content    TEXT      NOT NULL,
    parent_id  BIGINT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,

    CONSTRAINT fk_comments_parent
        FOREIGN KEY (parent_id)
            REFERENCES comment_service.comments (id)
            ON DELETE CASCADE
);

CREATE INDEX idx_comments_post_id ON comment_service.comments (post_id);
CREATE INDEX idx_comments_user_id ON comment_service.comments (user_id);
CREATE INDEX idx_comments_parent_id ON comment_service.comments (parent_id);
CREATE INDEX idx_comments_created_at ON comment_service.comments (created_at);