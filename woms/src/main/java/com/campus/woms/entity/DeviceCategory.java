package com.campus.woms.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 设备分类实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("device_category")
public class DeviceCategory extends BaseEntity {

    /** 父分类ID，0为顶级 */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long parentId;

    /** 分类名称 */
    private String categoryName;

    /** 显示顺序 */
    private Integer sort;

    /** 状态：0正常 1停用 */
    private Integer status;

    /** 创建人ID（★ 加序列化：19位雪花ID不丢精度，前端才能 === 匹配） */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long creatorId;

    /** 非表字段：子分类（分类树） */
    @TableField(exist = false)
    private List<DeviceCategory> children;
}
