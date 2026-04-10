package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.media.MediaDto;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.MediaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class AdminUploadController {

    private final MediaService mediaService;

    public AdminUploadController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<MediaDto>> upload(@RequestParam("file") MultipartFile file) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(ApiResponse.success(mediaService.upload(file)));
    }
}
