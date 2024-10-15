package org.gone.dameng.autogen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("PRODUCT_SUBCATEGORY")
public class ProductSubCategory {

    @TableId(type = IdType.ASSIGN_ID)
    private Long productSubCategoryId;
    private Integer productCategoryId;
    private String name;
}
