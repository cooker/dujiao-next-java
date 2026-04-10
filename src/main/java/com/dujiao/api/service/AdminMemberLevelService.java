package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.MemberLevelEntity;
import com.dujiao.api.domain.MemberLevelPriceEntity;
import com.dujiao.api.domain.UserAccount;
import com.dujiao.api.dto.memberlevel.MemberLevelDto;
import com.dujiao.api.dto.memberlevel.MemberLevelPriceBatchRequest;
import com.dujiao.api.dto.memberlevel.MemberLevelPriceDto;
import com.dujiao.api.dto.memberlevel.MemberLevelUpsertRequest;
import com.dujiao.api.repository.MemberLevelPriceRepository;
import com.dujiao.api.repository.MemberLevelRepository;
import com.dujiao.api.repository.UserAccountRepository;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMemberLevelService {

    private final MemberLevelRepository memberLevelRepository;
    private final MemberLevelPriceRepository memberLevelPriceRepository;
    private final UserAccountRepository userAccountRepository;

    public AdminMemberLevelService(
            MemberLevelRepository memberLevelRepository,
            MemberLevelPriceRepository memberLevelPriceRepository,
            UserAccountRepository userAccountRepository) {
        this.memberLevelRepository = memberLevelRepository;
        this.memberLevelPriceRepository = memberLevelPriceRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional(readOnly = true)
    public List<MemberLevelDto> list() {
        return memberLevelRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberLevelDto get(long id) {
        return toDto(
                memberLevelRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "member_level_not_found")));
    }

    @Transactional
    public MemberLevelDto create(MemberLevelUpsertRequest req) {
        String slug = req.slug().trim().toLowerCase();
        if (memberLevelRepository.findBySlug(slug).isPresent()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "member_level_slug_exists");
        }
        if (Boolean.TRUE.equals(req.defaultLevel())) {
            unsetOtherDefaults(null);
        }
        MemberLevelEntity e = new MemberLevelEntity();
        apply(e, req, slug);
        return toDto(memberLevelRepository.save(e));
    }

    @Transactional
    public MemberLevelDto update(long id, MemberLevelUpsertRequest req) {
        MemberLevelEntity e =
                memberLevelRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "member_level_not_found"));
        String slug = req.slug().trim().toLowerCase();
        memberLevelRepository
                .findBySlug(slug)
                .ifPresent(
                        other -> {
                            if (!other.getId().equals(id)) {
                                throw new BusinessException(
                                        ResponseCodes.BAD_REQUEST, "member_level_slug_exists");
                            }
                        });
        if (Boolean.TRUE.equals(req.defaultLevel())) {
            unsetOtherDefaults(id);
        }
        apply(e, req, slug);
        return toDto(memberLevelRepository.save(e));
    }

    @Transactional
    public void delete(long id) {
        MemberLevelEntity e =
                memberLevelRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.NOT_FOUND, "member_level_not_found"));
        if (userAccountRepository.countByMemberLevelId(id) > 0) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "member_level_in_use");
        }
        memberLevelPriceRepository.deleteByMemberLevelId(id);
        memberLevelRepository.delete(e);
    }

    @Transactional(readOnly = true)
    public List<MemberLevelPriceDto> listPrices() {
        return memberLevelPriceRepository.findAll().stream().map(this::priceToDto).toList();
    }

    @Transactional
    public void batchUpsertPrices(MemberLevelPriceBatchRequest req) {
        for (MemberLevelPriceBatchRequest.MemberLevelPriceItem it : req.items()) {
            if (!memberLevelRepository.existsById(it.memberLevelId())) {
                throw new BusinessException(ResponseCodes.BAD_REQUEST, "member_level_not_found");
            }
            MemberLevelPriceEntity e =
                    memberLevelPriceRepository
                            .findByMemberLevelIdAndProductId(it.memberLevelId(), it.productId())
                            .orElseGet(MemberLevelPriceEntity::new);
            e.setMemberLevelId(it.memberLevelId());
            e.setProductId(it.productId());
            e.setPriceAmount(it.priceAmount());
            memberLevelPriceRepository.save(e);
        }
    }

    @Transactional
    public void deletePrice(long priceId) {
        if (!memberLevelPriceRepository.existsById(priceId)) {
            throw new BusinessException(ResponseCodes.NOT_FOUND, "member_level_price_not_found");
        }
        memberLevelPriceRepository.deleteById(priceId);
    }

    /** 将未设置等级的用户关联到当前默认等级。 */
    @Transactional
    public int backfillDefaultLevel() {
        MemberLevelEntity def =
                memberLevelRepository
                        .findByDefaultLevelTrue()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                ResponseCodes.BAD_REQUEST, "no_default_member_level"));
        List<UserAccount> users = userAccountRepository.findByMemberLevelIdIsNull();
        int n = 0;
        for (UserAccount u : users) {
            u.setMemberLevelId(def.getId());
            userAccountRepository.save(u);
            n++;
        }
        return n;
    }

    private void unsetOtherDefaults(Long exceptId) {
        memberLevelRepository
                .findByDefaultLevelTrue()
                .ifPresent(
                        cur -> {
                            if (exceptId == null || !cur.getId().equals(exceptId)) {
                                cur.setDefaultLevel(false);
                                memberLevelRepository.save(cur);
                            }
                        });
    }

    private void apply(MemberLevelEntity e, MemberLevelUpsertRequest req, String slug) {
        e.setSlug(slug);
        e.setName(req.name().trim());
        if (req.discountRate() != null) {
            e.setDiscountRate(req.discountRate());
        }
        if (req.rechargeThreshold() != null) {
            e.setRechargeThreshold(req.rechargeThreshold());
        }
        if (req.spendThreshold() != null) {
            e.setSpendThreshold(req.spendThreshold());
        }
        if (req.defaultLevel() != null) {
            e.setDefaultLevel(req.defaultLevel());
        }
        if (req.sortOrder() != null) {
            e.setSortOrder(req.sortOrder());
        }
        if (req.active() != null) {
            e.setActive(req.active());
        }
    }

    private MemberLevelDto toDto(MemberLevelEntity e) {
        return new MemberLevelDto(
                e.getId(),
                e.getSlug(),
                e.getName(),
                e.getDiscountRate(),
                e.getRechargeThreshold(),
                e.getSpendThreshold(),
                e.isDefaultLevel(),
                e.getSortOrder(),
                e.isActive());
    }

    private MemberLevelPriceDto priceToDto(MemberLevelPriceEntity p) {
        return new MemberLevelPriceDto(
                p.getId(), p.getMemberLevelId(), p.getProductId(), p.getPriceAmount());
    }
}
