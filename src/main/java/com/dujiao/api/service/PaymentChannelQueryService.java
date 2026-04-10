package com.dujiao.api.service;

import com.dujiao.api.domain.PaymentChannelEntity;
import com.dujiao.api.dto.channel.ChannelPaymentChannelItem;
import com.dujiao.api.dto.payment.PaymentChannelOptionDto;
import com.dujiao.api.repository.PaymentChannelRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentChannelQueryService {

    private final PaymentChannelRepository paymentChannelRepository;

    public PaymentChannelQueryService(PaymentChannelRepository paymentChannelRepository) {
        this.paymentChannelRepository = paymentChannelRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentChannelOptionDto> listActive() {
        return paymentChannelRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(e -> new PaymentChannelOptionDto(e.getId(), e.getName(), e.getChannelType()))
                .toList();
    }

    /** 渠道 API：与 Go {@code GetPaymentChannels} 列表项字段对齐（费率等暂无则占位）。 */
    @Transactional(readOnly = true)
    public List<ChannelPaymentChannelItem> listChannelItems() {
        return paymentChannelRepository.findByActiveTrueOrderByIdAsc().stream()
                .map(this::toChannelItem)
                .toList();
    }

    private ChannelPaymentChannelItem toChannelItem(PaymentChannelEntity e) {
        String ct = e.getChannelType() != null ? e.getChannelType() : "";
        return new ChannelPaymentChannelItem(
                e.getId(), e.getName(), ct, ct, "redirect", "0.00", "0.00");
    }
}
