package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.notification.PushNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface PushNotificationLogRepository extends JpaRepository<PushNotificationLog, Long> {

    boolean existsByUser_IdAndSubscriptionIdAndNotificationTypeAndTargetDate(
            Long userId,
            Long subscriptionId,
            String notificationType,
            LocalDate targetDate
    );
}