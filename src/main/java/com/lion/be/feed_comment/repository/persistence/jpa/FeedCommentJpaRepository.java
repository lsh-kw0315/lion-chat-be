package com.lion.be.feed_comment.repository.persistence.jpa;

import com.lion.be.feed_comment.domain.dto.FeedCommentResponse;
import com.lion.be.feed_comment.domain.entity.FeedComment;
import com.lion.be.global.aop.ElapsedTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedCommentJpaRepository extends JpaRepository<FeedComment, Long> {

    @ElapsedTime
    @Query("SELECT NEW com.lion.be.feed_comment.domain.dto.FeedCommentResponse(" +
            "c.id, c.feed.id, c.content, c.createdAt, u.id, u.nickname, img.imageUrl"
            + ") " +
            "FROM FeedComment c " +
            "JOIN c.user u "+
            "LEFT JOIN u.userPhotos up ON up.orderIndex = 1 "+
            "LEFT JOIN up.image img "+
            "WHERE c.feed.id = :feedId "
            + "AND c.isDeleted = false "
            + "AND u.role != 'BANNED'")
    Slice<FeedCommentResponse> findAllByFeedId(@Param("feedId") Long feedId, Pageable pageable);

    Long countAllByFeed_Id(Long feedId);

    @Modifying
    @Query("UPDATE FeedComment c "
            + "SET c.isDeleted = true "
            + "WHERE c.id = :id")
    void softDeleteById(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE FeedComment c "
            + "SET c.likeCount = :likeCount "
            + "WHERE c.id = :id")
    void updateLikeCount(@Param("id") Long id, @Param("likeCount") long likeCount);

    @ElapsedTime
    @Query("SELECT NEW com.lion.be.feed_comment.domain.dto.FeedCommentResponse(" +
            "c.id, c.feed.id, c.content, c.createdAt, u.id, u.nickname, img.imageUrl"
            + ") " +
            "FROM FeedComment c " +
            "JOIN c.user u "+
            "LEFT JOIN u.userPhotos up ON up.orderIndex = 1 "+
            "LEFT JOIN up.image img "+
            "WHERE c.id = :commentId "
            + "AND c.isDeleted = false "
            + "AND u.role != 'BANNED'")
    FeedCommentResponse findCommentById(@Param("commentId") Long commentId);

    @Modifying(clearAutomatically = true)
    @Query("""
        update FeedComment c
        set c.isDeleted = true
        where c.feed.id = :feedId
    """)
    void softDeleteByFeedId(@Param("feedId") Long feedId);

    @ElapsedTime
    @Query("""
            SELECT NEW com.lion.be.feed_comment.domain.dto.FeedCommentResponse(
            c.id, c.feed.id, c.content, c.createdAt, u.id, u.nickname, img.imageUrl
            )
            FROM FeedComment c
            JOIN c.user u
            LEFT JOIN u.userPhotos up ON up.orderIndex = 1
            LEFT JOIN up.image img
            WHERE c.feed.id = :feedId and c.isDeleted = false and u.role != 'BANNED'
            """)
    Slice<FeedCommentResponse> findAllByFeedIdFirst(@Param("feedId")Long feedId, Pageable pageable);

    @ElapsedTime
    @Query("""
            SELECT NEW com.lion.be.feed_comment.domain.dto.FeedCommentResponse(
            c.id, c.feed.id, c.content, c.createdAt, u.id, u.nickname, img.imageUrl
            )
            FROM FeedComment c
            JOIN c.user u
            LEFT JOIN u.userPhotos up ON up.orderIndex = 1
            LEFT JOIN up.image img
            WHERE c.feed.id = :feedId and c.isDeleted = false and u.role != 'BANNED' and c.id > :lastId
            """)
    Slice<FeedCommentResponse> findAllByFeedIdAfter(@Param("feedId")Long feedId, @Param("lastId")Long lastId, Pageable pageable);

    @Query("SELECT c.feed.id, COALESCE(COUNT(c), 0) FROM FeedComment c WHERE c.feed.id IN :ids GROUP BY c.feed.id")
    List<Object[]> countFeeds(@Param("ids") List<Long> ids);

}
