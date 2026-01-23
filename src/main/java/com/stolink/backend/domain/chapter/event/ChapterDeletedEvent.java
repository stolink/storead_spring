package com.stolink.backend.domain.chapter.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ChapterDeletedEvent {
    private final List<String> documentIds;
}
