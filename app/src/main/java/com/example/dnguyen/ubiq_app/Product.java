package com.example.dnguyen.ubiq_app;

/**
 * Created by dnguyen on 12.01.18.
 */

public class Product {
    String name;
    String price;
    String product_type;

    public Product(String name, String price, String product_type ) {
        this.name=name;
        this.price=price;
        this.product_type=product_type;

    }

    public String getName() {
        return name;
    }

    public String getPrice() {
        return price;
    }

    public String getProduct_type() {
        return product_type;
    }

}
