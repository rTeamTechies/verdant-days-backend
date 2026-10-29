package com.verdantdays.model.dto;

import com.verdantdays.model.Product;

public class ProductStockBalance {
    private Product product;
    private Integer balance;

    public ProductStockBalance(Product product, Integer balance) {
        this.product = product;
        this.balance = balance;
    }

    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Integer getBalance() { return balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
}
