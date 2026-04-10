package com.dujiao.api.repository;

import com.dujiao.api.domain.PostEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository
        extends JpaRepository<PostEntity, Long>, JpaSpecificationExecutor<PostEntity> {

    Optional<PostEntity> findBySlug(String slug);

    Optional<PostEntity> findBySlugAndPublishedTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /** 与 Go {@code ListPublic} 排序 {@code published_at DESC, created_at DESC} 一致。 */
    @Query(
            "SELECT p FROM PostEntity p WHERE p.published = true ORDER BY p.publishedAt DESC NULLS"
                    + " LAST, p.createdAt DESC")
    List<PostEntity> findPublicPublished();
}
