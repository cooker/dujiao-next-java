package com.dujiao.api.web.upstream;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.web.ApiPaths;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1)
public class UpstreamCallbackController {

    @PostMapping("/upstream/callback")
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleCallback() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("received", true)));
    }
}
