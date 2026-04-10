package com.dujiao.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "channel_clients")
public class ChannelClientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_id", nullable = false, unique = true, length = 64)
    private String clientId;

    /** AES-GCM(hex) 存储，与 Go 侧一致，用于 HMAC 验签时解密出明文密钥。 */
    @Column(name = "secret_cipher", nullable = false, length = 4000)
    private String secretCipher;

    @Column(nullable = false, length = 32)
    private String status = "active";

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

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getSecretCipher() {
        return secretCipher;
    }

    public void setSecretCipher(String secretCipher) {
        this.secretCipher = secretCipher;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
