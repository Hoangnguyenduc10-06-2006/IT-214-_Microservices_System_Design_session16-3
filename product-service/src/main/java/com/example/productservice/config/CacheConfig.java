package com.example.productservice.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;


@Configuration
@EnableCaching
@Profile("local")
public class CacheConfig {


    @Bean
    public CacheManager concurrentMapCacheManager() {
        // Chỉ tạo các cache region được khai báo — kiểm soát tốt hơn
        return new ConcurrentMapCacheManager("users", "productPrices", "inventory");
    }
}
