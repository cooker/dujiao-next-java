package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_mappings")
public class ProductMappingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "connection_id", nullable = false)
    private Long connectionId;

    @Column(name = "local_product_id", nullable = false)
    private Long localProductId;

    @Column(name = "remote_product_id", nullable = false)
    private Long remoteProductId;

    @Column(nullable = false, length = 32)
    private String status = "active";

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(Long connectionId) {
        this.connectionId = connectionId;
    }

    public Long getLocalProductId() {
        return localProductId;
    }

    public void setLocalProductId(Long localProductId) {
        this.localProductId = localProductId;
    }

    public Long getRemoteProductId() {
        return remoteProductId;
    }

    public void setRemoteProductId(Long remoteProductId) {
        this.remoteProductId = remoteProductId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
