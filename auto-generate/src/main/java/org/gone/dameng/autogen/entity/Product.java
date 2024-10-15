package org.gone.dameng.autogen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("PRODUCT")
public class Product {

    @TableId(type = IdType.ASSIGN_ID)
    private Long productId;
    private String name;
    private String author;
    private String publisher;
    private LocalDate publishTime;
    private Integer productSubcategoryId;
    private String productNo;
    private Integer safetyStockLevel;
    private BigDecimal originalPrice;
    private BigDecimal nowPrice;
    private BigDecimal discount;
    private String description;
    private byte[] photo;
    private String type;
    private Integer paperTotal;
    private Integer wordTotal;
    private LocalDate sellStartTime;
    private LocalDate sellEndTime;
}
