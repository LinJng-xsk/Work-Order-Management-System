package com.campus.woms.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitReviewDTO {

    @NotBlank(message = "维修方案不能为空")
    private String repairContent;

    private BigDecimal repairCost;
}
