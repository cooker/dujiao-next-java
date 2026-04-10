package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.BannerEntity;
import com.dujiao.api.dto.banner.BannerDto;
import com.dujiao.api.dto.banner.BannerUpsertRequest;
import com.dujiao.api.repository.BannerRepository;
import com.dujiao.api.util.BannerI18nJson;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminBannerService {

    private final BannerRepository bannerRepository;

    public AdminBannerService(BannerRepository bannerRepository) {
        this.bannerRepository = bannerRepository;
    }

    @Transactional(readOnly = true)
    public List<BannerDto> list() {
        return bannerRepository
                .findAll(Sort.by(Sort.Order.desc("sortOrder"), Sort.Order.desc("createdAt")))
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BannerDto get(long id) {
        return toDto(
                bannerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "banner_not_found")));
    }

    @Transactional
    public BannerDto create(BannerUpsertRequest req) {
        ParsedTimes times = parseTimes(req);
        BannerEntity b = new BannerEntity();
        applyCreate(b, req, times);
        return toDto(bannerRepository.save(b));
    }

    @Transactional
    public BannerDto update(long id, BannerUpsertRequest req) {
        ParsedTimes times = parseTimes(req);
        BannerEntity b =
                bannerRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "banner_not_found"));
        applyUpdate(b, req, times);
        return toDto(bannerRepository.save(b));
    }

    @Transactional
    public void delete(long id) {
        if (!bannerRepository.existsById(id)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "banner_not_found");
        }
        bannerRepository.deleteById(id);
    }

    private void applyCreate(BannerEntity b, BannerUpsertRequest req, ParsedTimes times) {
        String name = trimOrEmpty(req.name());
        String image = trimOrEmpty(req.image());
        if (name.isEmpty() || image.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }
        String linkType = normalizeLinkType(req.linkType());
        if (linkType.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }
        String linkValue = trimOrEmpty(req.linkValue());
        if ("none".equals(linkType)) {
            linkValue = "";
        } else if (linkValue.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }
        if (times.startAt != null && times.endAt != null && times.endAt.isBefore(times.startAt)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }

        b.setName(name);
        b.setPosition(normalizePosition(req.position()));
        b.setTitleJson(BannerI18nJson.normalizeToStoredJson(req.title()));
        b.setSubtitleJson(BannerI18nJson.normalizeToStoredJson(req.subtitle()));
        b.setImage(image);
        b.setMobileImage(trimOrEmpty(req.mobileImage()));
        b.setLinkType(linkType);
        b.setLinkValue(linkValue);
        if (req.openInNewTab() != null) {
            b.setOpenInNewTab(req.openInNewTab());
        }
        if (req.active() != null) {
            b.setActive(req.active());
        } else {
            b.setActive(true);
        }
        b.setStartAt(times.startAt);
        b.setEndAt(times.endAt);
        b.setSortOrder(req.sortOrder() != null ? req.sortOrder() : 0);
    }

    private void applyUpdate(BannerEntity b, BannerUpsertRequest req, ParsedTimes times) {
        String name = trimOrEmpty(req.name());
        String image = trimOrEmpty(req.image());
        if (name.isEmpty() || image.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }
        String linkType = normalizeLinkType(req.linkType());
        if (linkType.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }
        String linkValue = trimOrEmpty(req.linkValue());
        if ("none".equals(linkType)) {
            linkValue = "";
        } else if (linkValue.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }
        if (times.startAt != null && times.endAt != null && times.endAt.isBefore(times.startAt)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "banner_invalid");
        }

        b.setName(name);
        b.setPosition(normalizePosition(req.position()));
        b.setTitleJson(BannerI18nJson.normalizeToStoredJson(req.title()));
        b.setSubtitleJson(BannerI18nJson.normalizeToStoredJson(req.subtitle()));
        b.setImage(image);
        b.setMobileImage(trimOrEmpty(req.mobileImage()));
        b.setLinkType(linkType);
        b.setLinkValue(linkValue);
        b.setStartAt(times.startAt);
        b.setEndAt(times.endAt);
        b.setSortOrder(req.sortOrder() != null ? req.sortOrder() : b.getSortOrder());
        if (req.openInNewTab() != null) {
            b.setOpenInNewTab(req.openInNewTab());
        }
        if (req.active() != null) {
            b.setActive(req.active());
        }
    }

    private static String trimOrEmpty(String s) {
        return s == null ? "" : s.trim();
    }

    /** 与 Go {@code normalizeBannerPosition} 一致。 */
    private static String normalizePosition(String raw) {
        String value = trimOrEmpty(raw);
        if (value.isEmpty()) {
            return "home_hero";
        }
        if ("home_hero".equals(value)) {
            return value;
        }
        return "home_hero";
    }

    /** 与 Go {@code normalizeBannerLinkType} 一致；非法值返回空串。 */
    private static String normalizeLinkType(String raw) {
        if (raw == null || raw.isBlank()) {
            return "none";
        }
        String v = raw.trim().toLowerCase(Locale.ROOT);
        return switch (v) {
            case "none" -> "none";
            case "internal" -> "internal";
            case "external" -> "external";
            default -> "";
        };
    }

    private static ParsedTimes parseTimes(BannerUpsertRequest req) {
        Instant startAt = parseInstantNullable(req.startAt());
        Instant endAt = parseInstantNullable(req.endAt());
        return new ParsedTimes(startAt, endAt);
    }

    /** 与 Go {@code shared.ParseTimeNullable}（RFC3339）一致。 */
    private static Instant parseInstantNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw);
        } catch (DateTimeParseException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "bad_request");
        }
    }

    private record ParsedTimes(Instant startAt, Instant endAt) {}

    private BannerDto toDto(BannerEntity b) {
        JsonNode title = BannerI18nJson.storedToResponseNode(b.getTitleJson());
        JsonNode subtitle = BannerI18nJson.storedToResponseNode(b.getSubtitleJson());
        return new BannerDto(
                b.getId(),
                b.getName(),
                b.getPosition(),
                title,
                subtitle,
                b.getImage(),
                emptyToNull(b.getMobileImage()),
                b.getLinkType(),
                emptyToNull(b.getLinkValue()),
                b.isOpenInNewTab(),
                b.isActive(),
                b.getStartAt(),
                b.getEndAt(),
                b.getSortOrder(),
                b.getCreatedAt(),
                b.getUpdatedAt());
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return s;
    }
}
