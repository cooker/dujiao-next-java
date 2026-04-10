package com.dujiao.api.service;

import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.AffiliateCommissionEntity;
import com.dujiao.api.domain.AffiliateProfileEntity;
import com.dujiao.api.domain.AffiliateWithdrawEntity;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.dto.user.AffiliateCommissionDto;
import com.dujiao.api.dto.user.AffiliateDashboardDto;
import com.dujiao.api.dto.user.AffiliateProfileOpenDto;
import com.dujiao.api.dto.user.AffiliateWithdrawApplyRequest;
import com.dujiao.api.dto.user.AffiliateWithdrawDto;
import com.dujiao.api.repository.AffiliateCommissionRepository;
import com.dujiao.api.repository.AffiliateProfileRepository;
import com.dujiao.api.repository.AffiliateWithdrawRepository;
import com.dujiao.api.repository.UserAccountRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户侧推广：开通、看板、佣金/提现列表、提现申请。
 *
 * <p>提现：当前实现要求申请金额<strong>等于</strong>全部可提现佣金之和（与 Go 支持任意拆分相比为简化版）。
 */
@Service
public class UserAffiliateService {

    private static final String PROFILE_ACTIVE = "active";
    private static final String COMMISSION_PENDING = "pending_confirm";
    private static final String COMMISSION_AVAILABLE = "available";
    private static final String COMMISSION_WITHDRAWN = "withdrawn";
    private static final String COMMISSION_REJECTED = "rejected";
    private static final String WITHDRAW_PENDING = "pending_review";
    private static final String AFF_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final SettingsService settingsService;
    private final UserAccountRepository userAccountRepository;
    private final AffiliateProfileRepository affiliateProfileRepository;
    private final AffiliateCommissionRepository affiliateCommissionRepository;
    private final AffiliateWithdrawRepository affiliateWithdrawRepository;
    private final AffiliateClickService affiliateClickService;

    public UserAffiliateService(
            SettingsService settingsService,
            UserAccountRepository userAccountRepository,
            AffiliateProfileRepository affiliateProfileRepository,
            AffiliateCommissionRepository affiliateCommissionRepository,
            AffiliateWithdrawRepository affiliateWithdrawRepository,
            AffiliateClickService affiliateClickService) {
        this.settingsService = settingsService;
        this.userAccountRepository = userAccountRepository;
        this.affiliateProfileRepository = affiliateProfileRepository;
        this.affiliateCommissionRepository = affiliateCommissionRepository;
        this.affiliateWithdrawRepository = affiliateWithdrawRepository;
        this.affiliateClickService = affiliateClickService;
    }

