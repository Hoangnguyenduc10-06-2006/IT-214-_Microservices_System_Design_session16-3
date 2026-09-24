package com.example.productservice.service;

import com.example.productservice.dto.ProductInventoryDTO;
import com.example.productservice.entity.ProductInventory;
import com.example.productservice.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Service
public class InventoryService {

    private static final Logger logger = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }




    @Cacheable(
            value = "inventory",
            key = "#productId",
            condition = "#productId != null && !#productId.isBlank()",
            unless = "#result == null"
    )
    public ProductInventoryDTO getInventory(String productId) {
        // Fail-fast validation — ném exception ngay nếu input không hợp lệ
        validateProductId(productId);

        logger.info(" Cache MISS cho tồn kho '{}' — truy vấn Database", productId);

        ProductInventory inventory = inventoryRepository.findByProductId(productId);

        if (inventory == null) {
            logger.warn(" Sản phẩm '{}' không tồn tại trong Database", productId);
            return null;
        }

    
        ProductInventoryDTO dto = new ProductInventoryDTO(
                inventory.getProductId(),
                inventory.getQuantity()
        );

        logger.info(" Đã lấy tồn kho từ DB: {} — sẽ được cache vào Redis", dto);
        return dto;
    }


  


    @CacheEvict(
            value = "inventory",
            key = "#productId"
    )
    public ProductInventoryDTO updateInventory(String productId, Integer newQuantity) {

        validateProductId(productId);
        validateQuantity(newQuantity);

        logger.info(" Cập nhật tồn kho '{}': {} đơn vị — Cache sẽ bị evict", productId, newQuantity);

     
        ProductInventory updated = inventoryRepository.updateQuantity(productId, newQuantity);

      
        logger.info(" Đã cập nhật tồn kho và evict cache cho sản phẩm '{}'", productId);

      
        return new ProductInventoryDTO(
                updated.getProductId(),
                updated.getQuantity()
        );
    }


    // =========================================================================
    // VALIDATION — Chặn dữ liệu không hợp lệ trước khi chạm DB/Cache
    // =========================================================================


    private void validateProductId(String productId) {
        if (productId == null || productId.isBlank()) {
            throw new IllegalArgumentException(
                    "productId không được null hoặc rỗng. Giá trị nhận được: '"
                            + productId + "'"
            );
        }
    }


    private void validateQuantity(Integer newQuantity) {
        if (newQuantity == null) {
            throw new IllegalArgumentException(
                    "Số lượng tồn kho không được null"
            );
        }
        if (newQuantity < 0) {
            throw new IllegalArgumentException(
                    "Số lượng tồn kho phải >= 0. Giá trị nhận được: " + newQuantity
                            + ". Không thể đặt tồn kho âm vì sẽ gây ra tình trạng bán hàng vượt quá thực tế."
            );
        }
    }
}
