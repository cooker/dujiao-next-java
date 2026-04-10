package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.GiftCardEntity;
import com.dujiao.api.domain.WalletAccount;
import com.dujiao.api.domain.WalletTransaction;
import com.dujiao.api.dto.user.RedeemGiftCardRequest;
import com.dujiao.api.repository.GiftCardRepository;
import com.dujiao.api.repository.WalletAccountRepository;
import com.dujiao.api.repository.WalletTransactionRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 用户兑换礼品卡：入账钱包，与 Go {@code GiftCardService.RedeemGiftCard} 行为对齐（简化）。 */
@Service
public class UserGiftCardService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_REDEEMED = "redeemed";
    private static final String STATUS_DISABLED = "disabled";

    private final GiftCardRepository giftCardRepository;
    private final WalletAccountRepository walletAccountRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final WalletBootstrapService walletBootstrapService;
    private final SettingsService settingsService;
    private final CaptchaImageService captchaImageService;

    public UserGiftCardService(
            GiftCardRepository giftCardRepository,
            WalletAccountRepository walletAccountRepository,
            WalletTransactionRepository walletTransactionRepository,
            WalletBootstrapService walletBootstrapService,
            SettingsService settingsService,
            CaptchaImageService captchaImageService) {
        this.giftCardRepository = giftCardRepository;
        this.walletAccountRepository = walletAccountRepository;
        this.walletTransactionRepository = walletTransactionRepository;
        this.walletBootstrapService = walletBootstrapService;
        this.settingsService = settingsService;
        this.captchaImageService = captchaImageService;
    }

    @Transactional
    public void redeem(long userId, RedeemGiftCardRequest req) {
        if (settingsService.captchaEnabled() && settingsService.captchaSceneEnabled("gift_card_redeem")) {
            if (req.captchaPayload() == null) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "captcha_required");
            }
            if (!captchaImageService.verifyAndConsume(req.captchaPayload())) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "captcha_invalid");
            }
        }
        String raw = req.code() == null ? "" : req.code().trim();
        if (raw.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }
        String code = raw.toUpperCase(Locale.ROOT);
        GiftCardEntity card =
                giftCardRepository
                        .findByCodeForUpdate(code)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "gift_card_not_found"));
        if (STATUS_REDEEMED.equals(card.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_redeemed");
        }
        if (STATUS_DISABLED.equals(card.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_disabled");
        }
        if (!STATUS_ACTIVE.equals(card.getStatus())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }
        if (card.getExpiresAt() != null && Instant.now().isAfter(card.getExpiresAt())) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_expired");
        }
        BigDecimal amt = card.getBalance();
        if (amt == null || amt.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "gift_card_invalid");
        }

        walletBootstrapService.ensureWallet(userId);
        WalletAccount w =
                walletAccountRepository
                        .findById(userId)
                        .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "wallet_not_found"));
        BigDecimal newBal = w.getBalance().add(amt);
        w.setBalance(newBal);
        walletAccountRepository.save(w);

        WalletTransaction t = new WalletTransaction();
        t.setUserId(userId);
        t.setType("gift_card");
        t.setAmount(amt);
        t.setBalanceAfter(newBal);
        t.setRemark("gift_card:" + code);
        walletTransactionRepository.save(t);

        Instant now = Instant.now();
        card.setStatus(STATUS_REDEEMED);
        card.setRedeemedUserId(userId);
        card.setRedeemedAt(now);
        card.setWalletTxnId(t.getId());
        giftCardRepository.save(card);
    }
}
