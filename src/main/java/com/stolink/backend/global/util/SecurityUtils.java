package com.stolink.backend.global.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.UUID;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SecurityUtils {

    /**
     * @AuthenticationPrincipal Object principal에서 UUID 추출
     *                          - UUID 타입 직접 전달 시
     *                          - UserDetails 구현체 (username이 UUID인 경우)
     */
    public static UUID extractUserId(Object principal) {
        if (principal == null) {
            return null;
        }

        // 1. UUID 직접 전달
        if (principal instanceof UUID) {
            return (UUID) principal;
        }

        // 2. UserDetails 구현체 (Spring Security)
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            try {
                return UUID.fromString(username);
            } catch (IllegalArgumentException e) {
                // username이 UUID 형식이 아닌 경우
                return null;
            }
        }

        // 3. String (경우에 따라 String으로 전달될 수도 있음)
        if (principal instanceof String) {
            try {
                return UUID.fromString((String) principal);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }

        return null;
    }
}
