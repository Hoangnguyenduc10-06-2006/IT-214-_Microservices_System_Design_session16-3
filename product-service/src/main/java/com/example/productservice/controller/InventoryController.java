package com.example.productservice.controller;

import com.example.productservice.dto.ProductInventoryDTO;
import com.example.productservice.service.InventoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;



@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private static final Logger logger = LoggerFactory.getLogger(InventoryController.class);

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }



    @GetMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> getInventory(
            @PathVariable("productId") String productId) {

        logger.info("📥 [API] GET /api/inventory/{}", productId);

        ProductInventoryDTO inventory = inventoryService.getInventory(productId);

        if (inventory == null) {
            return ResponseEntity.notFound().build();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("productId", inventory.getProductId());
        response.put("quantity", inventory.getQuantity());
        response.put("message", "Lấy tồn kho thành công");

        return ResponseEntity.ok(response);
    }


    @PutMapping("/{productId}")
    public ResponseEntity<Map<String, Object>> updateInventory(
            @PathVariable("productId") String productId,
            @RequestParam("newQuantity") Integer newQuantity) {

        logger.info("📤 [API] PUT /api/inventory/{} — newQuantity={}", productId, newQuantity);

        ProductInventoryDTO updated = inventoryService.updateInventory(productId, newQuantity);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("productId", updated.getProductId());
        response.put("quantity", updated.getQuantity());
        response.put("message", "Tồn kho đã được cập nhật và cache đã bị xóa trên Redis");

        return ResponseEntity.ok(response);
    }
}
