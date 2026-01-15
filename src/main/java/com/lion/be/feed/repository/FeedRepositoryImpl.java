package com.lion.be.feed.repository;

import com.lion.be.feed.domain.dto.FeedResponse;
import com.lion.be.feed.domain.entity.FeedLike;
import com.lion.be.feed.domain.entity.QFeed;
import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.image.domain.entity.QImage;
import com.lion.be.user.domain.Role;
import com.lion.be.user.domain.entity.QUser;
import com.lion.be.user.domain.entity.QUserPhoto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.*;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;


import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final JPAQueryFactory queryFactory;


    // 배치 업데이트를 위한 메서드 구현
    @Override
    public void batchUpdateFeedLikeCount(List<Long> feedIds, List<Long> likeCounts) {
        QFeed feed = QFeed.feed;

        // 배치 업데이트 로직 구현
        if(feedIds == null || likeCounts == null || feedIds.size() != likeCounts.size()) {
            log.debug("피드 좋아요 배치 업데이트 할 것이 없음");
            return;
        }

        CaseBuilder.Cases<Long, NumberExpression<Long>> likeCountCase =
                new CaseBuilder()
                        .when(feed.id.eq(feedIds.get(0)))
                        .then(likeCounts.get(0));

        for(int i = 1; i < feedIds.size(); i++) {
            Long feedId = feedIds.get(i);
            Long likeCount = likeCounts.get(i);
            likeCountCase.when(feed.id.eq(feedId)).then(likeCount);
        }


        queryFactory
                .update(feed)
                .set(feed.likeCount, likeCountCase.otherwise(feed.likeCount)) // 기존 좋아요 수를 유지
                .where(feed.id.in(feedIds))
                .execute();
    }

    @Override
    public void batchUpdateFeedCommentCount(List<Long> feedIds, List<Long> commentCounts) {
        // 배치 업데이트 로직 구현
        QFeed feed = QFeed.feed;

        // 배치 업데이트 로직 구현
        if(feedIds == null || commentCounts == null || feedIds.size() != commentCounts.size()) {
            log.debug("피드 좋아요 배치 업데이트 할 것이 없음");
            return;
        }

        CaseBuilder.Cases<Long, NumberExpression<Long>> commentCountCase =
                new CaseBuilder()
                        .when(feed.id.eq(feedIds.get(0)))
                        .then(commentCounts.get(0));

        for(int i = 1; i < feedIds.size(); i++) {
            Long feedId = feedIds.get(i);
            Long commentCount = commentCounts.get(i);
            commentCountCase.when(feed.id.eq(feedId)).then(commentCount);
        }


        queryFactory
                .update(feed)
                .set(feed.commentCount, commentCountCase.otherwise(feed.commentCount)) // 기존 좋아요 수를 유지
                .where(feed.id.in(feedIds))
                .execute();
    }

    
    @Override
    public Slice<FeedResponse> fetchRecentFeedsFirst(Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeed feed = QFeed.feed;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetFeedIds =
                queryFactory.select(feed.id)
                        .from(feed)
                        .join(user).on(feed.user.id.eq(user.id), user.role.ne(Role.BANNED))
                        .where(feed.isDeleted.eq(false))
                        .orderBy(feed.id.desc())
                        .limit(limit)
                        .fetch();

        return getFeedResponses(pageable, size, user, feed, userPhoto, image, targetFeedIds);
    }


    @Override
    
    public Slice<FeedResponse> fetchRecentFeedsAfter(Long lastId, Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeed feed = QFeed.feed;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetFeedIds =
                queryFactory.select(feed.id)
                        .from(feed)
                        .join(user).on(feed.user.id.eq(user.id), user.role.ne(Role.BANNED))
                        .where(feed.isDeleted.eq(false), feed.id.lt(lastId))
                        .orderBy(feed.id.desc())
                        .limit(limit)
                        .fetch();

        return getFeedResponses(pageable, size, user, feed, userPhoto, image, targetFeedIds);

    }

    @Override
    
    public Slice<FeedResponse> fetchHotFeedsFirst(Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeed feed = QFeed.feed;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetFeedIds =
                queryFactory.select(feed.id)
                        .from(feed)
                        .join(user).on(feed.user.id.eq(user.id), user.role.ne(Role.BANNED))
                        .where(feed.isDeleted.eq(false))
                        .orderBy(feed.likeCount.desc(),feed.id.desc())
                        .limit(limit)
                        .fetch();

        return getHotFeedResponses(pageable, size, user, feed, userPhoto, image, targetFeedIds);
    }


    @Override
    
    public Slice<FeedResponse> fetchHotFeedsAfter(Long lastLikeCount, Long lastId, Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeed feed = QFeed.feed;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetFeedIds =
                queryFactory.select(feed.id)
                        .from(feed)
                        .join(user).on(feed.user.id.eq(user.id), user.role.ne(Role.BANNED))
                        .where(feed.isDeleted.eq(false).and(feed.likeCount.lt(lastLikeCount).or(feed.likeCount.eq(lastLikeCount).and(feed.id.lt(lastId))))
                        )
                        .orderBy(feed.likeCount.desc(),feed.id.desc())
                        .limit(limit)
                        .fetch();

        return getHotFeedResponses(pageable, size, user, feed, userPhoto, image, targetFeedIds);
    }


    @Override
    
    public Slice<FeedResponse> fetchFeedsByUserIdFirst(Long currentUserId, Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeed feed = QFeed.feed;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetFeedIds =
                queryFactory.select(feed.id)
                        .from(feed)
                        .join(user).on(feed.user.id.eq(user.id), user.role.ne(Role.BANNED), user.id.eq(currentUserId))
                        .where(feed.isDeleted.eq(false))
                        .orderBy(feed.id.desc())
                        .limit(limit)
                        .fetch();

        return getFeedResponses(pageable, size, user, feed, userPhoto, image, targetFeedIds);
    }

    @Override
    
    public Slice<FeedResponse> fetchFeedsByUserIdAfter(Long currentUserId, Long lastId, Pageable pageable) {
        int size = pageable.getPageSize();
        int limit = size + 1;
        QUser user = QUser.user;
        QFeed feed = QFeed.feed;
        QUserPhoto userPhoto = QUserPhoto.userPhoto;
        QImage image = QImage.image;

        List<Long> targetFeedIds =
                queryFactory.select(feed.id)
                        .from(feed)
                        .join(user).on(feed.user.id.eq(user.id), user.role.ne(Role.BANNED), user.id.eq(currentUserId))
                        .where(feed.isDeleted.eq(false), feed.id.lt(lastId))
                        .orderBy(feed.id.desc())
                        .limit(limit)
                        .fetch();
        return getFeedResponses(pageable, size, user, feed, userPhoto, image, targetFeedIds);
    }

    @NotNull
    private Slice<FeedResponse> getHotFeedResponses(Pageable pageable, int size, QUser user, QFeed feed, QUserPhoto userPhoto, QImage image, List<Long> targetFeedIds) {
        List<FeedResponse> contents =
                queryFactory.select(
                                Projections.constructor(FeedResponse.class,
                                        feed.id,
                                        feed.title,
                                        feed.content,
                                        feed.createdAt,
                                        feed.likeCount,
                                        feed.commentCount,
                                        user.nickname,
                                        user.id,
                                        image.imageUrl.coalesce("https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png"))
                        ).from(feed)
                        .join(user).on(feed.user.id.eq(user.id))
                        .leftJoin(userPhoto).on(user.id.eq(userPhoto.user.id), userPhoto.orderIndex.eq(1))
                        .leftJoin(image).on(userPhoto.image.id.eq(image.id))
                        .where(feed.id.in(targetFeedIds))
                        .orderBy(feed.likeCount.desc(),feed.id.desc())
                        .fetch();

        boolean hasNext = false;
        if(contents.size() > size){
            hasNext = true;
            contents.remove(size);
        }

        return new SliceImpl<>(contents, pageable, hasNext);
    }

    @NotNull
    private Slice<FeedResponse> getFeedResponses(Pageable pageable, int size, QUser user, QFeed feed, QUserPhoto userPhoto, QImage image, List<Long> targetFeedIds) {
        List<FeedResponse> contents =
                queryFactory.select(
                                Projections.constructor(FeedResponse.class,
                                        feed.id,
                                        feed.title,
                                        feed.content,
                                        feed.createdAt,
                                        feed.likeCount,
                                        feed.commentCount,
                                        user.nickname,
                                        user.id,
                                        image.imageUrl.coalesce("https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png"))
                        ).from(feed)
                        .join(user).on(feed.user.id.eq(user.id))
                        .leftJoin(userPhoto).on(user.id.eq(userPhoto.user.id), userPhoto.orderIndex.eq(1))
                        .leftJoin(image).on(userPhoto.image.id.eq(image.id))
                        .where(feed.id.in(targetFeedIds))
                        .orderBy(feed.id.desc())
                        .fetch();


        boolean hasNext = false;
        if(contents.size() > size){
            hasNext = true;
            contents.remove(size);
        }

        return new SliceImpl<>(contents, pageable, hasNext);
    }


}
