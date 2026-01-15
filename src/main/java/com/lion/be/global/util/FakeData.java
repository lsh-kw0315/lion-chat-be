package com.lion.be.global.util;

import com.lion.be.feed.domain.entity.Feed;
import com.lion.be.feed.domain.entity.FeedLike;
import com.lion.be.feed.repository.FeedRepository;
import com.lion.be.feed.service.FeedLikeService;
import com.lion.be.feed_comment.domain.dto.FeedCommentSaveRequest;
import com.lion.be.feed_comment.domain.entity.FeedComment;
import com.lion.be.feed_comment.repository.FeedCommentRepository;
import com.lion.be.feed_comment.repository.persistence.jpa.FeedCommentJpaRepository;
import com.lion.be.feed_comment.service.FeedCommentLikeService;
import com.lion.be.feed_comment.service.FeedCommentWriteService;
import com.lion.be.user.domain.Role;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.repository.UserRepository;
import com.lion.be.user.repository.persistence.jpa.UserJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Profile("local")
public class FakeData implements CommandLineRunner {

    private final UserJpaRepository userJpaRepository;
    private final FeedRepository feedRepository;
    private final FeedLikeService feedLikeService;
    private final FeedCommentLikeService feedCommentLikeService;
    private final FeedCommentJpaRepository feedCommentJpaRepository;
    private final FeedCommentWriteService feedCommentWriteService;

    private static final int USER_COUNT = 1500;
    private static final int FEED_COUNT = 15000;

    @Override
    public void run(String... args) throws Exception {
        List<User> existingUsers = userJpaRepository.findAll();
        int existingUserCount = existingUsers.size();

        if(existingUserCount < USER_COUNT) {
            for (int i = 0; i < USER_COUNT -  existingUserCount; i++) {
                existingUsers.add(userJpaRepository.save(
                        new User(
                                "user" + (i + 1),
                                "user" + (i + 1) + "@example.com",
                                "https://www",
                                Role.USER
                        ))
                );
            }
        }

        List<Feed> existingFeeds = feedRepository.findAll();

//        int existingFeedCount = existingFeeds.size();
//        if(existingFeedCount < FEED_COUNT) {
//            for (int i = 0; i < FEED_COUNT - existingFeedCount; i++) {
//                User user = existingUsers.get(i % USER_COUNT);
//                existingFeeds.add(
//                        feedRepository.save(
//                        new Feed("제목" + (i + 1), "내용" + (i + 1), user))
//                );
//            }
//        }
//
//        List<FeedComment> existingComments = feedCommentJpaRepository.findAll();



//        Random random = new Random();
//        random.setSeed(System.currentTimeMillis());
//        for (int i=existingFeeds.size()-1; i>=existingFeeds.size()-201; i--) {
//            Feed feed = existingFeeds.get(i);
//            if(feed.getLikeCount() > 0 || feed.getIsDeleted()) continue;
//            int rand = (random.nextInt(existingUserCount) % 5) + 1;
//            for (int j = 0; j < rand; j++) {
//                User user = existingUsers.get(j);
//                feedLikeService.likeFeedRDB(feed.getId(), user.getId());
//            }
//        }


//        Random random = new Random();
//        random.setSeed(System.currentTimeMillis());
//        for(int i=0; i<100; i++){
//            Feed feed = existingFeeds.get(i);
//            if(feed.getIsDeleted()) continue;
////            int rand = (random.nextInt(existingUserCount) % 10) + 1;
//            for(int j=0; j<10; j++){
//                User user = existingUsers.get(j);
//                feedCommentWriteService.save(
//                        feed.getId(),
//                        user.getId(),
//                        new FeedCommentSaveRequest(
//                                "댓글 내용" + (i + 1) + "-" + (j + 1)
//                        )
//                );
//            }
//        }

//        Random random = new Random();
//        random.setSeed(System.currentTimeMillis());
//        for(int i=0; i<100; i++){
//            FeedComment comment = existingComments.get(i);
//            if(comment.isDeleted()) continue;
//            int rand = (random.nextInt(existingUserCount) % 10) + 1;
//            for(int j=0; j<rand; j++){
//                User user = existingUsers.get(j);
//                feedCommentLikeService.likeComment(comment.getId(), user.getId());
//            }
//        }


    }
}
