package com.supply.dto;


import jakarta.validation.constraints.NotEmpty;
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
public class FlashSaleDrugDTO implements Serializable {

    @NotEmpty
    private String drugName;

    @NotEmpty
    private Integer number;

    @NotEmpty
    private String beginTime;

    @NotEmpty
    private String endTime;

}
