package org.gone.dameng.autogen.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("PRODUCT_REVIEW")
public class ProductReview {

    @TableId(type = IdType.ASSIGN_ID)
    private Long productReviewId;
    private Integer productId;
    private String name;
    private LocalDate reviewDate;
    private String email;
    private Integer rating;
    private String comments;
}
