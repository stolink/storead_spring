package com.stolink.backend.domain.draft.controller;

import com.stolink.backend.domain.draft.repository.DraftRepository;
import com.stolink.backend.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftRepository draftRepository;

    /**
     * Draft 삭제 API
     * 게시 취소 시 storead 프론트엔드에서 호출
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@PathVariable UUID id) {
        int deleted = draftRepository.deleteByIdAndReturnCount(id);
        if (deleted == 0) {
            throw new ResourceNotFoundException("Draft를 찾을 수 없습니다: " + id);
        }
    }
}
