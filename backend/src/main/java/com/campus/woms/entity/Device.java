package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 设备实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device")
public class Device extends BaseEntity {

    /** 设备编号 */
    private String deviceCode;

    /** 设备名称 */
    private String deviceName;

    /** 设备分类ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long categoryId;

    /** 品牌 */
    private String brand;

    /** 型号 */
    private String model;

    /** 存放位置 */
    private String location;

    /** 状态：0在线 1离线 2维修中 3报废 */
    private Integer status;

    /** 责任人ID */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long principalId;

    /** 使用人 */
    private String userName;


    /** 购入日期 */
    private LocalDate purchaseDate;

    /** 保修截止日期 */
    private LocalDate warrantyEndDate;

    /** 备注 */
    private String remark;

    /** 创建人ID（决定编辑/删除按钮归属）★ 加序列化：19位雪花ID不丢精度 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;



    /** 非表字段：分类名称（联查填充） */
    @TableField(exist = false)
    private String categoryName;
}
