package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.BannerEntity;
import com.dujiao.api.util.BannerI18nJson;
import com.dujiao.api.util.PostJsonMapper;
import com.dujiao.api.domain.Category;
import com.dujiao.api.domain.MemberLevelEntity;
import com.dujiao.api.domain.PostEntity;
import com.dujiao.api.domain.Product;
import com.dujiao.api.dto.payment.PaymentChannelOptionDto;
import com.dujiao.api.dto.product.ProductDto;
import com.dujiao.api.repository.BannerRepository;
import com.dujiao.api.repository.CategoryRepository;
import com.dujiao.api.repository.MemberLevelRepository;
import com.dujiao.api.repository.PostRepository;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.util.CategoryDtoMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PublicService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SettingsService settingsService;
    private final BannerRepository bannerRepository;
    private final PostRepository postRepository;
    private final MemberLevelRepository memberLevelRepository;
    private final PaymentChannelQueryService paymentChannelQueryService;
    private final ObjectMapper objectMapper;
    private final PublicProductAssembly publicProductAssembly;

    @Value("${dujiao.app.version:0.1.0-SNAPSHOT}")
    private String appVersion;

    @Value("${spring.mail.host:}")
    private String springMailHost;

    public PublicService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            SettingsService settingsService,
            BannerRepository bannerRepository,
            PostRepository postRepository,
            MemberLevelRepository memberLevelRepository,
            PaymentChannelQueryService paymentChannelQueryService,
            ObjectMapper objectMapper,
            PublicProductAssembly publicProductAssembly) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.settingsService = settingsService;
        this.bannerRepository = bannerRepository;
        this.postRepository = postRepository;
        this.memberLevelRepository = memberLevelRepository;
        this.paymentChannelQueryService = paymentChannelQueryService;
        this.objectMapper = objectMapper;
        this.publicProductAssembly = publicProductAssembly;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConfig() {
        Map<String, Object> data = new HashMap<>(settingsService.getSettingsMap());
        data.putIfAbsent("site_name", "Dujiao-Next");
        data.putIfAbsent("locale_default", "zh-CN");
        data.putIfAbsent("currency", "CNY");
        data.put("registration_enabled", settingsService.registrationEnabled());
        data.put("email_verification_enabled", settingsService.emailVerificationEnabled());
        data.put("telegram_auth", settingsService.telegramAuthPublicMap());
        data.put("payment_channels", paymentChannelsForPublic());
        data.put("smtp_enabled", springMailHost != null && !springMailHost.isBlank());
        data.put("languages", List.of("zh-CN", "en-US", "zh-TW"));
        data.put(
                "contact",
                Map.of(
                        "telegram", "https://t.me/dujiaoka",
                        "whatsapp", "https://wa.me/1234567890"));
        data.put("scripts", List.of());
        data.put("affiliate", affiliatePublicMap());
        data.put("nav_config", navConfigOrDefault());
        data.put("server_time", System.currentTimeMillis());
        data.put("app_version", appVersion);
        List<Long> walletChannelIds = settingsService.walletRechargeChannelIds();
        if (!walletChannelIds.isEmpty()) {
            data.put("wallet_recharge_channel_ids", walletChannelIds);
        }
        if (settingsService.walletOnlyPayment()) {
            data.put("wallet_only_payment", true);
        }
        return data;
    }

    private List<Map<String, Object>> paymentChannelsForPublic() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (PaymentChannelOptionDto d : paymentChannelQueryService.listActive()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.id());
            m.put("name", d.name());
            m.put("channel_type", d.channelType());
            list.add(m);
        }
        return list;
    }

    private Map<String, Object> affiliatePublicMap() {
        JsonNode n = settingsService.getJson("affiliate");
        if (n != null && n.isObject() && !n.isEmpty()) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = objectMapper.convertValue(n, Map.class);
                return m;
            } catch (IllegalArgumentException e) {
                // fall through
            }
        }
        return Map.of("enabled", false);
    }

    private Map<String, Object> navConfigOrDefault() {
        JsonNode n = settingsService.getJson("nav_config");
        if (n != null && n.isObject() && n.size() > 0) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = objectMapper.convertValue(n, Map.class);
                return m;
            } catch (IllegalArgumentException e) {
                // fall through
            }
        }
        return Map.of(
                "builtin",
                Map.of("blog", true, "notice", true, "about", true),
                "custom_items",
                List.of());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPublicBanners() {
        return bannerRepository.findByActiveTrueOrderBySortOrderDescCreatedAtDesc().stream()
                .map(this::bannerToMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPublicPosts() {
        return postRepository.findPublicPublished().stream().map(this::postToMap).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPublicPost(String slug) {
        PostEntity p =
                postRepository
                        .findBySlugAndPublishedTrue(slug)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "post_not_found"));
        return postToMap(p);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPublicMemberLevels() {
        return memberLevelRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(this::memberLevelToMap)
                .toList();
    }

    /** 与 Go {@code dto.NewBannerResp} 一致（不含后台字段 name/is_active/sort_order 等）。 */
    private Map<String, Object> bannerToMap(BannerEntity b) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", b.getId());
        m.put("position", b.getPosition());
        m.put("title", BannerI18nJson.storedToResponseNode(b.getTitleJson()));
        m.put("subtitle", BannerI18nJson.storedToResponseNode(b.getSubtitleJson()));
        m.put("image", b.getImage());
        String mobile = b.getMobileImage();
        if (mobile != null && !mobile.isBlank()) {
            m.put("mobile_image", mobile);
        }
        m.put("link_type", b.getLinkType());
        String lv = b.getLinkValue();
        if (lv != null && !lv.isBlank()) {
            m.put("link_value", lv);
        }
        m.put("open_in_new_tab", b.isOpenInNewTab());
        return m;
    }

    /** 与 Go {@code dto.NewPostResp} 一致（不含 {@code is_published}）。 */
    private Map<String, Object> postToMap(PostEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("slug", p.getSlug());
        m.put("type", p.getType());
        m.put("title", PostJsonMapper.toResponseNode(p.getTitleJson()));
        m.put("summary", PostJsonMapper.toResponseNode(p.getSummaryJson()));
        m.put("content", PostJsonMapper.toResponseNode(p.getContentJson()));
        String th = p.getThumbnail();
        if (th != null && !th.isBlank()) {
            m.put("thumbnail", th);
        }
        m.put("published_at", p.getPublishedAt());
        return m;
    }

    private Map<String, Object> memberLevelToMap(MemberLevelEntity e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId());
        m.put("slug", e.getSlug());
        m.put("name", e.getName());
        m.put("discount_rate", e.getDiscountRate());
        m.put("recharge_threshold", e.getRechargeThreshold());
        m.put("spend_threshold", e.getSpendThreshold());
        m.put("is_default", e.isDefaultLevel());
        m.put("sort_order", e.getSortOrder());
        m.put("is_active", e.isActive());
        return m;
    }

    @Transactional(readOnly = true)
    public Page<ProductDto> listProducts(int page, int pageSize) {
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), pageSize <= 0 ? 20 : pageSize);
        Page<Product> pg = productRepository.findByActiveTrueOrderBySortOrderAsc(pr);
        Map<Long, Category> categories = loadCategoriesForProducts(pg.getContent());
        return pg.map(
                p -> {
                    Category cat = categories.get(p.getCategoryId());
                    com.dujiao.api.dto.category.CategoryDto cDto =
                            cat != null ? CategoryDtoMapper.from(cat, objectMapper) : null;
                    return publicProductAssembly.buildPublicProductDto(p, cDto);
                });
    }

    @Transactional(readOnly = true)
    public ProductDto getProductBySlug(String slug) {
        Product p =
                productRepository
                        .findBySlug(slug)
                        .filter(Product::isActive)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "product_not_found"));
        com.dujiao.api.dto.category.CategoryDto cDto =
                categoryRepository
                        .findById(p.getCategoryId())
                        .map(c -> CategoryDtoMapper.from(c, objectMapper))
                        .orElse(null);
        return publicProductAssembly.buildPublicProductDto(p, cDto);
    }

    @Transactional(readOnly = true)
    public ProductDto getProductById(long id) {
        Product p =
                productRepository
                        .findById(id)
                        .filter(Product::isActive)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "product_not_found"));
        com.dujiao.api.dto.category.CategoryDto cDto =
                categoryRepository
                        .findById(p.getCategoryId())
                        .map(c -> CategoryDtoMapper.from(c, objectMapper))
                        .orElse(null);
        return publicProductAssembly.buildPublicProductDto(p, cDto);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> listAllActiveProducts() {
        List<Product> list = productRepository.findByActiveTrueOrderBySortOrderAsc();
        Map<Long, Category> categories = loadCategoriesForProducts(list);
        return list.stream()
                .map(
                        p -> {
                            Category cat = categories.get(p.getCategoryId());
                            com.dujiao.api.dto.category.CategoryDto cDto =
                                    cat != null ? CategoryDtoMapper.from(cat, objectMapper) : null;
                            return publicProductAssembly.buildPublicProductDto(p, cDto);
                        })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<com.dujiao.api.dto.category.CategoryDto> listCategories() {
        return categoryRepository.findAllByOrderBySortOrderAsc().stream()
                .map(this::toCategoryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> listChannelCategories(String locale) {
        String loc = (locale == null || locale.isBlank()) ? "zh-CN" : locale.trim();
        String defaultLocale = "zh-CN";
        List<Category> categories = categoryRepository.findAllByOrderBySortOrderAsc();

        Map<Long, Long> directCounts = new HashMap<>();
        Set<Long> hasChildren = new HashSet<>();
        for (Category cat : categories) {
            long count = productRepository.countByCategoryIdAndActiveTrue(cat.getId());
            directCounts.put(cat.getId(), count);
            if (cat.getParentId() > 0) {
                hasChildren.add(cat.getParentId());
            }
        }

        Set<Long> visibleParentIds = new HashSet<>();
        for (Category cat : categories) {
            if (cat.getParentId() == 0) {
                long count = directCounts.getOrDefault(cat.getId(), 0L);
                if (count > 0 || hasChildren.contains(cat.getId())) {
                    visibleParentIds.add(cat.getId());
                }
            }
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (Category cat : categories) {
            if (cat.getParentId() == 0) {
                if (!visibleParentIds.contains(cat.getId())) {
                    continue;
                }
            } else if (!visibleParentIds.contains(cat.getParentId())) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", cat.getId());
            row.put("parent_id", cat.getParentId());
            row.put("name", resolveLocalizedName(cat, loc, defaultLocale));
            row.put("icon", cat.getIcon() == null ? "" : cat.getIcon());
            row.put("slug", cat.getSlug());
            row.put("product_count", directCounts.getOrDefault(cat.getId(), 0L));
            items.add(row);
        }
        return Map.of("items", items);
    }

    public PaginationDto pagination(Page<?> page) {
        return PaginationDto.of(page.getNumber() + 1, page.getSize(), page.getTotalElements());
    }

    private Map<Long, Category> loadCategoriesForProducts(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return Map.of();
        }
        Set<Long> ids =
                products.stream().map(Product::getCategoryId).collect(Collectors.toSet());
        return categoryRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
    }

    private com.dujiao.api.dto.category.CategoryDto toCategoryDto(Category c) {
        return CategoryDtoMapper.from(c, objectMapper);
    }

    private String resolveLocalizedName(Category c, String locale, String defaultLocale) {
        Map<String, Object> m = CategoryDtoMapper.nameMap(c, objectMapper);
        Object v = m.get(locale);
        if (v != null && !String.valueOf(v).isBlank()) {
            return String.valueOf(v).trim();
        }
        v = m.get(defaultLocale);
        if (v != null && !String.valueOf(v).isBlank()) {
            return String.valueOf(v).trim();
        }
        return m.values().stream()
                .filter(x -> x != null && !String.valueOf(x).isBlank())
                .map(x -> String.valueOf(x).trim())
                .findFirst()
                .orElse("");
    }
}
