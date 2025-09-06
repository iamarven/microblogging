package com.merfonteen.profileservice.model.cursors;

import java.time.Instant;

public record PostCursor(Instant createdAt, long id) {
}
