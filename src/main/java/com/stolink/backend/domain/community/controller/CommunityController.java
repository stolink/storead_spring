package com.stolink.backend.domain.community.controller;

import com.stolink.backend.domain.community.dto.CommunityPublishRequest;
import com.stolink.backend.domain.community.dto.CommunityPublishResponse;
import com.stolink.backend.domain.community.service.CommunityService;
import com.stolink.backend.global.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;

    /**
     * 커뮤니티 게시 API
     * Draft 데이터를 기반으로 Work(없으면 생성) + Chapter 생성
     */
    @PostMapping("/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CommunityPublishResponse> publish(
            @Valid @RequestBody CommunityPublishRequest request) {
        return ApiResponse.created(communityService.publish(request));
    }
}
