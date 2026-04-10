package com.dujiao.api.service.admin;

import com.dujiao.api.common.api.PageResponse;
import com.dujiao.api.common.api.PaginationDto;
import com.dujiao.api.domain.AdminPaymentRecordEntity;
import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.dto.payment.AdminPaymentRecordDto;
import com.dujiao.api.repository.AdminPaymentRecordRepository;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPaymentRecordService {

    private final AdminPaymentRecordRepository adminPaymentRecordRepository;

    public AdminPaymentRecordService(AdminPaymentRecordRepository adminPaymentRecordRepository) {
        this.adminPaymentRecordRepository = adminPaymentRecordRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<List<AdminPaymentRecordDto>> list(int page, int pageSize) {
        PageRequest pr = PageRequest.of(Math.max(page - 1, 0), pageSize <= 0 ? 20 : pageSize);
        Page<AdminPaymentRecordEntity> result = adminPaymentRecordRepository.findAllByOrderByCreatedAtDesc(pr);
        List<AdminPaymentRecordDto> list = result.getContent().stream().map(this::toDto).toList();
        PaginationDto pg =
                PaginationDto.of(result.getNumber() + 1, result.getSize(), result.getTotalElements());
        return PageResponse.success(list, pg);
    }

    @Transactional(readOnly = true)
    public AdminPaymentRecordDto get(long id) {
        return toDto(require(id));
    }

    @Transactional(readOnly = true)
    public byte[] exportCsv() {
        List<AdminPaymentRecordEntity> all = adminPaymentRecordRepository.findAllByOrderByCreatedAtDesc();
        StringBuilder sb = new StringBuilder();
        sb.append("id,order_no,amount,status,channel_id,created_at\n");
        for (AdminPaymentRecordEntity e : all) {
            sb.append(e.getId())
                    .append(',')
                    .append(escapeCsv(e.getOrderNo()))
                    .append(',')
                    .append(e.getAmount() != null ? e.getAmount().toPlainString() : "")
                    .append(',')
                    .append(escapeCsv(e.getStatus()))
                    .append(',')
                    .append(e.getChannelId() != null ? e.getChannelId() : "")
                    .append(',')
                    .append(e.getCreatedAt() != null ? e.getCreatedAt().toString() : "")
                    .append('\n');
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private AdminPaymentRecordEntity require(long id) {
        return adminPaymentRecordRepository
                .findById(id)
                .orElseThrow(() -> new BusinessException(ResponseCodes.NOT_FOUND, "payment_record_not_found"));
    }

    private AdminPaymentRecordDto toDto(AdminPaymentRecordEntity e) {
        return new AdminPaymentRecordDto(
                e.getId(), e.getOrderNo(), e.getAmount(), e.getStatus(), e.getChannelId(), e.getCreatedAt());
    }

    private static String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
