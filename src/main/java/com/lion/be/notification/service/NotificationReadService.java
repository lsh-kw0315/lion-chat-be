package com.lion.be.notification.service;

import com.lion.be.global.aop.ElapsedTime;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.image.domain.entity.Image;
import com.lion.be.image.repository.ImageRepository;
import com.lion.be.notification.domain.dto.NotificationResponse;
import com.lion.be.notification.domain.entity.Notification;
import com.lion.be.notification.repository.NotificationRepository;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationReadService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ImageRepository imageRepository;

    @ElapsedTime
    public Slice<NotificationResponse> fetchAllAlarm(Long currentUserId, Long lastId, int size){
        size = size > 0 && size <=30 ? size : 30;
        int limit = size + 1;
        Pageable pageable = PageRequest.of(0, limit, Sort.by("id").descending());

        List<Notification> origins;
        if(lastId != null) {
            origins = notificationRepository.fetchAllSecond(currentUserId, lastId, pageable);
        }else{
            origins = notificationRepository.fetchAllFirst(currentUserId, pageable);
        }

        boolean hasNext = false;
        if(origins.size() > size){
            hasNext = true;
            origins.remove(size);
        }

        User receiver = userRepository.fetchById(currentUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        List<Long> userIds = origins.stream()
                .map(Notification::getFromUserId)
                .toList();

        Map<Long, String> userNicknameMap = new HashMap<>();
        List<User> users = userRepository.fetchAllUser(userIds);
        for(User user : users){
            userNicknameMap.put(user.getId(), user.getNickname());
        }


        Map<Long , String> userImageMap = new HashMap<>();

        List<Image> images = imageRepository.fetchAllByUserId(userIds);
        for(Image img : images){
            userImageMap.put(img.getUploaderId(), img.getImageUrl());
        }

        List<NotificationResponse> notificationResponses =
                origins.stream().map(
                        notification ->
                            NotificationResponse.toResponse(
                                notification.getId(),
                                notification.getFromUserId(),
                                userNicknameMap.get(notification.getFromUserId()),
                                notification.getToUserId(),
                                receiver.getNickname(),
                                notification.getType(),
                                notification.getCreatedAt(),
                                userImageMap.get(notification.getFromUserId()) != null ? userImageMap.get(notification.getFromUserId()) : "https://tokit-bucket.s3.ap-northeast-2.amazonaws.com/profile/defaultimage.png",
                                    notification.getTargetId()
                        )
                ).toList();

        return new SliceImpl<>(notificationResponses,  PageRequest.of(0, size, Sort.by("id").descending()), hasNext);

    }

}
