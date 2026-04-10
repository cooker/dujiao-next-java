package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "authz_audit_logs")
public class AuthzAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_admin_id", nullable = false)
    private Long operatorAdminId;

    @Column(name = "operator_username", length = 64)
    private String operatorUsername;

    @Column(name = "target_admin_id")
    private Long targetAdminId;

    @Column(name = "target_username", length = 64)
    private String targetUsername;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(length = 128)
    private String role;

    @Column(length = 255)
    private String object;

    @Column(length = 32)
    private String method;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "detail_json", columnDefinition = "text")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public Long getOperatorAdminId() {
        return operatorAdminId;
    }

    public void setOperatorAdminId(Long operatorAdminId) {
        this.operatorAdminId = operatorAdminId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public void setOperatorUsername(String operatorUsername) {
        this.operatorUsername = operatorUsername;
    }

    public Long getTargetAdminId() {
        return targetAdminId;
    }

    public void setTargetAdminId(Long targetAdminId) {
        this.targetAdminId = targetAdminId;
    }

    public String getTargetUsername() {
        return targetUsername;
    }

    public void setTargetUsername(String targetUsername) {
        this.targetUsername = targetUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(String detailJson) {
        this.detailJson = detailJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
