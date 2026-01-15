package com.lion.be.user.domain.entity;

import com.lion.be.feed.domain.entity.FeedLike;
import com.lion.be.feed_comment.domain.entity.FeedComment;
import java.util.ArrayList;
import java.util.List;

import com.lion.be.chat.room.domain.entity.ChatRoomUser;
import com.lion.be.feed.domain.entity.Feed;
import com.lion.be.global.entity.BaseEntity;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.image.domain.entity.Image;
import com.lion.be.user.domain.AgreementType;
import com.lion.be.user.domain.Gender;
import com.lion.be.user.domain.Mbti;
import com.lion.be.user.domain.OnboardingStatus;
import com.lion.be.user.domain.Position;
import com.lion.be.user.domain.PreferenceType;
import com.lion.be.user.domain.Role;
import com.lion.be.user.domain.University;
import com.lion.be.user.domain.entity.dto.OnboardingData;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "member")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

	@Column(unique = true)
	private String nickname;

    private String email;

    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private Role role;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChatRoomUser> chatRoomUsers = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("orderIndex ASC")
    @BatchSize(size = 10)
	private List<UserPhoto> userPhotos = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedComment> feedComments = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Feed> userFeeds = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<FeedLike> userFeedLikes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    private University university;

    @Enumerated(EnumType.STRING)
    private Position position;

    @Enumerated(EnumType.STRING)
    private Mbti mbti;

    @Enumerated(EnumType.STRING)
    private OnboardingStatus onboardingStatus = OnboardingStatus.PENDING;

    private String bio;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Agreements> agreements = new ArrayList<>();

    private Boolean isUniversityView;

    /**
     * K-means 클러스터링으로 배정된 클러스터 ID
     * - 유사한 성향(MBTI + Position)의 사용자들을 그룹핑
     * - 추천 시스템에서 같은 클러스터 내 사용자 우선 노출
     * - 온보딩 완료 시점에 자동 배정
     */
    @Column(name = "cluster_id")
    private Integer clusterId;

	@Enumerated(EnumType.STRING)
	private PreferenceType preferenceType;

    public User(String name, String email, String imageUrl, Role role) {
        this.name = name;
        this.email = email;
        this.imageUrl = imageUrl;
        this.role = role;
    }

    public String getRoleKey() {
        return this.role.getKey();
    }

    public void addChatRoomUser(ChatRoomUser chatRoomUser) {
        chatRoomUsers.add(chatRoomUser);
    }

    public void addUserFeed(Feed feed){ userFeeds.add(feed); }

    public void addUserFeedLike(FeedLike feedLike){userFeedLikes.add(feedLike);}

    public void removeUserFeedLike(FeedLike feedLike){userFeedLikes.remove(feedLike);}

    public void completeOnboarding(OnboardingData data) {
        validateOnboardingPreconditions();
        validateOnboardingData(data);
		this.nickname = data.nickname();
        this.gender = data.gender();
        this.university = data.university();
        this.position = data.position();
        this.mbti = data.mbti();
        this.bio = data.bio();
        this.agreements.add(new Agreements(this, AgreementType.REQUIRED, data.requiredAgreements()));
        this.agreements.add(new Agreements(this, AgreementType.MARKETING, data.marketingAgreements()));
        this.isUniversityView = data.isUniversityView();
		this.preferenceType = data.preferenceType();
        this.onboardingStatus = OnboardingStatus.COMPLETED;
    }

	public boolean isOnboardingCompleted() {
		return OnboardingStatusVO.from(this.onboardingStatus).isCompleted();
	}

    private void validateOnboardingPreconditions() {
        if (isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.USER_ONBOARDING_ALREADY_COMPLETED);
        }
        if (this.name == null || this.email == null) {
            throw new CustomException(ErrorCode.USER_PROFILE_INCOMPLETE);
        }
    }

    private void validateOnboardingData(OnboardingData data) {
		if (data.gender() == null || data.university() == null || data.position() == null ||
			data.mbti() == null) {
			throw new CustomException(ErrorCode.USER_ONBOARDING_PROFILE_INCOMPLETE);
		}
    }

    /**
     * 클러스터 ID를 배정합니다.
     * 온보딩 완료된 사용자에게만 배정 가능합니다.
     */
    public void assignToCluster(Integer clusterId) {
        if (!isOnboardingCompleted()) {
            throw new CustomException(ErrorCode.USER_ONBOARDING_NOT_COMPLETED);
        }
        if (clusterId == null || clusterId < 0) {
            throw new CustomException(ErrorCode.INVALID_CLUSTER_ID);
        }
        this.clusterId = clusterId;
    }

	public void addProfileImage(Image image, int orderIndex) {
		UserPhoto userPhoto = new UserPhoto(this, image, orderIndex);
		this.userPhotos.add(userPhoto);
	}
	public void updateRole(Role role) {
		this.role = role;
	}

	public void updateBio(String bio) {
		this.bio = bio;
	}

	public void updatePreferenceType(PreferenceType preferenceType) {
		this.preferenceType = preferenceType;
	}
}
