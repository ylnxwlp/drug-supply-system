package com.supply.entity;


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
public class FlashSaleDrug implements Serializable {

    private Long id;

    private Long userId;

    private String drugName;

    private Integer number;

    private LocalDateTime beginTime;

    private LocalDateTime endTime;

}
