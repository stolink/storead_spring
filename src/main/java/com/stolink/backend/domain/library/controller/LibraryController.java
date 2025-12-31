package com.stolink.backend.domain.library.controller;

import com.stolink.backend.domain.library.dto.LibraryResponse;
import com.stolink.backend.domain.library.service.LibraryService;
import com.stolink.backend.global.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/library")
@RequiredArgsConstructor
public class LibraryController {

    private final LibraryService libraryService;

    @GetMapping
    public ApiResponse<Map<String, Object>> getLibrary(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<LibraryResponse> library = libraryService.getLibrary(userId, pageable);

        return ApiResponse.ok(Map.of(
                "items", library.getContent(),
                "pagination", Map.of(
                        "page", page,
                        "size", size,
                        "total", library.getTotalElements(),
                        "totalPages", library.getTotalPages(),
                        "hasNext", library.hasNext()
                )
        ));
    }

    @PostMapping("/{workId}")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LibraryResponse> addToLibrary(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID workId) {
        LibraryResponse response = libraryService.addToLibrary(userId, workId);
        return ApiResponse.created(response);
    }

    @DeleteMapping("/{workId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeFromLibrary(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID workId) {
        libraryService.removeFromLibrary(userId, workId);
    }

    @GetMapping("/{workId}/status")
    public ApiResponse<Map<String, Boolean>> checkLibraryStatus(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID workId) {
        boolean inLibrary = libraryService.isInLibrary(userId, workId);
        return ApiResponse.ok(Map.of("inLibrary", inLibrary));
    }
}
