package com.stolink.backend.domain.comment.dto;

import java.util.UUID;

public record ReplyCountDto(UUID parentId, Long count) {
}
