package com.dujiao.api.config;

import com.dujiao.api.domain.AdminAccount;
import com.dujiao.api.domain.BannerEntity;
import com.dujiao.api.util.BannerI18nJson;
import com.dujiao.api.util.PostJsonMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dujiao.api.domain.Category;
import com.dujiao.api.domain.MemberLevelEntity;
import com.dujiao.api.domain.PostEntity;
import java.time.Instant;
import com.dujiao.api.domain.PaymentChannelEntity;
import com.dujiao.api.domain.Product;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.repository.AdminAccountRepository;
import com.dujiao.api.repository.BannerRepository;
import com.dujiao.api.repository.CategoryRepository;
import com.dujiao.api.repository.MemberLevelRepository;
import com.dujiao.api.repository.PaymentChannelRepository;
import com.dujiao.api.repository.PostRepository;
import com.dujiao.api.repository.ProductRepository;
import com.dujiao.api.repository.SiteSettingRepository;
import com.dujiao.api.repository.UserAccountRepository;
import com.dujiao.api.service.SettingsService;
import com.dujiao.api.service.WalletBootstrapService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seed(
            AdminAccountRepository adminAccountRepository,
            UserAccountRepository userAccountRepository,
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            PaymentChannelRepository paymentChannelRepository,
            MemberLevelRepository memberLevelRepository,
            BannerRepository bannerRepository,
            PostRepository postRepository,
            SiteSettingRepository siteSettingRepository,
            SettingsService settingsService,
            WalletBootstrapService walletBootstrapService,
            PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminAccountRepository.count() == 0) {
                AdminAccount a = new AdminAccount();
                a.setUsername("admin");
                a.setPasswordHash(passwordEncoder.encode("admin123"));
                a.setSuperAdmin(true);
                adminAccountRepository.save(a);
            }
            if (memberLevelRepository.count() == 0) {
                MemberLevelEntity ml = new MemberLevelEntity();
                ml.setSlug("normal");
                ml.setName("普通会员");
                ml.setDiscountRate(new BigDecimal("100"));
                ml.setRechargeThreshold(BigDecimal.ZERO);
                ml.setSpendThreshold(BigDecimal.ZERO);
                ml.setDefaultLevel(true);
                ml.setSortOrder(0);
                ml.setActive(true);
                memberLevelRepository.save(ml);
            }
            if (siteSettingRepository.findById("settings").isEmpty()) {
                Map<String, Object> site = new HashMap<>();
                site.put("site_name", "Dujiao-Next");
                site.put("locale_default", "zh-CN");
                settingsService.putSettingsMap(site);
            }
            if (userAccountRepository.count() == 0) {
                Long levelId =
                        memberLevelRepository
                                .findByDefaultLevelTrue()
                                .map(MemberLevelEntity::getId)
                                .orElse(null);
                UserAccount u = new UserAccount();
                u.setEmail("demo@example.com");
                u.setPasswordHash(passwordEncoder.encode("password123"));
                u.setDisplayName("Demo");
                u.setStatus("active");
                u.setMemberLevelId(levelId);
                u = userAccountRepository.save(u);
                walletBootstrapService.ensureWallet(u.getId());
            }
            if (categoryRepository.count() == 0) {
                Category c = new Category();
                c.setName("默认分类");
                c.setSlug("default");
                c.setSortOrder(0);
                c = categoryRepository.save(c);
                Product p = new Product();
                p.setCategoryId(c.getId());
                p.setSlug("sample-product");
                p.setTitle("{\"zh-CN\":\"示例商品\"}");
                p.setSeoMetaJson("{}");
                p.setDescriptionJson("{}");
                p.setContentJson("{}");
                p.setManualFormSchemaJson("{}");
                p.setPriceAmount(new BigDecimal("9.99"));
                p.setCostPriceAmount(BigDecimal.ZERO);
                p.setImagesJson("[]");
                p.setTagsJson("[]");
                p.setPurchaseType("member");
                p.setFulfillmentType("manual");
                p.setManualStockTotal(0);
                p.setManualStockLocked(0);
                p.setManualStockSold(0);
                p.setPaymentChannelIds("");
                p.setMapped(false);
                p.setActive(true);
                p.setSortOrder(0);
                p.setAffiliateEnabled(true);
                productRepository.save(p);
            }
            if (paymentChannelRepository.count() == 0) {
                PaymentChannelEntity ch = new PaymentChannelEntity();
                ch.setName("演示在线支付");
                ch.setChannelType("alipay");
                ch.setProviderType("manual");
                ch.setInteractionMode("redirect");
                ch.setConfigJson("{}");
                ch.setActive(true);
                paymentChannelRepository.save(ch);
            }
            if (bannerRepository.count() == 0) {
                ObjectNode title = new ObjectMapper().createObjectNode();
                title.put("zh-CN", "欢迎使用独角数卡");
                BannerEntity b = new BannerEntity();
                b.setName("首页横幅");
                b.setPosition("home_hero");
                b.setTitleJson(BannerI18nJson.normalizeToStoredJson(title));
                b.setSubtitleJson(BannerI18nJson.normalizeToStoredJson(null));
                b.setImage("/placeholder.png");
                b.setLinkType("none");
                b.setActive(true);
                b.setSortOrder(0);
                bannerRepository.save(b);
            }
            if (postRepository.count() == 0) {
                ObjectMapper om = new ObjectMapper();
                ObjectNode title = om.createObjectNode();
                title.put("zh-CN", "欢迎");
                ObjectNode summary = om.createObjectNode();
                summary.put("zh-CN", "站点说明");
                ObjectNode content = om.createObjectNode();
                content.put("zh-CN", "<p>欢迎使用 Dujiao-Next（Java 版）。</p>");
                PostEntity post = new PostEntity();
                post.setSlug("welcome");
                post.setType("blog");
                post.setTitleJson(PostJsonMapper.toStoredJson(title));
                post.setSummaryJson(PostJsonMapper.toStoredJson(summary));
                post.setContentJson(PostJsonMapper.toStoredJson(content));
                post.setPublished(true);
                post.setPublishedAt(Instant.now());
                postRepository.save(post);
            }
        };
    }
}