    @Transactional
    public AffiliateProfileOpenDto openAffiliate(long userId) {
        if (!settingsService.affiliateEnabled()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "forbidden");
        }
        UserAccount u =
                userAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "user_not_found"));
        if (!"active".equals(u.getStatus())) {
            throw new BusinessException(ResponseCodes.FORBIDDEN, "account_disabled");
        }
        Optional<AffiliateProfileEntity> existing = affiliateProfileRepository.findByUserId(userId);
        if (existing.isPresent()) {
            return toProfileDto(existing.get());
        }
        for (int i = 0; i < 24; i++) {
            String code = randomAffiliateCode();
            if (!affiliateProfileRepository.existsByAffiliateCode(code)) {
                AffiliateProfileEntity e = new AffiliateProfileEntity();
                e.setUserId(userId);
                e.setAffiliateCode(code);
                e.setStatus(PROFILE_ACTIVE);
                return toProfileDto(affiliateProfileRepository.save(e));
            }
        }
        throw new BusinessException(ResponseCodes.INTERNAL, "save_failed");
    }

    @Transactional(readOnly = true)
    public AffiliateDashboardDto getDashboard(long userId) {
        BigDecimal zero = new BigDecimal("0.00");
        AffiliateProfileEntity profile = affiliateProfileRepository.findByUserId(userId).orElse(null);
        if (profile == null) {
            return new AffiliateDashboardDto(
                    false, "", "", 0, 0, 0.0, zero, zero, zero);
        }
        long pid = profile.getId();
        BigDecimal pending =
                nz(affiliateCommissionRepository.sumAmountByProfileAndStatuses(pid, List.of(COMMISSION_PENDING)));
        BigDecimal available =
                nz(affiliateCommissionRepository.sumAmountByProfileAndStatuses(pid, List.of(COMMISSION_AVAILABLE)));
        BigDecimal withdrawn =
                nz(affiliateCommissionRepository.sumAmountByProfileAndStatuses(pid, List.of(COMMISSION_WITHDRAWN)));
        long validOrderCount =
                affiliateCommissionRepository.countDistinctOrdersExcludingRejected(pid, COMMISSION_REJECTED);
        long clickCount = affiliateClickService.countClicksByProfileId(pid);
        double conversionRate = calcAffiliateConversion(validOrderCount, clickCount);
        return new AffiliateDashboardDto(
                true,
                profile.getAffiliateCode(),
                "/?aff=" + profile.getAffiliateCode(),
                clickCount,
                validOrderCount,
                conversionRate,
                pending,
                available,
                withdrawn);
    }

    /** 与 Go {@code calcAffiliateConversion}：有效订单数 / 点击数 × 100，保留两位小数。 */
    private static double calcAffiliateConversion(long validOrders, long clicks) {
        if (clicks <= 0 || validOrders <= 0) {
            return 0.0;
        }
        double v = (validOrders * 100.0) / clicks;
        return Math.round(v * 100.0) / 100.0;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<AffiliateCommissionDto>> listCommissions(
            long userId, int page, int pageSize, String status) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);
        Optional<AffiliateProfileEntity> prof = affiliateProfileRepository.findByUserId(userId);
        if (prof.isEmpty()) {
            return PageResponse.success(List.of(), PaginationDto.of(p, ps, 0));
        }
        long pid = prof.get().getId();
        String st = status == null ? "" : status.trim();
        Page<AffiliateCommissionEntity> result;
        if (st.isEmpty()) {
            result = affiliateCommissionRepository.findByAffiliateProfileIdOrderByIdDesc(pid, pr);
        } else {
            result = affiliateCommissionRepository.findByAffiliateProfileIdAndStatusOrderByIdDesc(pid, st, pr);
        }
        List<AffiliateCommissionDto> list = result.getContent().stream().map(this::toCommissionDto).toList();
        PaginationDto pg =
                PaginationDto.of(result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public PageResponse<List<AffiliateWithdrawDto>> listWithdraws(
            long userId, int page, int pageSize, String status) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        PageRequest pr = PageRequest.of(p - 1, ps);
        Optional<AffiliateProfileEntity> prof = affiliateProfileRepository.findByUserId(userId);
        if (prof.isEmpty()) {
            return PageResponse.success(List.of(), PaginationDto.of(p, ps, 0));
        }
        long pid = prof.get().getId();
        String st = status == null ? "" : status.trim();
        Page<AffiliateWithdrawEntity> result;
        if (st.isEmpty()) {
            result = affiliateWithdrawRepository.findByAffiliateProfileIdOrderByIdDesc(pid, pr);
        } else {
            result = affiliateWithdrawRepository.findByAffiliateProfileIdAndStatusOrderByIdDesc(pid, st, pr);
        }
        List<AffiliateWithdrawDto> list = result.getContent().stream().map(this::toWithdrawDto).toList();
        PaginationDto pg =
                PaginationDto.of(result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional
    public AffiliateWithdrawDto applyWithdraw(long userId, AffiliateWithdrawApplyRequest req) {
        if (!settingsService.affiliateEnabled()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "forbidden");
        }
        BigDecimal amount;
        try {
            amount = new BigDecimal(req.amount().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_amount_invalid");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_amount_invalid");
        }
        BigDecimal min = settingsService.affiliateMinWithdrawAmount();
        if (amount.compareTo(min) < 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_amount_invalid");
        }
        String channel = req.channel().trim();
        String account = req.account().trim();
        if (channel.isEmpty() || account.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_channel_invalid");
        }
        List<String> allowed = settingsService.affiliateWithdrawChannels();
        if (!allowed.isEmpty()) {
            String ch = channel.toLowerCase(Locale.ROOT);
            if (!allowed.contains(ch)) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_channel_invalid");
            }
        }

        AffiliateProfileEntity profile =
                affiliateProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_not_opened"));
        if (!PROFILE_ACTIVE.equals(profile.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_not_opened");
        }
        long pid = profile.getId();
        List<AffiliateCommissionEntity> available =
                affiliateCommissionRepository.findByAffiliateProfileIdAndStatusAndWithdrawRequestIdIsNullOrderByIdAsc(
                        pid, COMMISSION_AVAILABLE);
        BigDecimal sum = BigDecimal.ZERO;
        for (AffiliateCommissionEntity c : available) {
            sum = sum.add(c.getCommissionAmount());
        }
        sum = sum.setScale(2, RoundingMode.HALF_UP);
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_insufficient");
        }
        if (amount.compareTo(sum) > 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_insufficient");
        }

        AffiliateWithdrawEntity w = new AffiliateWithdrawEntity();
        w.setAffiliateProfileId(pid);
        w.setAmount(amount);
        w.setChannel(channel);
        w.setAccount(account);
        w.setStatus(WITHDRAW_PENDING);
        AffiliateWithdrawEntity saved = affiliateWithdrawRepository.save(w);

        BigDecimal remain = amount;
        for (AffiliateCommissionEntity c : available) {
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal lineAmt = nz(c.getCommissionAmount());
            if (lineAmt.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (lineAmt.compareTo(remain) <= 0) {
                c.setStatus(COMMISSION_WITHDRAWN);
                c.setWithdrawRequestId(saved.getId());
                affiliateCommissionRepository.save(c);
                remain = remain.subtract(lineAmt);
                continue;
            }
            // 拆分佣金行：原行保留可提现余额，新增一行标记为已提现并挂到当前提现申请。
            BigDecimal withdrawPart = remain;
            BigDecimal leftPart = lineAmt.subtract(withdrawPart).setScale(2, RoundingMode.HALF_UP);
            c.setCommissionAmount(leftPart);
            affiliateCommissionRepository.save(c);

            AffiliateCommissionEntity split = new AffiliateCommissionEntity();
            split.setAffiliateProfileId(c.getAffiliateProfileId());
            split.setOrderId(c.getOrderId());
            split.setCommissionType(c.getCommissionType());
            split.setCommissionAmount(withdrawPart);
            split.setStatus(COMMISSION_WITHDRAWN);
            split.setConfirmAt(c.getConfirmAt());
            split.setAvailableAt(c.getAvailableAt());
            split.setWithdrawRequestId(saved.getId());
            split.setInvalidReason(c.getInvalidReason());
            affiliateCommissionRepository.save(split);
            remain = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (remain.compareTo(BigDecimal.ZERO) > 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "affiliate_withdraw_insufficient");
        }
        return toWithdrawDto(saved);
    }

    private static BigDecimal nz(BigDecimal v) {
        if (v == null) {
            return new BigDecimal("0.00");
        }
        return v.setScale(2, RoundingMode.HALF_UP);
    }

    private static String randomAffiliateCode() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            sb.append(AFF_CHARS.charAt(r.nextInt(AFF_CHARS.length())));
        }
        return sb.toString();
    }

    private AffiliateProfileOpenDto toProfileDto(AffiliateProfileEntity e) {
        return new AffiliateProfileOpenDto(e.getId(), e.getAffiliateCode(), e.getStatus(), e.getCreatedAt());
    }

    private AffiliateCommissionDto toCommissionDto(AffiliateCommissionEntity e) {
        return new AffiliateCommissionDto(
                e.getId(),
                e.getCommissionType(),
                e.getCommissionAmount(),
                e.getStatus(),
                e.getConfirmAt(),
                e.getAvailableAt(),
                e.getCreatedAt());
    }

    private AffiliateWithdrawDto toWithdrawDto(AffiliateWithdrawEntity e) {
        return new AffiliateWithdrawDto(
                e.getId(),
                e.getAmount(),
                e.getChannel(),
                e.getAccount(),
                e.getStatus(),
                e.getRejectReason(),
                e.getCreatedAt());
    }
}
