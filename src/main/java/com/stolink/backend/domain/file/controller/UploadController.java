package com.stolink.backend.domain.file.controller;

import com.stolink.backend.global.common.dto.ApiResponse;
import com.stolink.backend.global.util.FileStorageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageUtil fileStorageUtil;

    @PostMapping
    public ApiResponse<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", defaultValue = "common") String type) {

        log.info("File upload request: name={}, size={}, type={}",
                file.getOriginalFilename(), file.getSize(), type);

        // FileStorageUtil을 사용하여 파일 저장
        // subDirectory는 type에 따라 결정 (예: covers, profiles 등)
        String subDirectory = type.equals("cover") ? "covers" : "uploads";
        String filePath = fileStorageUtil.store(file, subDirectory);

        // 클라이언트에게 반환할 파일 경로 (정적 리소스 핸들러 설정 필요)
        return ApiResponse.ok(Map.of("url", filePath));
    }
}
