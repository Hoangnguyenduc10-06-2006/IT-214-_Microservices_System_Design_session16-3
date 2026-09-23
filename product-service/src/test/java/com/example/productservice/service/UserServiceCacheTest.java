package com.example.productservice.service;

import com.example.productservice.entity.User;
import com.example.productservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@SpringBootTest
class UserServiceCacheTest {

    @Configuration
    @EnableCaching
    static class TestCacheConfig {
        @Bean
        public CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("users");
        }
    }

    @Autowired
    private UserService userService;

    @MockitoBean
    private UserRepository userRepository;

    // =========================================================================
    // TEST 1: Cache Miss → Cache Hit
    // =========================================================================

    @Test
    @DisplayName("Lần gọi đầu tiên (cache miss) phải truy vấn DB, lần thứ hai (cache hit) không truy vấn DB")
    void testCacheMissAndCacheHit() {
        // Arrange: Mock repository trả về User khi query DB
        String userId = "U001";
        User mockUser = new User(userId, "Nguyễn Văn An", "an.nguyen@bank.vn", "0901234567");
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // ===== LẦN GỌI THỨ 1: Cache MISS — phải truy vấn Database =====
        User firstCall = userService.getUserById(userId);

        assertNotNull(firstCall, "Lần gọi đầu tiên phải trả về User");
        assertEquals("Nguyễn Văn An", firstCall.getFullName());
        // Xác nhận Repository (Database) đã được gọi 1 lần
        verify(userRepository, times(1)).findById(userId);

        // ===== LẦN GỌI THỨ 2: Cache HIT — KHÔNG truy vấn Database =====
        User secondCall = userService.getUserById(userId);

        assertNotNull(secondCall, "Lần gọi thứ hai phải trả về User từ cache");
        assertEquals("Nguyễn Văn An", secondCall.getFullName());
        // XÁC NHẬN QUAN TRỌNG: Repository vẫn chỉ được gọi 1 lần (không gọi thêm)
        // Điều này chứng minh lần thứ 2 lấy kết quả từ cache, không query DB
        verify(userRepository, times(1)).findById(userId);

        // ===== LẦN GỌI THỨ 3: Vẫn Cache HIT =====
        User thirdCall = userService.getUserById(userId);
        assertNotNull(thirdCall);
        // Repository vẫn chỉ được gọi đúng 1 lần kể từ đầu
        verify(userRepository, times(1)).findById(userId);
    }

    // =========================================================================
    // TEST 2: userId null → IllegalArgumentException
    // =========================================================================

    @Test
    @DisplayName("userId null phải ném IllegalArgumentException — fail-fast, không tạo key rác")
    void testGetUserById_NullUserId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById(null);
        });

        // Xác nhận: KHÔNG gọi Database khi userId null
        verify(userRepository, never()).findById(any());
    }

    // =========================================================================
    // TEST 3: userId rỗng → IllegalArgumentException
    // =========================================================================

    @Test
    @DisplayName("userId rỗng phải ném IllegalArgumentException — fail-fast, không tạo key rác")
    void testGetUserById_EmptyUserId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById("");
        });

        verify(userRepository, never()).findById(any());
    }

    @Test
    @DisplayName("userId chỉ có khoảng trắng phải ném IllegalArgumentException")
    void testGetUserById_BlankUserId_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            userService.getUserById("   ");
        });

        verify(userRepository, never()).findById(any());
    }

    // =========================================================================
    // TEST 4: User không tồn tại → trả null, KHÔNG cache (nhờ unless)
    // =========================================================================

    @Test
    @DisplayName("User không tồn tại → trả về null, không lưu vào cache (unless = '#result == null')")
    void testGetUserById_UserNotFound_NotCached() {
        // Arrange: Mock repository trả về empty (user không tồn tại)
        String userId = "U999";
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Lần gọi 1: Cache miss → query DB → trả null
        User firstCall = userService.getUserById(userId);
        assertNull(firstCall, "User không tồn tại phải trả về null");
        verify(userRepository, times(1)).findById(userId);

        // Lần gọi 2: Vì null KHÔNG được cache (unless), nên vẫn query DB
        User secondCall = userService.getUserById(userId);
        assertNull(secondCall);
        // Repository bị gọi 2 LẦN — chứng minh null không được cache
        verify(userRepository, times(2)).findById(userId);
    }

    // =========================================================================
    // TEST 5: Các userId khác nhau → cache riêng biệt
    // =========================================================================

    @Test
    @DisplayName("Các userId khác nhau được cache riêng biệt")
    void testCacheIsolationBetweenDifferentUserIds() {
        // Arrange
        User user1 = new User("U001", "Nguyễn Văn An", "an@bank.vn", "0901234567");
        User user2 = new User("U002", "Trần Thị Bình", "binh@bank.vn", "0912345678");
        when(userRepository.findById("U001")).thenReturn(Optional.of(user1));
        when(userRepository.findById("U002")).thenReturn(Optional.of(user2));

        // Gọi user1 lần 1 → cache miss
        User result1 = userService.getUserById("U001");
        assertEquals("Nguyễn Văn An", result1.getFullName());

        // Gọi user2 lần 1 → cache miss (cache key khác)
        User result2 = userService.getUserById("U002");
        assertEquals("Trần Thị Bình", result2.getFullName());

        // Gọi lại user1 → cache hit
        userService.getUserById("U001");
        // Gọi lại user2 → cache hit
        userService.getUserById("U002");

        // Mỗi userId chỉ query DB đúng 1 lần
        verify(userRepository, times(1)).findById("U001");
        verify(userRepository, times(1)).findById("U002");
    }
}
