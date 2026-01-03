package com.stolink.backend.domain.draft.exception;

import java.util.UUID;

public class DraftExpiredException extends RuntimeException {

    public DraftExpiredException(UUID id) {
        super("Draft가 만료되었습니다: " + id);
    }
}
