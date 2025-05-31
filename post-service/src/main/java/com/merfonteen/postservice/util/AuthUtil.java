package com.merfonteen.postservice.util;

import com.merfonteen.exceptions.ForbiddenException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public final class AuthUtil {

    private AuthUtil() {

    }

    public static void requireSelfAccess(Long targetId, Long currentId) {
        if(!Objects.equals(targetId, currentId)) {
            log.warn("Unauthorized attempt to update post {} by user {}", targetId, currentId);
            throw new ForbiddenException("You are not allowed to modify this post");
        }
    }
}
