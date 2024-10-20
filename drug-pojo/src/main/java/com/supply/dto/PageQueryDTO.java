package com.supply.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;

@Data
public class PageQueryDTO implements Serializable {

    @NotEmpty
    private Integer page;

    @NotEmpty
    private Integer pageSize;
}
