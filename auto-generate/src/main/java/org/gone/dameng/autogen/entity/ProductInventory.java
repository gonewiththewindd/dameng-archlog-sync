package org.gone.dameng.autogen.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("PRODUCT_INVENTORY")
public class ProductInventory {

    private Integer productId;
    private Integer locationId;
    private Integer quantity;
}
