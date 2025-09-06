package com.merfonteen.profileservice.model.cursors;

import java.time.Instant;

public record CommentCursor(Instant createdAt, long id) {
}
