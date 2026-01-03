package com.stolink.backend.domain.draft.controller;

import com.stolink.backend.domain.draft.dto.DraftResponse;
import com.stolink.backend.domain.draft.service.DraftService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/drafts")
@RequiredArgsConstructor
public class DraftController {

    private final DraftService draftService;

    /**
     * Draft 조회 API
     * Stolink에서 생성한 Draft 데이터 조회
     */
    @GetMapping("/{id}")
    public ApiResponse<DraftResponse> getDraft(@PathVariable UUID id) {
        return ApiResponse.ok(draftService.findById(id));
    }

    /**
     * Draft 삭제 API
     * 게시 완료 또는 취소 시 호출
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDraft(@PathVariable UUID id) {
        draftService.deleteById(id);
    }
}
