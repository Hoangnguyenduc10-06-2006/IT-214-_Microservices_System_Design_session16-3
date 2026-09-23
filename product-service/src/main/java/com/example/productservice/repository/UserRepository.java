package com.example.productservice.repository;

import com.example.productservice.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


@Repository
public class UserRepository {

    private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

    // Giả lập database table "users"
    private final Map<String, User> database = new ConcurrentHashMap<>();

    @PostConstruct
    public void initData() {
        database.put("U001", new User("U001", "Nguyễn Văn An", "an.nguyen@bank.vn", "0901234567"));
        database.put("U002", new User("U002", "Trần Thị Bình", "binh.tran@bank.vn", "0912345678"));
        database.put("U003", new User("U003", "Lê Hoàng Cường", "cuong.le@bank.vn", "0923456789"));

        logger.info(" Đã khởi tạo {} người dùng trong database giả lập", database.size());
    }


    public Optional<User> findById(String userId) {
        logger.info("🗄️ [DB QUERY] Truy vấn thông tin userId '{}' từ Database", userId);

        // Giả lập độ trễ Database (200ms) — đây là nguyên nhân cần cache
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        User user = database.get(userId);
        return Optional.ofNullable(user);
    }
}
