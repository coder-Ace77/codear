package com.codear.user.repository;

import com.codear.user.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ChatRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdAndProblemIdOrderByTimestampAsc(Long userId, String problemId);
}