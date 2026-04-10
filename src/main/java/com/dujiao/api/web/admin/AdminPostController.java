package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.dto.post.PostDto;
import com.dujiao.api.dto.post.PostUpsertRequest;
import com.dujiao.api.service.AdminPostService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin/posts")
public class AdminPostController {

    private final AdminPostService adminPostService;

    public AdminPostController(AdminPostService adminPostService) {
        this.adminPostService = adminPostService;
    }

    /** 与 Go {@code GetAdminPosts}：{@code page},{@code page_size},{@code type},{@code search}。 */
    @GetMapping
    public ResponseEntity<PageResponse<List<PostDto>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(required = false) String search) {
        Page<PostDto> p = adminPostService.list(page, pageSize, type, search);
        PaginationDto pg =
                PaginationDto.of(
                        p.getNumber() + 1, p.getSize(), p.getTotalElements());
        return ResponseEntity.ok(PageResponse.success(p.getContent(), pg));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(adminPostService.get(Long.parseLong(id))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PostDto>> create(@Valid @RequestBody PostUpsertRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminPostService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PostDto>> update(
            @PathVariable String id, @Valid @RequestBody PostUpsertRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(adminPostService.update(Long.parseLong(id), req)));
    }

    /** 与 Go {@code DeletePost} 一致：软删除，成功体为 {@code null}。 */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        adminPostService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
