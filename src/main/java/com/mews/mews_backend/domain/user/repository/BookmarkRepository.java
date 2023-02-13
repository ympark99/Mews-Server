package com.mews.mews_backend.domain.user.repository;

import com.mews.mews_backend.domain.article.entity.Article;
import com.mews.mews_backend.domain.user.entity.Bookmark;
import com.mews.mews_backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Integer> {

    @Query("select b from Bookmark b where b.user.id = :userId and b.article.id = :articleId")
    List<Bookmark> existsByIdAndArticleId(Integer userId, Integer articleId);

    boolean existsByUserAndArticle(User user, Article article);

//    @Query("select b from Bookmark b where b.user.id = :userId ORDER BY b.modifiedAt DESC")
//    List<Bookmark> findAllByUserId(Integer userId);

    List<Bookmark> findAllByUserOrderByModifiedAtDesc(User user);

    @Modifying
    @Query("delete from Bookmark b where b.user.id = :userId and b.article.id = :articleId")
    void deleteByIdAndArticleId(Integer userId, Integer articleId);
}
