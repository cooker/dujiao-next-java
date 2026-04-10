package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.MediaAsset;
import com.dujiao.api.dto.media.AdminMediaItemDto;
import com.dujiao.api.dto.media.MediaDto;
import com.dujiao.api.dto.media.MediaUpdateRequest;
import com.dujiao.api.repository.MediaAssetRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MediaService {

    private final MediaAssetRepository mediaAssetRepository;
    private final Path uploadRoot;
    private final String publicBasePath;

    public MediaService(
            MediaAssetRepository mediaAssetRepository,
            @Value("${dujiao.upload.base-dir:./data/uploads}") String baseDir,
            @Value("${dujiao.upload.public-base-path:/uploads}") String publicBasePath) {
        this.mediaAssetRepository = mediaAssetRepository;
        this.uploadRoot = Path.of(baseDir).toAbsolutePath().normalize();
        this.publicBasePath = normalizePublicBase(publicBasePath);
    }

    private static String normalizePublicBase(String p) {
        if (p == null || p.isBlank()) {
            return "";
        }
        return p.startsWith("/") ? p : "/" + p;
    }

    @PostConstruct
    void ensureUploadDir() throws IOException {
        Files.createDirectories(uploadRoot);
    }

    @Transactional
    public MediaDto upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "empty_file");
        }
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            original = "bin";
        }
        String ext = extension(original);
        String stored = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = uploadRoot.resolve(stored);
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "upload_failed");
        }
        MediaAsset m = new MediaAsset();
        m.setStoredFilename(stored);
        m.setOriginalFilename(original);
        m.setName(basename(original));
        m.setScene("default");
        m.setContentType(file.getContentType());
        m.setSizeBytes(file.getSize());
        m.setWidth(0);
        m.setHeight(0);
        m = mediaAssetRepository.save(m);
        return toDto(m);
    }

    @Transactional(readOnly = true)
    public MediaListResult listForAdmin(String scene, String search, int page, int pageSize) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : pageSize;
        Specification<MediaAsset> spec =
                (root, query, cb) -> {
                    var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    if (scene != null && !scene.isBlank()) {
                        predicates.add(cb.equal(root.get("scene"), scene.trim()));
                    }
                    if (search != null && !search.isBlank()) {
                        String kw = "%" + search.trim().toLowerCase() + "%";
                        predicates.add(
                                cb.or(
                                        cb.like(cb.lower(root.get("name")), kw),
                                        cb.like(cb.lower(root.get("originalFilename")), kw)));
                    }
                    return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
        Page<MediaAsset> rows =
                mediaAssetRepository.findAll(
                        spec,
                        PageRequest.of(
                                p - 1, ps, org.springframework.data.domain.Sort.by("id").descending()));
        return new MediaListResult(
                rows.getContent().stream().map(this::toAdminItem).toList(), rows.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<MediaDto> list() {
        return mediaAssetRepository.findAllByOrderByIdDesc().stream().map(this::toDto).toList();
    }

    @Transactional
    public MediaDto update(long id, MediaUpdateRequest req) {
        MediaAsset m =
                mediaAssetRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "media_not_found"));
        if (req.name() != null && !req.name().isBlank()) {
            m.setName(req.name().trim());
        }
        return toDto(mediaAssetRepository.save(m));
    }

    @Transactional
    public void delete(long id) {
        MediaAsset m =
                mediaAssetRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "media_not_found"));
        if (m.getStoredFilename() != null) {
            try {
                Files.deleteIfExists(uploadRoot.resolve(m.getStoredFilename()));
            } catch (IOException ignored) {
                // 仍删除数据库记录，避免残留元数据
            }
        }
        mediaAssetRepository.delete(m);
    }

    private MediaDto toDto(MediaAsset m) {
        String url = publicBasePath + "/" + m.getStoredFilename();
        return new MediaDto(
                m.getId(),
                m.getOriginalFilename(),
                m.getContentType(),
                m.getSizeBytes(),
                url,
                m.getCreatedAt());
    }

    private AdminMediaItemDto toAdminItem(MediaAsset m) {
        return new AdminMediaItemDto(
                m.getId(),
                m.getName() == null || m.getName().isBlank() ? basename(m.getOriginalFilename()) : m.getName(),
                m.getOriginalFilename(),
                publicBasePath + "/" + m.getStoredFilename(),
                m.getContentType() == null ? "application/octet-stream" : m.getContentType(),
                m.getSizeBytes(),
                m.getScene() == null ? "default" : m.getScene(),
                m.getWidth() == null ? 0 : m.getWidth(),
                m.getHeight() == null ? 0 : m.getHeight(),
                m.getCreatedAt());
    }

    private static String basename(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        int idx = name.lastIndexOf('.');
        if (idx <= 0) {
            return name;
        }
        return name.substring(0, idx);
    }

    private static String extension(String filename) {
        int i = filename.lastIndexOf('.');
        if (i <= 0 || i == filename.length() - 1) {
            return "";
        }
        return filename.substring(i).toLowerCase();
    }

    public record MediaListResult(List<AdminMediaItemDto> items, long total) {}
}
