package com.example.productservice.dto;

import java.io.Serializable;


public class ProductInventoryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String productId;
    private Integer quantity;

    public ProductInventoryDTO() {
    }

    public ProductInventoryDTO(String productId, Integer quantity) {
        this.productId = productId;
        this.quantity = quantity;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    @Override
    public String toString() {
        return "ProductInventoryDTO{productId='" + productId + "', quantity=" + quantity + "}";
    }
}
