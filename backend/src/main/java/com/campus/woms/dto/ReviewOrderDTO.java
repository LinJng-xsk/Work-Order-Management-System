package com.campus.woms.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewOrderDTO {

    private BigDecimal score;
    private String comment;

    @NotNull(message = "验收结果不能为空")
    private Boolean pass;
}
