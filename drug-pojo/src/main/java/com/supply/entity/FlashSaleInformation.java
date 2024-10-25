package com.supply.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Builder
@AllArgsConstructor
@Data
@NoArgsConstructor
public class FlashSaleInformation implements Serializable {

    private Long id;

    private Long userId;

    private Long timeOfDuration;

    private Long orderNumber;

    private LocalDateTime orderTime;
}
