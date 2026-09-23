package com.smeservicemanager.notification;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @EntityGraph(attributePaths = "user")
    List<Notification> findTop8ByUserUsernameOrUserIsNullOrderByCreateDateDesc(String username);
    long countByUserUsernameAndReadFalseAndStatus(String username, String status);
}
