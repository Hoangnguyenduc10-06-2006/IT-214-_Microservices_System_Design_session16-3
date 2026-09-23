package com.example.productservice.repository;

import com.example.productservice.entity.ProductInventory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Repository
public class InventoryRepository {

    private static final Logger logger = LoggerFactory.getLogger(InventoryRepository.class);

    // Giả lập bảng product_inventory trong RDBMS
    private final Map<String, ProductInventory> database = new ConcurrentHashMap<>();


    @PostConstruct
    public void initData() {
        database.put("IPHONE15", new ProductInventory("IPHONE15", "iPhone 15", 100));
        database.put("SAMSUNG_S24", new ProductInventory("SAMSUNG_S24", "Samsung Galaxy S24", 200));
        database.put("MACBOOK_M3", new ProductInventory("MACBOOK_M3", "MacBook Pro M3", 50));
        database.put("AIRPODS_PRO", new ProductInventory("AIRPODS_PRO", "AirPods Pro 2", 300));
        database.put("IPAD_AIR", new ProductInventory("IPAD_AIR", "iPad Air M2", 75));

        logger.info("📦 Đã khởi tạo {} sản phẩm tồn kho trong database giả lập", database.size());
    }



    public ProductInventory findByProductId(String productId) {
        logger.info("🗄️ [DB QUERY] Truy vấn tồn kho sản phẩm '{}' từ Database", productId);

        // Giả lập độ trễ database (50ms)
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return database.get(productId);
    }



    public ProductInventory updateQuantity(String productId, Integer newQuantity) {
        logger.info("📝 [DB UPDATE] Cập nhật tồn kho sản phẩm '{}' → {} đơn vị", productId, newQuantity);

        ProductInventory inventory = database.get(productId);
        if (inventory == null) {
            throw new RuntimeException(
                    "Sản phẩm '" + productId + "' không tồn tại trong database"
            );
        }

        inventory.setQuantity(newQuantity);
        return inventory;
    }
}
