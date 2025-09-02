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
public enum PreferenceType {
	POSITION_FOCUSED("직무 관련"),
	CAREER_FOCUSED("취업 준비"),
	PREFERENCE_FOCUSED("일상 이야기");

	@JsonValue
	private final String koreanName;

	@JsonCreator
	public static PreferenceType fromValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new CustomException(ErrorCode.PREFERENCE_TYPE_VALUE_INVALID);
		}

		// 한국어 이름으로 매칭
		return Arrays.stream(values())
			.filter(preference -> preference.getKoreanName().equals(value))
			.findFirst()
			.orElseGet(() -> {
				// 영어 이름으로도 시도
				try {
					return valueOf(value.toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new CustomException(ErrorCode.PREFERENCE_TYPE_VALUE_INVALID, e);
				}
			});
	}
}
