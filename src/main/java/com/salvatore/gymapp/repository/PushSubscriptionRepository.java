package com.salvatore.gymapp.repository;

import com.salvatore.gymapp.entity.notification.PushSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushSubscriptionRepository extends JpaRepository<PushSubscription, Long> {

    Optional<PushSubscription> findByEndpoint(String endpoint);

    List<PushSubscription> findByUserIdAndActiveTrue(Long userId);

    List<PushSubscription> findByActiveTrue();
}