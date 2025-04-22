package com.merfonteen.userservice.util;

import com.merfonteen.userservice.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public final class AuthUtil {

    private AuthUtil() {

    }

    public static void requireSelfAccess(Long targetId, Long currentId) {
        if(!Objects.equals(targetId, currentId)) {
            log.warn("Unauthorized attempt to update user {} by user {}", targetId, currentId);
            throw new ForbiddenException("You are not allowed to modify this user");
        }
    }
}
