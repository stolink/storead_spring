package com.stolink.backend.domain.chapter.entity.projection;

import java.util.List;
import java.util.UUID;

public interface ChapterIdProjection {
    UUID getId();

    String getDocumentId();

    List<String> getDocumentIds();
}
