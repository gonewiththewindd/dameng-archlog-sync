package org.gone.dameng.autogen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("PRODUCT_VENDOR")
public class ProductVendor {

    @TableId(type = IdType.ASSIGN_ID)
    private Long productId;
    private Long vendorId;
    private BigDecimal standardPrice;
    private BigDecimal lastPrice;
    private LocalDate lastDate;
    private Integer minQty;
    private Integer maxQty;
    private Integer onOrderQty;

}
