package com.dujiao.api.web.publicapi;

import com.dujiao.api.common.api.ApiResponse;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.dto.publicapi.PublicAffiliateClickRequest;
import com.dujiao.api.service.AffiliateClickService;
import com.dujiao.api.service.CaptchaImageService;
import com.dujiao.api.service.PublicService;
import com.dujiao.api.service.SettingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(com.dujiao.api.web.ApiPaths.V1 + "/public")
public class PublicApiController {

    private final PublicService publicService;
    private final AffiliateClickService affiliateClickService;
    private final SettingsService settingsService;
    private final CaptchaImageService captchaImageService;

    public PublicApiController(
            PublicService publicService,
            AffiliateClickService affiliateClickService,
            SettingsService settingsService,
            CaptchaImageService captchaImageService) {
        this.publicService = publicService;
        this.affiliateClickService = affiliateClickService;
        this.settingsService = settingsService;
        this.captchaImageService = captchaImageService;
    }

    @GetMapping("/config")
    public ResponseEntity<ApiResponse<Map<String, Object>>> config() {
        return ResponseEntity.ok(ApiResponse.success(publicService.getConfig()));
    }

    @GetMapping("/products")
    public ResponseEntity<PageResponse<List<com.dujiao.api.dto.product.ProductDto>>> products(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize) {
        Page<com.dujiao.api.dto.product.ProductDto> p = publicService.listProducts(page, pageSize);
        PaginationDto pg = publicService.pagination(p);
        return ResponseEntity.ok(PageResponse.success(p.getContent(), pg));
    }

    @GetMapping("/products/{slug}")
    public ResponseEntity<ApiResponse<com.dujiao.api.dto.product.ProductDto>> productBySlug(
            @PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(publicService.getProductBySlug(slug)));
    }

    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> posts() {
        return ResponseEntity.ok(ApiResponse.success(publicService.listPublicPosts()));
    }

    @GetMapping("/posts/{slug}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> postBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(publicService.getPublicPost(slug)));
    }

    @GetMapping("/banners")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> banners() {
        return ResponseEntity.ok(ApiResponse.success(publicService.listPublicBanners()));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<com.dujiao.api.dto.category.CategoryDto>>> categories() {
        return ResponseEntity.ok(ApiResponse.success(publicService.listCategories()));
    }

    @GetMapping("/captcha/image")
    public ResponseEntity<ApiResponse<Map<String, Object>>> captchaImage() {
        return ResponseEntity.ok(
                ApiResponse.success(
                        captchaImageService.generate(
                                settingsService.captchaEnabled(),
                                settingsService.captchaImageExpireSeconds())));
    }

    /** 与 Go {@code TrackAffiliateClick} 一致：写入 {@code affiliate_clicks}（10 分钟内同访客+落地路径去重）。 */
    @PostMapping("/affiliate/click")
    public ResponseEntity<ApiResponse<Map<String, Object>>> affiliateClick(
            HttpServletRequest http, @Valid @RequestBody PublicAffiliateClickRequest req) {
        affiliateClickService.trackClick(
                req.affiliateCode(),
                req.visitorKey(),
                req.landingPath(),
                req.referrer(),
                clientIp(http),
                http.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", true)));
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String ra = req.getRemoteAddr();
        return ra == null ? "" : ra;
    }

    @GetMapping("/member-levels")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> memberLevels() {
        return ResponseEntity.ok(ApiResponse.success(publicService.listPublicMemberLevels()));
    }
}
