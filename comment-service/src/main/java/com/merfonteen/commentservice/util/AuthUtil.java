package com.merfonteen.commentservice.util;

import com.merfonteen.exceptions.ForbiddenException;
import org.springframework.stereotype.Component;

public class AuthUtil {

    private AuthUtil() {}

    public static void validateChangingComment(Long currentUserId, Long commentUserId) {
        if(!commentUserId.equals(currentUserId)) {
            throw new ForbiddenException("You cannot update not your own comment");
        }
    }
}
