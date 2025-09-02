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
public enum University {
	PROFESSIONAL("현직자"),
	JOBLESS("취업준비생"),
	CATHOLIC("가톨릭대학교"),
	KANGNAM("강남대학교"),
	KYUNGPOOK("경북대학교"),
	KEIMYUNG("계명대학교"),
	KOREA_SEJONG("고려대학교(세종)"),
	KWANGWOON("광운대학교"),
	KOOKMIN("국민대학교"),
	KUMOH("금오공과대학교"),
	NAMSEOUL("남서울대학교"),
	DUKSUNG("덕성여자대학교"),
	DONGGUK("동국대학교"),
	DONGDUK("동덕여자대학교"),
	MYONGJI_HUMANITIES("명지대학교(인문)"),
	BAEKSEOK("백석대학교"),
	SAHMYOOK("삼육대학교"),
	SANGMYUNG_SEOUL("상명대학교(서울)"),
	SANGMYUNG_CHEONAN("상명대학교(천안)"),
	SOGANG("서강대학교"),
	SEOKYEONG("서경대학교"),
	SEOULTECH("서울과학기술대학교"),
	SEOUL("서울대학교"),
	SWOMEN("서울여자대학교"),
	SUNGKYUL("성결대학교"),
	SKHU("성공회대학교"),
	SKKU("성균관대학교"),
	SUNGSHIN("성신여자대학교"),
	SOOKMYUNG("숙명여자대학교"),
	SUNCHON("순천대학교"),
	SCH("순천향대학교"),
	SSU("숭실대학교"),
	YONSEI_SINCHON("연세대학교(신촌)"),
	YEUNGNAM("영남대학교"),
	EULJI_SEONGNAM("을지대학교(성남)"),
	EWHA("이화여자대학교"),
	INCHEON("인천대학교"),
	INHA("인하대학교"),
	JOONGBU_GOYANG("중부대학교(고양)"),
	CAU("중앙대학교"),
	CHEONGJU("청주대학교"),
	CNU("충남대학교"),
	KUTC("한국교통대학교(충주)"),
	HUFS_GLOBAL("한국외국어대학교(글로벌)"),
	HUFS_SEOUL("한국외국어대학교(서울)"),
	KAU("한국항공대학교"),
	HNU("한남대학교"),
	HANDONG("한동대학교"),
	HANBAT("한밭대학교"),
	HANSEO("한서대학교"),
	HANSUNG("한성대학교"),
	HANYANG_ERICA("한양대학교(ERICA)"),
	HONGIK("홍익대학교");

	private final String koreanName;

	@JsonValue
	public String getKoreanName() {
		return koreanName;
	}

	@JsonCreator
	public static University fromValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			throw new CustomException(ErrorCode.UNIVERSITY_VALUE_INVALID);
		}

		// 한국어 이름으로 매칭
		return Arrays.stream(values())
			.filter(university -> university.getKoreanName().equals(value))
			.findFirst()
			.orElseGet(() -> {
				// 영어 이름으로도 시도
				try {
					return valueOf(value.toUpperCase());
				} catch (IllegalArgumentException e) {
					throw new CustomException(ErrorCode.UNIVERSITY_VALUE_INVALID, e);
				}
			});
	}
}
