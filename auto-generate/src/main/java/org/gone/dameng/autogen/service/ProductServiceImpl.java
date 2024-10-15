package org.gone.dameng.autogen.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.gone.dameng.autogen.entity.Product;
import org.gone.dameng.autogen.mapper.ProductMapper;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> {
}
