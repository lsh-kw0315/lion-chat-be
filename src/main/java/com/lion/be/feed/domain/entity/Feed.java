package com.lion.be.feed.domain.entity;

import com.lion.be.feed_comment.domain.entity.FeedComment;
import com.lion.be.global.entity.BaseEntity;
import com.lion.be.user.domain.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Feed extends BaseEntity {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    private Boolean isDeleted;

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<FeedComment> feedComments = new ArrayList<>();

    @OneToMany(mappedBy = "feed", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    List<FeedLike> feedLikes = new ArrayList<>();

    private long likeCount;

    private long commentCount;

    public Feed(String title, String content, User user) {
        this.isDeleted = false; // 기본값 설정
        this.title = title;
        this.content = content;
        this.user = user;
        this.likeCount = 0L;
        this.commentCount = 0L;
    }

    public void delete() {
        this.isDeleted = true; // 삭제 상태로 변경
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void addComment(FeedComment comment) {
        feedComments.add(comment);
    }

    public void addLike(FeedLike like) {feedLikes.remove(like);}

    public void removeLike(FeedLike like) { feedLikes.remove(like);}
}
