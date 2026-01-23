package com.stolink.backend.domain.chapter.event;

import com.stolink.backend.domain.draft.service.DocumentPublishService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChapterEventListener {

    private final DocumentPublishService documentPublishService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChapterDeletedEvent(ChapterDeletedEvent event) {
        if (event.getDocumentIds() == null || event.getDocumentIds().isEmpty()) {
            return;
        }

        log.info("[ChapterEventListener] Handling ChapterDeletedEvent: documentIds={}", event.getDocumentIds());
        try {
            documentPublishService.markAsUnpublished(event.getDocumentIds());
            log.info("[ChapterEventListener] Stolink documents unpublished: count={}", event.getDocumentIds().size());
        } catch (Exception e) {
            log.warn("[ChapterEventListener] Failed to sync Stolink document status: {}", e.getMessage());
        }
    }
}
