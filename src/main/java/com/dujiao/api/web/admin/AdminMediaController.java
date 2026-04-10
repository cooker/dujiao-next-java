package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.media.MediaDto;
import com.dujiao.api.dto.media.MediaUpdateRequest;
import com.dujiao.api.security.SecurityUtils;
import com.dujiao.api.service.MediaService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/media")
public class AdminMediaController {

    private final MediaService mediaService;

    public AdminMediaController(MediaService mediaService) {
        this.mediaService = mediaService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(required = false) String scene,
            @RequestParam(required = false) String search) {
        SecurityUtils.requireAdminId();
        MediaService.MediaListResult result = mediaService.listForAdmin(scene, search, page, pageSize);
        return ResponseEntity.ok(
                ApiResponse.success(
                        Map.of(
                                "items", result.items(),
                                "total", result.total())));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<MediaDto>> update(
            @PathVariable String id, @Valid @RequestBody MediaUpdateRequest req) {
        SecurityUtils.requireAdminId();
        return ResponseEntity.ok(
                ApiResponse.success(mediaService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        SecurityUtils.requireAdminId();
        mediaService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
