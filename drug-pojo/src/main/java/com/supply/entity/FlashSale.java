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
public class FlashSale implements Serializable {

    private Long id;

    private Long flashSaleDrugId;

    private Long userId;

    private String orderNumber;

    private LocalDateTime orderTime;

    private Integer status;

}
