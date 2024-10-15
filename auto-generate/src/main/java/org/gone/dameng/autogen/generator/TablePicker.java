package org.gone.dameng.autogen.generator;

import org.gone.dameng.autogen.entity.*;

import java.util.Random;

public class TablePicker {

    private static Class[] classes = {Product.class, ProductCategory.class, ProductInventory.class, ProductReview.class, ProductSubCategory.class, ProductVendor.class};

    public static final Random RANDOM = new Random();

    public static Class pick() {
        return classes[RANDOM.nextInt(classes.length)];
    }

}
