package com.dujiao.api.dto.admin;

import com.fasterxml.jackson.annotation.JsonProperty;

/** 审批通过时返回明文 secret 一次，请客户端妥善保存。 */
public record ApiCredentialApproveResponse(
        ApiCredentialAdminDto credential,
        @JsonProperty("secret_plain") String secretPlain) {}
