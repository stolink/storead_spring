package com.stolink.backend.domain.draft.service;

import com.stolink.backend.domain.draft.dto.DraftResponse;
import com.stolink.backend.domain.draft.entity.Draft;
import com.stolink.backend.domain.draft.exception.DraftExpiredException;
import com.stolink.backend.domain.draft.repository.DraftRepository;
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

    public DraftResponse findById(UUID id) {
        Draft draft = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Draft", "id", id));

        if (draft.isExpired()) {
            throw new DraftExpiredException(id);
        }

        return DraftResponse.from(draft);
    }

    public Draft findEntityById(UUID id) {
        Draft draft = draftRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Draft", "id", id));

        if (draft.isExpired()) {
            throw new DraftExpiredException(id);
        }

        return draft;
    }

    @Transactional
    public void deleteById(UUID id) {
        int deleted = draftRepository.deleteByIdAndReturnCount(id);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Draft", "id", id);
        }
    }
}
