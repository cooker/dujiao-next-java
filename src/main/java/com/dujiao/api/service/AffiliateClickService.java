package com.dujiao.api.service;

import com.dujiao.api.domain.AffiliateClickEntity;
import com.dujiao.api.domain.AffiliateProfileEntity;
import com.dujiao.api.repository.AffiliateClickRepository;
import com.dujiao.api.repository.AffiliateProfileRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 推广点击记录与访客归因（与 Go {@code TrackClick} / {@code GetLatestActiveProfileByVisitorKey} 对齐）。
 */
@Service
public class AffiliateClickService {

    private static final String PROFILE_ACTIVE = "active";

    /** 与 Go {@code affiliateAttributionWindow} 一致：30 天。 */
    private static final int ATTRIBUTION_DAYS = 30;

    /** 与 Go {@code affiliateClickDedupeWindow} 一致：10 分钟。 */
    private static final int DEDUPE_MINUTES = 10;

    private final SettingsService settingsService;
    private final AffiliateProfileRepository affiliateProfileRepository;
    private final AffiliateClickRepository affiliateClickRepository;

    public AffiliateClickService(
            SettingsService settingsService,
            AffiliateProfileRepository affiliateProfileRepository,
            AffiliateClickRepository affiliateClickRepository) {
        this.settingsService = settingsService;
        this.affiliateProfileRepository = affiliateProfileRepository;
        this.affiliateClickRepository = affiliateClickRepository;
    }

    /**
     * 记录一次推广点击；非空 {@code visitor_key} 时 10 分钟内同 profile+visitor（及非空时的落地路径）去重。
     */
    @Transactional
    public void trackClick(
            String rawAffiliateCode,
            String rawVisitorKey,
            String landingPath,
            String referrer,
            String clientIp,
            String userAgent) {
        if (!settingsService.affiliateEnabled()) {
            return;
        }
        String code = normalizeAffiliateCode(rawAffiliateCode);
        if (code.isEmpty()) {
            return;
        }
        Optional<AffiliateProfileEntity> profOpt = affiliateProfileRepository.findByAffiliateCode(code);
        if (profOpt.isEmpty() || !PROFILE_ACTIVE.equals(profOpt.get().getStatus())) {
            return;
        }
        AffiliateProfileEntity profile = profOpt.get();
        String vk = rawVisitorKey == null ? "" : rawVisitorKey.trim();
        String lp = landingPath == null ? "" : landingPath.trim();
        Instant sinceDedupe = Instant.now().minus(DEDUPE_MINUTES, ChronoUnit.MINUTES);
        if (!vk.isEmpty()) {
            boolean dup =
                    lp.isEmpty()
                            ? affiliateClickRepository.existsByAffiliateProfileIdAndVisitorKeyAndCreatedAtAfter(
                                    profile.getId(), vk, sinceDedupe)
                            : affiliateClickRepository
                                    .existsByAffiliateProfileIdAndVisitorKeyAndLandingPathAndCreatedAtAfter(
                                            profile.getId(), vk, lp, sinceDedupe);
            if (dup) {
                return;
            }
        }
        AffiliateClickEntity e = new AffiliateClickEntity();
        e.setAffiliateProfileId(profile.getId());
        e.setVisitorKey(vk);
        e.setLandingPath(lp);
        e.setReferrer(trimToLength(referrer, 1024));
        e.setClientIp(trimToLength(clientIp, 64));
        e.setUserAgent(trimToLength(userAgent, 1024));
        affiliateClickRepository.save(e);
    }

    /**
     * 与 Go {@code ResolveOrderAffiliateSnapshot} 中 visitor 分支一致：30 天内最近一次点击对应的 active profile。
     */
    @Transactional(readOnly = true)
    public Optional<AffiliateProfileEntity> resolveLatestActiveProfileForOrder(
            String visitorKey, Long buyerUserId) {
        if (!settingsService.affiliateEnabled()) {
            return Optional.empty();
        }
        if (visitorKey == null || visitorKey.isBlank()) {
            return Optional.empty();
        }
        Instant since = Instant.now().minus(ATTRIBUTION_DAYS, ChronoUnit.DAYS);
        var page =
                affiliateClickRepository.findLatestForVisitor(
                        visitorKey.trim(), since, PageRequest.of(0, 1));
        if (page.isEmpty()) {
            return Optional.empty();
        }
        AffiliateClickEntity click = page.getContent().getFirst();
        Optional<AffiliateProfileEntity> prof = affiliateProfileRepository.findById(click.getAffiliateProfileId());
        if (prof.isEmpty() || !PROFILE_ACTIVE.equals(prof.get().getStatus())) {
            return Optional.empty();
        }
        AffiliateProfileEntity p = prof.get();
        if (buyerUserId != null && buyerUserId > 0 && p.getUserId() == buyerUserId) {
            return Optional.empty();
        }
        return Optional.of(p);
    }

    @Transactional(readOnly = true)
    public long countClicksByProfileId(long affiliateProfileId) {
        return affiliateClickRepository.countByAffiliateProfileId(affiliateProfileId);
    }

    private static String normalizeAffiliateCode(String raw) {
        if (raw == null) {
            return "";
        }
        String code = raw.trim();
        if (code.isEmpty()) {
            return "";
        }
        if (code.length() > 32) {
            return code.substring(0, 32);
        }
        return code;
    }

    private static String trimToLength(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        if (t.length() <= maxLen) {
            return t;
        }
        return t.substring(0, maxLen);
    }
}
