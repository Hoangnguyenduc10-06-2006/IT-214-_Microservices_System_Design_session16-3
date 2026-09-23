package com.example.productservice.service;

import com.example.productservice.entity.User;
import com.example.productservice.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    // Sử dụng Constructor Injection (best practice) thay vì @Autowired trên field
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Cacheable(
            value = "users",
            key = "#userId",
            condition = "#userId != null && !#userId.isBlank()",
            unless = "#result == null"
    )
    public User getUserById(String userId) {
        // Fail-fast: ném exception ngay nếu userId không hợp lệ
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "userId không được null hoặc rỗng. Giá trị nhận được: '" + userId + "'"
            );
        }

        logger.info("🔍 Cache MISS — Truy vấn Database cho userId: '{}'", userId);
        return userRepository.findById(userId).orElse(null);
    }
}
