package com.stolink.backend.domain.file.controller;

import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageUtil fileStorageUtil;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp");

    @PostMapping
    public ApiResponse<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "common") String type) {

        if (file.isEmpty()) {
            return ApiResponse.error("파일이 비어있습니다");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ApiResponse.error("파일 크기가 너무 큽니다 (최대 10MB)");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !isValidExtension(originalFilename)) {
            return ApiResponse.error("허용되지 않는 파일 확장자입니다 (jpg, jpeg, png, webp 가능)");
        }

        log.info("File upload request: name={}, size={}, type={}",
                originalFilename, file.getSize(), type);

        // FileStorageUtil을 사용하여 파일 저장
        // subDirectory는 type에 따라 결정 (예: covers, profiles 등)
        String subDirectory = type.equals("cover") ? "covers" : "uploads";
        String filePath = fileStorageUtil.store(file, subDirectory);

        // 클라이언트에게 반환할 파일 경로 (정적 리소스 핸들러 설정 필요)
        return ApiResponse.ok(Map.of("url", filePath));
    }

    private boolean isValidExtension(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(extension);
    }
}
