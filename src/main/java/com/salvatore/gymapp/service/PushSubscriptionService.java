package com.salvatore.gymapp.service;

import com.salvatore.gymapp.dto.notification.PushSubscriptionRequest;
import com.salvatore.gymapp.entity.gym.User;
import com.salvatore.gymapp.entity.notification.PushSubscription;
import com.salvatore.gymapp.exception.NotFoundException;
import com.salvatore.gymapp.repository.PushSubscriptionRepository;
import com.salvatore.gymapp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PushSubscriptionService {

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    @Transactional
    public void saveSubscription(Long userId, PushSubscriptionRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Utente non trovato"));

        PushSubscription subscription = pushSubscriptionRepository
                .findByEndpoint(request.getEndpoint())
                .orElseGet(PushSubscription::new);

        subscription.setEndpoint(request.getEndpoint());
        subscription.setP256dh(request.getP256dh());
        subscription.setAuth(request.getAuth());
        subscription.setUser(user);
        subscription.setActive(true);
        subscription.setPlatform(resolvePlatform(request.getEndpoint()));
        subscription.setLastSeenAt(LocalDateTime.now());

        pushSubscriptionRepository.save(subscription);
    }
    @Transactional(readOnly = true)
    public List<PushSubscription> findMyActiveSubscriptions(Long userId) {
        return pushSubscriptionRepository.findByUserIdAndActiveTrue(userId);
    }

    private String resolvePlatform(String endpoint) {
        if (endpoint == null) {
            return "UNKNOWN";
        }

        String lowerEndpoint = endpoint.toLowerCase();

        if (lowerEndpoint.contains("web.push.apple.com")) {
            return "IOS";
        }

        if (lowerEndpoint.contains("fcm.googleapis.com")) {
            return "ANDROID_CHROME";
        }

        if (lowerEndpoint.contains("mozilla.com")) {
            return "FIREFOX";
        }

        return "WEB";
    }
}