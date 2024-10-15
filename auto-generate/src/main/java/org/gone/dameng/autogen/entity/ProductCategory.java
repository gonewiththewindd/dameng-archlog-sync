package org.gone.dameng.autogen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("PRODUCT_CATEGORY")
public class ProductCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long productCategoryId;
    private String name;
}
