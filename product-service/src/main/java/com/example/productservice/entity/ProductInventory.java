package com.example.productservice.entity;

import java.io.Serializable;


public class ProductInventory implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productId;
    private String productName;
    private Integer quantity;

    public ProductInventory() {
    }

    public ProductInventory(String productId, String productName, Integer quantity) {
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ProductInventory{productId='" + productId +
                "', productName='" + productName +
                "', quantity=" + quantity + "}";
    }
}
