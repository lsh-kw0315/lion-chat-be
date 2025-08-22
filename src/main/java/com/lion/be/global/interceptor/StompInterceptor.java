package com.lion.be.global.interceptor;

import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.global.util.JwtTokenProvider;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, String> redisTemplate;
    public final static String USER_ENDPOINT_KEY_PREFIX = "user_endpoint:";;
    public final static String SUBID_ENDPOINT_KEY_PREFIX = "subid_endpoint:";


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String jwtToken = accessor.getFirstNativeHeader("Authorization");
            log.info("CONNECT - Authorization Header: {}", jwtToken);

            if (jwtToken != null && jwtToken.startsWith("Bearer ")) {
                String token = jwtToken.substring(7);
                try {
                    // <<< 중요!!! 토큰 유효성 검사에서 발생하는 예외를 처리합니다.
                    if (jwtTokenProvider.validateToken(token)) {
                        Authentication authentication = jwtTokenProvider.getAuthentication(token);
                        accessor.setUser(authentication);
                        log.info("User '{}' connected. Session ID: {}", authentication.getName(),
                                accessor.getSessionId());
                    }
                } catch (ExpiredJwtException e) {
                    // <<< 중요!!! 토큰 만료 시, 클라이언트가 식별할 수 있는 에러 메시지와 함께 예외를 던집니다.
                    log.info("Expired JWT Token: {}", e.getMessage());
                    throw new MessageDeliveryException("JWT_EXPIRED");
                } catch (Exception e) {
                    // <<< 중요!!! 그 외 다른 JWT 관련 예외 처리
                    log.error("Invalid JWT Token: {}", e.getMessage());
                    throw new MessageDeliveryException("INVALID_TOKEN");
                }
            } else {
                // 토큰이 없는 경우
                throw new MessageDeliveryException("MISSING_TOKEN");
            }
        }else if(StompCommand.SUBSCRIBE.equals(accessor.getCommand())){
            log.info("Subscribe: {}", accessor);
            UserPrincipal principal = null;
            if(accessor.getUser() instanceof Authentication && ((Authentication) accessor.getUser()).getPrincipal() instanceof UserPrincipal) {
                principal = (UserPrincipal) ((Authentication) accessor.getUser()).getPrincipal();
            }

            if(principal != null){
                String key1 = USER_ENDPOINT_KEY_PREFIX + principal.getId();

                String key2 = SUBID_ENDPOINT_KEY_PREFIX + accessor.getFirstNativeHeader("id");

                redisTemplate.opsForSet().add(key1, accessor.getFirstNativeHeader("destination"));
                redisTemplate.opsForValue().set(key2, accessor.getFirstNativeHeader("destination"));
            }

        }else if(StompCommand.UNSUBSCRIBE.equals(accessor.getCommand())){
            log.info("UnSubscribe: {}", accessor);

            UserPrincipal principal = null;
            if(accessor.getUser() instanceof Authentication && ((Authentication) accessor.getUser()).getPrincipal() instanceof UserPrincipal) {
                principal = (UserPrincipal) ((Authentication) accessor.getUser()).getPrincipal();
            }

            if(principal != null){
                String key1 = USER_ENDPOINT_KEY_PREFIX + principal.getId();

                String key2 = SUBID_ENDPOINT_KEY_PREFIX + accessor.getFirstNativeHeader("id");

                String endPoint = redisTemplate.opsForValue().get(key2);
                redisTemplate.delete(key2);
                redisTemplate.opsForSet().remove(key1, endPoint);
            }
        }
        return message;
    }

}