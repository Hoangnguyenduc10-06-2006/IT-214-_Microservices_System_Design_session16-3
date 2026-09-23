package com.example.productservice.service;

import com.example.productservice.dto.ProductInventoryDTO;
import com.example.productservice.entity.ProductInventory;
import com.example.productservice.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;



@SpringBootTest
class InventoryServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("inventory");
        }
    }

    @Autowired
    private InventoryService inventoryService;

    @MockitoBean
    private InventoryRepository inventoryRepository;


    // =========================================================================
    // NHÓM 1: CACHE-ASIDE READ — Cache Miss → Cache Hit
    // =========================================================================

    @Nested
    @DisplayName("1. Cache-Aside Read Flow")
    class CacheAsideReadTests {

        @Test
        @DisplayName("Lần đọc đầu (cache miss) → truy vấn DB, lần đọc sau (cache hit) → không truy vấn DB")
        void testCacheMissThenCacheHit() {
            // Arrange
            String productId = "IPHONE15";
            ProductInventory mockInventory = new ProductInventory(productId, "iPhone 15", 100);
            when(inventoryRepository.findByProductId(productId)).thenReturn(mockInventory);

            // ===== LẦN ĐỌC 1: Cache MISS → truy vấn Database =====
            ProductInventoryDTO firstCall = inventoryService.getInventory(productId);

            assertNotNull(firstCall, "Lần đọc đầu phải trả về dữ liệu");
            assertEquals(productId, firstCall.getProductId());
            assertEquals(100, firstCall.getQuantity());
            verify(inventoryRepository, times(1)).findByProductId(productId);

            // ===== LẦN ĐỌC 2: Cache HIT → KHÔNG truy vấn Database =====
            ProductInventoryDTO secondCall = inventoryService.getInventory(productId);

            assertNotNull(secondCall, "Lần đọc thứ hai phải trả về dữ liệu từ cache");
            assertEquals(100, secondCall.getQuantity());
            // XÁC NHẬN: Repository vẫn chỉ được gọi 1 lần → chứng minh cache hit
            verify(inventoryRepository, times(1)).findByProductId(productId);

            // ===== LẦN ĐỌC 3: Vẫn Cache HIT =====
            ProductInventoryDTO thirdCall = inventoryService.getInventory(productId);
            assertNotNull(thirdCall);
            assertEquals(100, thirdCall.getQuantity());
            verify(inventoryRepository, times(1)).findByProductId(productId);
        }

        @Test
        @DisplayName("Sản phẩm không tồn tại → trả null, KHÔNG cache (nhờ unless)")
        void testProductNotFound_NotCached() {
            // Arrange
            String productId = "NONEXISTENT";
            when(inventoryRepository.findByProductId(productId)).thenReturn(null);

            // Lần đọc 1: Cache miss → query DB → null
            ProductInventoryDTO firstCall = inventoryService.getInventory(productId);
            assertNull(firstCall, "Sản phẩm không tồn tại phải trả về null");
            verify(inventoryRepository, times(1)).findByProductId(productId);

            // Lần đọc 2: null KHÔNG được cache → vẫn query DB
            ProductInventoryDTO secondCall = inventoryService.getInventory(productId);
            assertNull(secondCall);
            // Repository bị gọi 2 LẦN → chứng minh null không được cache
            verify(inventoryRepository, times(2)).findByProductId(productId);
        }
    }


    // =========================================================================
    // NHÓM 2: CACHE-ASIDE WRITE — Update DB → Evict Cache
    // =========================================================================

    @Nested
    @DisplayName("2. Cache-Aside Write Flow (Evict)")
    class CacheAsideWriteTests {

        @Test
        @DisplayName("Luồng hoàn chỉnh: Read (miss) → Read (hit) → Update (evict) → Read (miss lại)")
        void testFullCacheAsideFlow() {
            // Arrange
            String productId = "IPHONE15";
            ProductInventory originalInventory = new ProductInventory(productId, "iPhone 15", 100);
            ProductInventory updatedInventory = new ProductInventory(productId, "iPhone 15", 95);

            when(inventoryRepository.findByProductId(productId)).thenReturn(originalInventory);
            when(inventoryRepository.updateQuantity(productId, 95)).thenReturn(updatedInventory);

            // ===== BƯỚC 1: Đọc lần đầu → Cache MISS → lấy 100 từ DB =====
            ProductInventoryDTO read1 = inventoryService.getInventory(productId);
            assertEquals(100, read1.getQuantity());
            verify(inventoryRepository, times(1)).findByProductId(productId);

            // ===== BƯỚC 2: Đọc lần hai → Cache HIT → không gọi DB =====
            ProductInventoryDTO read2 = inventoryService.getInventory(productId);
            assertEquals(100, read2.getQuantity());
            verify(inventoryRepository, times(1)).findByProductId(productId); // vẫn 1 lần

            // ===== BƯỚC 3: Cập nhật tồn kho → 95 → DB được ghi, Cache bị evict =====
            // Chuẩn bị: sau khi evict, lần đọc tiếp theo sẽ lấy dữ liệu mới
            when(inventoryRepository.findByProductId(productId)).thenReturn(updatedInventory);

            ProductInventoryDTO writeResult = inventoryService.updateInventory(productId, 95);
            assertEquals(95, writeResult.getQuantity());
            verify(inventoryRepository, times(1)).updateQuantity(productId, 95);

            // ===== BƯỚC 4: Đọc lại → Cache MISS (đã bị evict) → lấy 95 từ DB =====
            ProductInventoryDTO read3 = inventoryService.getInventory(productId);
            assertEquals(95, read3.getQuantity());
            // findByProductId đã được gọi thêm 1 lần nữa (tổng = 2)
            verify(inventoryRepository, times(2)).findByProductId(productId);
        }

        @Test
        @DisplayName("Sau update, cache phải lấy giá trị mới từ DB (không dùng cache cũ)")
        void testCacheEvictAfterUpdate() {
            String productId = "SAMSUNG_S24";
            ProductInventory original = new ProductInventory(productId, "Samsung Galaxy S24", 200);
            ProductInventory updated = new ProductInventory(productId, "Samsung Galaxy S24", 180);

            when(inventoryRepository.findByProductId(productId)).thenReturn(original);
            when(inventoryRepository.updateQuantity(productId, 180)).thenReturn(updated);

            // Đọc → cache 200
            assertEquals(200, inventoryService.getInventory(productId).getQuantity());

            // Update → evict cache
            when(inventoryRepository.findByProductId(productId)).thenReturn(updated);
            inventoryService.updateInventory(productId, 180);

            // Đọc lại → cache miss → lấy 180 mới từ DB
            ProductInventoryDTO afterUpdate = inventoryService.getInventory(productId);
            assertEquals(180, afterUpdate.getQuantity(), "Phải lấy giá trị mới 180, không phải cache cũ 200");
        }
    }


    // =========================================================================
    // NHÓM 3: VALIDATION — Chặn dữ liệu không hợp lệ
    // =========================================================================

    @Nested
    @DisplayName("3. Input Validation")
    class ValidationTests {

        // ----- Tình huống 1: newQuantity âm -----

        @Test
        @DisplayName("TÌNH HUỐNG 1: newQuantity = -10 → ném IllegalArgumentException")
        void testNegativeQuantity_ThrowsException() {
            IllegalArgumentException ex = assertThrows(
                    IllegalArgumentException.class,
                    () -> inventoryService.updateInventory("IPHONE15", -10)
            );

            assertTrue(ex.getMessage().contains("-10"),
                    "Message phải chứa giá trị sai: " + ex.getMessage());
            assertTrue(ex.getMessage().contains(">= 0") || ex.getMessage().contains("âm"),
                    "Message phải giải thích yêu cầu: " + ex.getMessage());

            // Xác nhận: KHÔNG ghi vào Database khi quantity âm
            verify(inventoryRepository, never()).updateQuantity(any(), anyInt());
        }

        @Test
        @DisplayName("newQuantity = -1 (biên) → ném IllegalArgumentException")
        void testNegativeQuantity_BoundaryMinus1() {
            assertThrows(IllegalArgumentException.class,
                    () -> inventoryService.updateInventory("IPHONE15", -1));
            verify(inventoryRepository, never()).updateQuantity(any(), anyInt());
        }

        @Test
        @DisplayName("newQuantity = 0 (biên hợp lệ) → KHÔNG ném exception")
        void testZeroQuantity_Valid() {
            String productId = "IPHONE15";
            ProductInventory updated = new ProductInventory(productId, "iPhone 15", 0);
            when(inventoryRepository.updateQuantity(productId, 0)).thenReturn(updated);

            ProductInventoryDTO result = inventoryService.updateInventory(productId, 0);
            assertEquals(0, result.getQuantity());
            verify(inventoryRepository, times(1)).updateQuantity(productId, 0);
        }

        @Test
        @DisplayName("newQuantity = null → ném IllegalArgumentException")
        void testNullQuantity_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> inventoryService.updateInventory("IPHONE15", null));
            verify(inventoryRepository, never()).updateQuantity(any(), anyInt());
        }

        // ----- productId validation -----

        @Test
        @DisplayName("productId = null → ném IllegalArgumentException, không gọi DB")
        void testNullProductId_GetInventory() {
            assertThrows(IllegalArgumentException.class,
                    () -> inventoryService.getInventory(null));
            verify(inventoryRepository, never()).findByProductId(any());
        }

        @Test
        @DisplayName("productId = '' (rỗng) → ném IllegalArgumentException")
        void testEmptyProductId_GetInventory() {
            assertThrows(IllegalArgumentException.class,
                    () -> inventoryService.getInventory(""));
            verify(inventoryRepository, never()).findByProductId(any());
        }

        @Test
        @DisplayName("productId = '   ' (khoảng trắng) → ném IllegalArgumentException")
        void testBlankProductId_GetInventory() {
            assertThrows(IllegalArgumentException.class,
                    () -> inventoryService.getInventory("   "));
            verify(inventoryRepository, never()).findByProductId(any());
        }

        @Test
        @DisplayName("productId null khi update → ném IllegalArgumentException, không ghi DB")
        void testNullProductId_UpdateInventory() {
            assertThrows(IllegalArgumentException.class,
                    () -> inventoryService.updateInventory(null, 50));
            verify(inventoryRepository, never()).updateQuantity(any(), anyInt());
        }
    }


    // =========================================================================
    // NHÓM 4: CACHE ISOLATION — Các productId cache riêng biệt
    // =========================================================================

    @Nested
    @DisplayName("4. Cache Isolation")
    class CacheIsolationTests {

        @Test
        @DisplayName("Các productId khác nhau được cache riêng biệt")
        void testDifferentProductsCachedSeparately() {
            // Arrange
            ProductInventory iphone = new ProductInventory("IPHONE15", "iPhone 15", 100);
            ProductInventory samsung = new ProductInventory("SAMSUNG_S24", "Samsung S24", 200);

            when(inventoryRepository.findByProductId("IPHONE15")).thenReturn(iphone);
            when(inventoryRepository.findByProductId("SAMSUNG_S24")).thenReturn(samsung);

            // Đọc iPhone → cache miss
            assertEquals(100, inventoryService.getInventory("IPHONE15").getQuantity());
            // Đọc Samsung → cache miss (cache key khác)
            assertEquals(200, inventoryService.getInventory("SAMSUNG_S24").getQuantity());

            // Đọc lại → cả hai đều cache hit
            inventoryService.getInventory("IPHONE15");
            inventoryService.getInventory("SAMSUNG_S24");

            // Mỗi productId chỉ query DB đúng 1 lần
            verify(inventoryRepository, times(1)).findByProductId("IPHONE15");
            verify(inventoryRepository, times(1)).findByProductId("SAMSUNG_S24");
        }

        @Test
        @DisplayName("Evict một productId không ảnh hưởng cache của productId khác")
        void testEvictOneDoesNotAffectOthers() {
            // Arrange
            ProductInventory iphone = new ProductInventory("IPHONE15", "iPhone 15", 100);
            ProductInventory samsung = new ProductInventory("SAMSUNG_S24", "Samsung S24", 200);
            ProductInventory iphoneUpdated = new ProductInventory("IPHONE15", "iPhone 15", 95);

            when(inventoryRepository.findByProductId("IPHONE15")).thenReturn(iphone);
            when(inventoryRepository.findByProductId("SAMSUNG_S24")).thenReturn(samsung);
            when(inventoryRepository.updateQuantity("IPHONE15", 95)).thenReturn(iphoneUpdated);

            // Cache cả hai
            inventoryService.getInventory("IPHONE15");
            inventoryService.getInventory("SAMSUNG_S24");

            // Evict iPhone only
            when(inventoryRepository.findByProductId("IPHONE15")).thenReturn(iphoneUpdated);
            inventoryService.updateInventory("IPHONE15", 95);

            // Đọc Samsung → vẫn cache hit (không bị evict)
            inventoryService.getInventory("SAMSUNG_S24");
            verify(inventoryRepository, times(1)).findByProductId("SAMSUNG_S24"); // vẫn 1 lần

            // Đọc iPhone → cache miss (đã bị evict) → query DB lần 2
            assertEquals(95, inventoryService.getInventory("IPHONE15").getQuantity());
            verify(inventoryRepository, times(2)).findByProductId("IPHONE15"); // đã tăng lên 2
        }
    }


    // =========================================================================
    // NHÓM 5: SCENARIO TEST — Mô phỏng kịch bản thực tế
    // =========================================================================

    @Nested
    @DisplayName("5. Real-World Scenario")
    class ScenarioTests {

        @Test
        @DisplayName("Kịch bản đề bài: iPhone 15 tồn kho 100 → cập nhật 95 → đọc lại = 95")
        void testExactScenarioFromRequirement() {
            // === SETUP: iPhone 15 có tồn kho ban đầu = 100 ===
            String productId = "IPHONE15";
            ProductInventory initial = new ProductInventory(productId, "iPhone 15", 100);
            ProductInventory afterUpdate = new ProductInventory(productId, "iPhone 15", 95);

            when(inventoryRepository.findByProductId(productId)).thenReturn(initial);
            when(inventoryRepository.updateQuantity(productId, 95)).thenReturn(afterUpdate);

            // === BƯỚC 1: Người dùng xem sản phẩm → lấy 100 ===
            ProductInventoryDTO beforeUpdate = inventoryService.getInventory(productId);
            assertEquals(100, beforeUpdate.getQuantity(),
                    "Tồn kho ban đầu phải là 100");

            // === BƯỚC 2: Nhân viên kho cập nhật tồn kho lên 95 ===
            // Hệ thống: Ghi 95 vào DB → Xóa cache cũ
            when(inventoryRepository.findByProductId(productId)).thenReturn(afterUpdate);
            ProductInventoryDTO updateResult = inventoryService.updateInventory(productId, 95);
            assertEquals(95, updateResult.getQuantity());

            // === BƯỚC 3: Người dùng xem lại → Cache miss → Lấy 95 từ DB ===
            ProductInventoryDTO afterRead = inventoryService.getInventory(productId);
            assertEquals(95, afterRead.getQuantity(),
                    "Sau cập nhật, tồn kho phải là 95 (lấy từ DB qua cache miss)");

            // === BƯỚC 4: Xem lại lần nữa → Cache hit → Không query DB ===
            inventoryService.getInventory(productId);
            // findByProductId được gọi: 1 (initial) + 1 (after evict) = 2 lần
            verify(inventoryRepository, times(2)).findByProductId(productId);
        }
    }
}
