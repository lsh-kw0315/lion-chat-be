package com.lion.be.feed_comment.repository;

import com.lion.be.feed_comment.domain.dto.FeedCommentResponse;
import com.lion.be.feed_comment.domain.dto.FeedCommentSaveResponse;
import com.lion.be.feed_comment.domain.entity.FeedComment;
import com.lion.be.feed_comment.domain.entity.QFeedComment;
import com.lion.be.feed_comment.repository.persistence.jpa.FeedCommentJpaRepository;
import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.image.domain.entity.QImage;
import com.lion.be.user.domain.Role;
import com.lion.be.user.domain.entity.QUser;
import com.lion.be.user.domain.entity.QUserPhoto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
@Slf4j
public class FeedCommentRepositoryImpl implements FeedCommentRepository {

    private final FeedCommentJpaRepository feedCommentJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Slice<FeedCommentResponse> fetchAllByFeedId(Long feedId, Pageable pageable) {
        return feedCommentJpaRepository.findAllByFeedId(feedId, pageable);
    }

    @Override
    public FeedCommentSaveResponse save(FeedComment feedComment) {
        FeedComment savedFeedComment = feedCommentJpaRepository.save(feedComment);
        return new FeedCommentSaveResponse(savedFeedComment.getId());
    }

    @Override
    public void deleteById(Long id) {
        feedCommentJpaRepository.softDeleteById(id);
    }

    @Override
    public void updateLikeCount(Long commentId, long likeCount) {
        feedCommentJpaRepository.updateLikeCount(commentId, likeCount);
    }

    @Override
    public FeedCommentResponse findCommentById(Long commentId) {
        return feedCommentJpaRepository.findCommentById(commentId);
    }

    @Override
    public void softDeleteByFeedId(Long feedId) {
        feedCommentJpaRepository.softDeleteByFeedId(feedId);
    }

    @Override
    public FeedComment fetchById(Long commentId) {
        return feedCommentJpaRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("TODO"));
    }

    @Override
    
    public Slice<FeedCommentResponse> fetchAllByFeedIdFirst(Long feedId, Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeedComment comment = QFeedComment.feedComment;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetIds =
                queryFactory.select(comment.id)
                        .from(comment)
                        .join(user).on(comment.user.id.eq(user.id),user.role.ne(Role.BANNED))
                        .where(comment.isDeleted.eq(false), comment.feed.id.eq(feedId))
                        .limit(limit)
                        .fetch();

        return getFeedCommentResponses(pageable, size, user, comment, userPhoto, image, targetIds);
    }

    @Override
    
    public Slice<FeedCommentResponse> fetchAllByFeedIdAfter(Long feedId, Long lastId, Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeedComment comment = QFeedComment.feedComment;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetIds =
                queryFactory.select(comment.id)
                        .from(comment)
                        .join(user).on(comment.user.id.eq(user.id), user.role.ne(Role.BANNED))
                        .where(comment.isDeleted.eq(false), comment.feed.id.eq(feedId), comment.id.gt(lastId))
                        .limit(limit)
                        .fetch();

        return getFeedCommentResponses(pageable, size, user, comment, userPhoto, image, targetIds);
    }

    @NotNull
    private Slice<FeedCommentResponse> getFeedCommentResponses(Pageable pageable, int size, QUser user, QFeedComment comment, QUserPhoto userPhoto, QImage image, List<Long> targetIds) {
        List<FeedCommentResponse> contents =
                queryFactory.select(Projections.constructor(FeedCommentResponse.class,
                        comment.id,
                        comment.feed.id,
                        comment.content,
                        comment.createdAt,
                        user.id,
                        user.nickname,
                        image.imageUrl.coalesce("https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png")
                        ))
                        .from(comment)
                        .join(user).on(comment.user.id.eq(user.id))
                        .leftJoin(userPhoto).on(user.id.eq(userPhoto.user.id), userPhoto.orderIndex.eq(1))
                        .leftJoin(image).on(userPhoto.image.id.eq(image.id))
                        .where(comment.id.in(targetIds))
                        .fetch();

        boolean hasNext = false;
        if(contents.size() > size){
            hasNext = true;
            contents.remove(size);
        }

        return new SliceImpl<>(contents, pageable, hasNext);
    }


    @Override
    public void batchUpdateFeedCommentLikeCount(List<Long> commentIds, List<Long> likeCounts) {
        // 배치 업데이트 로직 구현
        QFeedComment feedComment = QFeedComment.feedComment;

        // 배치 업데이트 로직 구현
        if(commentIds == null || likeCounts == null || commentIds.size() != likeCounts.size()) {
            log.debug("피드 댓글 좋아요 수 배치 업데이트 할 것이 없음");
            return;
        }

        CaseBuilder.Cases<Long, NumberExpression<Long>> likeCountCase =
                new CaseBuilder()
                        .when(feedComment.id.eq(commentIds.get(0)))
                        .then(likeCounts.get(0));

        for(int i = 1; i < commentIds.size(); i++) {
            Long commentId = commentIds.get(i);
            Long likeCount = likeCounts.get(i);
            likeCountCase.when(feedComment.id.eq(commentId)).then(likeCount);
        }


        queryFactory
                .update(feedComment)
                .set(feedComment.likeCount, likeCountCase.otherwise(feedComment.likeCount)) // 기존 좋아요 수를 유지
                .where(feedComment.id.in(commentIds))
                .execute();
    }

    @Override
    public Long countAllByFeed_id(Long feedId) {
        return feedCommentJpaRepository.countAllByFeed_Id(feedId);
    }

    @Override
    public List<Object[]> countFeeds(List<Long> ids) {
        return feedCommentJpaRepository.countFeeds(ids);
    }
}
