package com.lion.be.chat.room.domain.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Boolean isDeleted = false;

    private ZonedDateTime regDt = ZonedDateTime.now();

    @OneToMany(mappedBy = "chatRoom", orphanRemoval = true, cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ChatRoomUser> chatRoomUsers = new ArrayList<>();

    private String recentMessageContent;

    private ZonedDateTime recentMessageDt = ZonedDateTime.now();

    public void updateRecentMessage(String content, ZonedDateTime dt) {
        this.recentMessageContent = content;
        this.recentMessageDt = dt;
    }

    public void addUser(ChatRoomUser chatRoomUser) {
        chatRoomUsers.add(chatRoomUser);
    }
}
