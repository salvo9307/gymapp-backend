package com.salvatore.gymapp.entity.notification;

import com.salvatore.gymapp.entity.gym.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "push_notification_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_push_notification_log",
                        columnNames = {"user_id", "subscription_id", "notification_type", "target_date"}
                )
        }
)
@Getter
@Setter
public class PushNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subscription_id", nullable = false)
    private Long subscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @PrePersist
    public void prePersist() {
        this.sentAt = LocalDateTime.now();
    }
}