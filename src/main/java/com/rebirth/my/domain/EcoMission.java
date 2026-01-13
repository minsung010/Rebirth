package com.rebirth.my.domain;

import lombok.Data;

@Data
public class EcoMission {

	
	private Long id;
	private String title;
	private Integer rewardPoint;


// 화면 렌더링용 (오늘 체크 여부)
private boolean checked;
}
