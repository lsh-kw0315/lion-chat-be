package com.lion.be.user.domain;

import java.util.Arrays;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.lion.be.global.exception.CustomException;
import com.lion.be.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Position {
	FRONTEND("프론트엔드"),
	BACKEND("백엔드"),
	FULLSTACK("풀스택"),
	AI("AI"),
	UX_UI("디자인"),
	PM("PM")
	;

	private final String koreanName;

	@JsonValue
	public String getKoreanName() {
		return koreanName;
	}

	@JsonCreator
	public static Position fromValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new CustomException(ErrorCode.POSITION_VALUE_INVALID);
		}

		// 한국어 이름으로 매칭
		return Arrays.stream(values())
			.filter(position -> position.getKoreanName().equals(value))
			.findFirst()
			.orElseGet(() -> {
				// 영어 이름으로도 시도
				try {
					return valueOf(value.toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new CustomException(ErrorCode.POSITION_VALUE_INVALID, e);
				}
			});
	}
}
