package com.stolink.backend.domain.draft.service;

import com.stolink.backend.domain.draft.dto.DraftResponse;
import com.stolink.backend.domain.draft.entity.Draft;
import com.stolink.backend.domain.draft.exception.DraftExpiredException;
import com.stolink.backend.domain.draft.repository.DraftRepository;
import com.stolink.backend.global.common.exception.AccessDeniedException;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DraftService {

    private final DraftRepository draftRepository;

    public DraftResponse findById(UUID id, UUID userId) {
        Draft draft = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Draft", "id", id));

        validateOwnership(draft, userId);

        if (draft.isExpired()) {
            throw new DraftExpiredException(id);
        }

        return DraftResponse.from(draft);
    }

    public Draft findEntityById(UUID id, UUID userId) {
        Draft draft = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Draft", "id", id));

        validateOwnership(draft, userId);

        if (draft.isExpired()) {
            throw new DraftExpiredException(id);
        }

        return draft;
    }

    @Transactional
    public void deleteById(UUID id, UUID userId) {
        Draft draft = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Draft", "id", id));

        validateOwnership(draft, userId);

        draftRepository.delete(draft);
    }

    private void validateOwnership(Draft draft, UUID userId) {
        if (!draft.getUserId().equals(userId)) {
            throw new AccessDeniedException("이 Draft를 조작할 권한이 없습니다.");
        }
    }
}
