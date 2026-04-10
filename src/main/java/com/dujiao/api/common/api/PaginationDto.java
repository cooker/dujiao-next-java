package com.dujiao.api.common.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PaginationDto {

    @JsonProperty("page")
    private final int page;

    @JsonProperty("page_size")
    private final int pageSize;

    private final long total;

    @JsonProperty("total_page")
    private final long totalPage;

    public PaginationDto(int page, int pageSize, long total, long totalPage) {
        this.page = page;
        this.pageSize = pageSize;
        this.total = total;
        this.totalPage = totalPage;
    }

    public static PaginationDto of(int page, int pageSize, long total) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        long tp = ps == 0 ? 0 : (total + ps - 1) / ps;
        return new PaginationDto(p, ps, total, tp);
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotal() {
        return total;
    }

    public long getTotalPage() {
        return totalPage;
    }
}
