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
public enum Mbti {
	ISTJ("ISTJ"),
	ISFJ("ISFJ"),
	INFJ("INFJ"),
	INTJ("INTJ"),
	ISTP("ISTP"),
	ISFP("ISFP"),
	INFP("INFP"),
	INTP("INTP"),
	ESTP("ESTP"),
	ESFP("ESFP"),
	ENFP("ENFP"),
	ENTP("ENTP"),
	ESTJ("ESTJ"),
	ESFJ("ESFJ"),
	ENTJ("ENTJ"),
	ENFJ("ENFJ");


	private final String koreanName;

	@JsonValue
	public String getKoreanName() {
		return koreanName;
	}

	@JsonCreator
	public static Mbti fromValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new CustomException(ErrorCode.MBTI_VALUE_INVALID);
		}

		// 한국어 이름으로 매칭 (MBTI는 영어와 한국어가 같음)
		return Arrays.stream(values())
			.filter(mbti -> mbti.getKoreanName().equals(value))
			.findFirst()
			.orElseGet(() -> {
				// 영어 이름으로도 시도
				try {
					return valueOf(value.toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new CustomException(ErrorCode.MBTI_VALUE_INVALID, e);
				}
			});
	}
}
