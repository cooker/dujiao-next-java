package com.dujiao.api.web.admin;

import com.dujiao.api.web.ApiPaths;
import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.dto.memberlevel.MemberLevelDto;
import com.dujiao.api.dto.memberlevel.MemberLevelPriceBatchRequest;
import com.dujiao.api.dto.memberlevel.MemberLevelPriceDto;
import com.dujiao.api.dto.memberlevel.MemberLevelUpsertRequest;
import com.dujiao.api.service.AdminMemberLevelService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/admin")
public class AdminMemberLevelController {

    private final AdminMemberLevelService adminMemberLevelService;

    public AdminMemberLevelController(AdminMemberLevelService adminMemberLevelService) {
        this.adminMemberLevelService = adminMemberLevelService;
    }

    @GetMapping("/member-levels")
    public ResponseEntity<ApiResponse<List<MemberLevelDto>>> listMemberLevels() {
        return ResponseEntity.ok(ApiResponse.success(adminMemberLevelService.list()));
    }

    @GetMapping("/member-levels/{id}")
    public ResponseEntity<ApiResponse<MemberLevelDto>> getMemberLevel(@PathVariable String id) {
        return ResponseEntity.ok(
                ApiResponse.success(adminMemberLevelService.get(Long.parseLong(id))));
    }

    @PostMapping("/member-levels")
    public ResponseEntity<ApiResponse<MemberLevelDto>> createMemberLevel(
            @Valid @RequestBody MemberLevelUpsertRequest req) {
        return ResponseEntity.ok(ApiResponse.success(adminMemberLevelService.create(req)));
    }

    @PutMapping("/member-levels/{id}")
    public ResponseEntity<ApiResponse<MemberLevelDto>> updateMemberLevel(
            @PathVariable String id, @Valid @RequestBody MemberLevelUpsertRequest req) {
        return ResponseEntity.ok(
                ApiResponse.success(adminMemberLevelService.update(Long.parseLong(id), req)));
    }

    @DeleteMapping("/member-levels/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMemberLevel(@PathVariable String id) {
        adminMemberLevelService.delete(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/member-level-prices")
    public ResponseEntity<ApiResponse<List<MemberLevelPriceDto>>> memberLevelPrices() {
        return ResponseEntity.ok(ApiResponse.success(adminMemberLevelService.listPrices()));
    }

    @PostMapping("/member-level-prices/batch")
    public ResponseEntity<ApiResponse<Void>> batchUpsertPrices(
            @Valid @RequestBody MemberLevelPriceBatchRequest req) {
        adminMemberLevelService.batchUpsertPrices(req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/member-level-prices/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMemberLevelPrice(@PathVariable String id) {
        adminMemberLevelService.deletePrice(Long.parseLong(id));
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/member-levels/backfill")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> backfill() {
        int n = adminMemberLevelService.backfillDefaultLevel();
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", n)));
    }
}
