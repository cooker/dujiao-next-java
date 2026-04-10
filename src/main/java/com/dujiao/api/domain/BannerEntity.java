package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** 与 Go {@code models.Banner} / 表 {@code banners} 对齐。 */
@Entity
@Table(name = "banners")
public class BannerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    /** Go {@code BannerPositionHomeHero} = {@code home_hero} */
    @Column(nullable = false, length = 60)
    private String position = "home_hero";

    @Column(name = "title", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String titleJson;

    @Column(name = "subtitle", columnDefinition = "jsonb", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String subtitleJson;

    @Column(nullable = false, length = 500)
    private String image;

    @Column(name = "mobile_image", length = 500)
    private String mobileImage = "";

    @Column(name = "link_type", nullable = false, length = 20)
    private String linkType = "none";

    @Column(name = "link_value", length = 1000)
    private String linkValue;

    @Column(name = "open_in_new_tab", nullable = false)
    private boolean openInNewTab;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "start_at")
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getTitleJson() {
        return titleJson;
    }

    public void setTitleJson(String titleJson) {
        this.titleJson = titleJson;
    }

    public String getSubtitleJson() {
        return subtitleJson;
    }

    public void setSubtitleJson(String subtitleJson) {
        this.subtitleJson = subtitleJson;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getMobileImage() {
        return mobileImage;
    }

    public void setMobileImage(String mobileImage) {
        this.mobileImage = mobileImage;
    }

    public String getLinkType() {
        return linkType;
    }

    public void setLinkType(String linkType) {
        this.linkType = linkType;
    }

    public String getLinkValue() {
        return linkValue;
    }

    public void setLinkValue(String linkValue) {
        this.linkValue = linkValue;
    }

    public boolean isOpenInNewTab() {
        return openInNewTab;
    }

    public void setOpenInNewTab(boolean openInNewTab) {
        this.openInNewTab = openInNewTab;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getStartAt() {
        return startAt;
    }

    public void setStartAt(Instant startAt) {
        this.startAt = startAt;
    }

    public Instant getEndAt() {
        return endAt;
    }

    public void setEndAt(Instant endAt) {
        this.endAt = endAt;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
