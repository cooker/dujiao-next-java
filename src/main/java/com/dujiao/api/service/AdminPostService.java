package com.dujiao.api.service;

import com.dujiao.api.common.api.ResponseCodes;
import com.dujiao.api.common.exception.BusinessException;
import com.dujiao.api.domain.PostEntity;
import com.dujiao.api.dto.post.PostDto;
import com.dujiao.api.dto.post.PostUpsertRequest;
import com.dujiao.api.repository.PostRepository;
import com.dujiao.api.util.PostJsonMapper;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPostService {

    private static final Set<String> ALLOWED_TYPES = Set.of("blog", "notice");

    private final PostRepository postRepository;

    public AdminPostService(PostRepository postRepository) {
        this.postRepository = postRepository;
    }

    @Transactional(readOnly = true)
    public Page<PostDto> list(int page, int pageSize, String type, String search) {
        int p = Math.max(page, 1);
        int ps = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Pageable pageable =
                PageRequest.of(p - 1, ps, Sort.by(Sort.Direction.DESC, "createdAt"));
        Specification<PostEntity> spec = adminListSpec(type, search);
        return postRepository.findAll(spec, pageable).map(this::toDto);
    }

    private static Specification<PostEntity> adminListSpec(String type, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (type != null && !type.isBlank()) {
                predicates.add(cb.equal(root.get("type"), type.trim()));
            }
            if (search != null && !search.isBlank()) {
                String q = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate slugLike = cb.like(cb.lower(root.get("slug")), q);
                Predicate titleLike = cb.like(cb.lower(root.get("titleJson")), q);
                predicates.add(cb.or(slugLike, titleLike));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    @Transactional(readOnly = true)
    public PostDto get(long id) {
        return toDto(
                postRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "post_not_found")));
    }

    @Transactional
    public PostDto create(PostUpsertRequest req) {
        String slug = req.slug().trim();
        String type = req.type().trim();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "post_type_invalid");
        }
        if (postRepository.existsBySlug(slug)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "slug_exists");
        }
        PostEntity p = new PostEntity();
        applyContent(p, req, slug, type);
        boolean pub = req.published() != null && req.published();
        p.setPublished(pub);
        if (pub) {
            p.setPublishedAt(Instant.now());
        }
        return toDto(postRepository.save(p));
    }

    @Transactional
    public PostDto update(long id, PostUpsertRequest req) {
        PostEntity p =
                postRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "post_not_found"));
        String slug = req.slug().trim();
        String type = req.type().trim();
        if (!ALLOWED_TYPES.contains(type)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "post_type_invalid");
        }
        if (postRepository.existsBySlugAndIdNot(slug, id)) {
            throw new BusinessException(ResponseCodes.BAD_REQUEST, "slug_used");
        }
        applyContent(p, req, slug, type);
        if (req.published() != null) {
            boolean next = req.published();
            if (next && !p.isPublished() && p.getPublishedAt() == null) {
                p.setPublishedAt(Instant.now());
            }
            p.setPublished(next);
        }
        return toDto(postRepository.save(p));
    }

    @Transactional
    public void delete(long id) {
        PostEntity p =
                postRepository
                        .findById(id)
                        .orElseThrow(
                                () -> new BusinessException(ResponseCodes.NOT_FOUND, "post_not_found"));
        postRepository.delete(p);
    }

    private static void applyContent(PostEntity p, PostUpsertRequest req, String slug, String type) {
        p.setSlug(slug);
        p.setType(type);
        p.setTitleJson(PostJsonMapper.toStoredJson(req.title()));
        p.setSummaryJson(PostJsonMapper.toStoredJson(req.summary()));
        p.setContentJson(PostJsonMapper.toStoredJson(req.content()));
        String th = req.thumbnail();
        p.setThumbnail(th == null ? "" : th.trim());
    }

    private PostDto toDto(PostEntity p) {
        JsonNode title = PostJsonMapper.toResponseNode(p.getTitleJson());
        JsonNode summary = PostJsonMapper.toResponseNode(p.getSummaryJson());
        JsonNode content = PostJsonMapper.toResponseNode(p.getContentJson());
        String th = p.getThumbnail();
        return new PostDto(
                p.getId(),
                p.getSlug(),
                p.getType(),
                title,
                summary,
                content,
                th == null || th.isEmpty() ? null : th,
                p.isPublished(),
                p.getPublishedAt(),
                p.getCreatedAt());
    }
}
