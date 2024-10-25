package com.supply.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FlashSaleDrugVO implements Serializable {

    private Long id;

    private Long userId;

    private String supplyName;

    private String drugName;

    private Integer number;

    private String beginTime;

    private String endTime;

    private Integer Status;//抢购状态，1为正在抢购，2为暂未开启

}
