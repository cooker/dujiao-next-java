package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

/** 与 Go {@code AffiliateClick} / 表 {@code affiliate_clicks} 对齐。 */
@Entity
@Table(name = "affiliate_clicks")
public class AffiliateClickEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "affiliate_profile_id", nullable = false)
    private Long affiliateProfileId;

    @Column(name = "visitor_key", length = 128)
    private String visitorKey;

    @Column(name = "landing_path", length = 512)
    private String landingPath;

    @Column(length = 1024)
    private String referrer;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

    @Column(name = "user_agent", length = 1024)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAffiliateProfileId() {
        return affiliateProfileId;
    }

    public void setAffiliateProfileId(Long affiliateProfileId) {
        this.affiliateProfileId = affiliateProfileId;
    }

    public String getVisitorKey() {
        return visitorKey;
    }

    public void setVisitorKey(String visitorKey) {
        this.visitorKey = visitorKey;
    }

    public String getLandingPath() {
        return landingPath;
    }

    public void setLandingPath(String landingPath) {
        this.landingPath = landingPath;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
