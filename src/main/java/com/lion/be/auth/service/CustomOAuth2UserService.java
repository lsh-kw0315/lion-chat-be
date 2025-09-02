package com.lion.be.auth.service;

import com.lion.be.auth.domain.OAuth2Attributes;
import com.lion.be.auth.domain.UserPrincipal;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;
import com.lion.be.user.domain.Role;
import com.lion.be.user.domain.entity.User;
import com.lion.be.user.service.UserConvertor;
import com.lion.be.user.service.UserReadService;
import com.lion.be.user.service.UserWriteService;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final UserReadService userReadService;
    private final UserWriteService userWriteService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2UserService<OAuth2UserRequest, OAuth2User> service = new DefaultOAuth2UserService();
        OAuth2User oAuth2User = service.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuth2Attributes attributes = OAuth2Attributes.of(registrationId, userNameAttributeName,
                oAuth2User.getAttributes());
        User user = login(attributes);

        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                Collections.singleton(new SimpleGrantedAuthority(user.getRoleKey())),
                oAuth2User.getAttributes()
        );
    }

    private User login(OAuth2Attributes attributes) {
        User user;

		try {
			// FIXME: 다른 oauth 플랫폼과 이메일이 같은 경우, 재로그인하도록 돌리기 (혹시나 시간이 없으면 카카오만)
			user = userReadService.fetchByEmail(attributes.getEmail());

			if(user.getRole() == Role.BANNED){
				log.info("차단된 유저가 로그인 시도 : {}", attributes.getEmail());
				throw new CustomException(ErrorCode.BANNED_USER_LOGIN);
			}

		} catch (CustomException e) {
			throw e;
		} catch (RuntimeException e) {
			user = UserConvertor.attributesToUser(attributes);
			userWriteService.save(user);
			log.info("{} 유저 최초 로그인.", attributes.getName());
		}

		return user;
	}
}
