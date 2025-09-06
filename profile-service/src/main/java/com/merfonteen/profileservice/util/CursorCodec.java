package com.merfonteen.profileservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.merfonteen.profileservice.model.cursors.CommentCursor;
import com.merfonteen.profileservice.model.cursors.PostCursor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class CursorCodec {
    private final ObjectMapper objectMapper;

    public String encodePostCursor(Instant createdAt, long id) {
        return encode(new PostCursor(createdAt, id));
    }

    public String encodeCommentCursor(Instant createdAt, long id) {
        return encode(new CommentCursor(createdAt, id));
    }

    public Optional<PostCursor> decodePostCursor(String encodedCursor) {
        return decode(encodedCursor, PostCursor.class);
    }

    public Optional<CommentCursor> decodeCommentCursor(String encodedCursor) {
        return decode(encodedCursor, CommentCursor.class);
    }

    private String encode(Object obj) {
        try {
            String json = objectMapper.writeValueAsString(obj);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to encode cursor", ex);
        }
    }

    private <T> Optional<T> decode(String encodedCursor, Class<T> type) {
        if (encodedCursor == null || encodedCursor.isEmpty()) {
            return Optional.empty();
        }
        try {
            byte[] raw = Base64.getDecoder().decode(encodedCursor);
            return Optional.of(objectMapper.readValue(new String(raw, StandardCharsets.UTF_8), type));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Bad cursor", ex);
        }
    }
}
