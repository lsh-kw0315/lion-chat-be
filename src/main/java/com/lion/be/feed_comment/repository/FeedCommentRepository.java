package com.lion.be.feed_comment.repository;

import com.lion.be.feed_comment.domain.dto.FeedCommentResponse;
import com.lion.be.feed_comment.domain.dto.FeedCommentSaveResponse;
import com.lion.be.feed_comment.domain.entity.FeedComment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedCommentRepository {

    Slice<FeedCommentResponse> fetchAllByFeedId(Long feedId, Pageable pageable);

    FeedCommentSaveResponse save(FeedComment feedComment);

    void deleteById(Long id);

    void updateLikeCount(Long commentId, long likeCount);

    FeedCommentResponse findCommentById(Long commentId);

    void softDeleteByFeedId(Long feedId);

    FeedComment fetchById(Long commentId);

    void batchUpdateFeedCommentLikeCount(List<Long> commentIds, List<Long> likeCounts);

    Slice<FeedCommentResponse> fetchAllByFeedIdFirst(Long feedId, Pageable pageable);

    Slice<FeedCommentResponse> fetchAllByFeedIdAfter(Long feedId, Long lastId, Pageable pageable);

    Long countAllByFeed_id(Long feedId);

    List<Object[]> countFeeds(List<Long> ids);
}
