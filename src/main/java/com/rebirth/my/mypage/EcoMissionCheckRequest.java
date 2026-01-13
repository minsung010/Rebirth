package com.rebirth.my.mypage;

import lombok.Data;

@Data
public class EcoMissionCheckRequest {
    private Long taskId;
    private boolean checked;
}